package org.stapledon.engine.source;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.StartSource;
import org.stapledon.common.util.LogContext;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.engine.source.ComicValidator.Problem;
import org.stapledon.engine.source.SourceCatalogRepository.MergeResult;
import org.stapledon.engine.source.SourceCatalogState.Entry;
import org.stapledon.engine.source.SourceCatalogState.SourceEntries;

/**
 * Runs the Sources page: refreshes each source's catalog, matches catalog entries to configured comics, adds comics from a catalog, and reads where
 * comics start. Work that calls a source on someone's behalf (avatars, start detection) is queued on the {@code catalogTaskExecutor}, so a request
 * never waits on a source's throttle.
 */
@Slf4j
@Service
public class SourceCatalogService {

    private final SourceRegistry sources;
    private final SourceCatalogRepository repository;
    private final ManagementFacade comics;
    private final ComicValidator validator;
    private final CatalogThumbnailService thumbnails;
    private final Executor executor;
    private final Clock clock;

    private final Map<String, ReentrantLock> refreshLocks = new ConcurrentHashMap<>();
    private final Set<Integer> pendingAvatars = ConcurrentHashMap.newKeySet();
    private final Set<Integer> pendingStarts = ConcurrentHashMap.newKeySet();

    @Autowired
    public SourceCatalogService(SourceRegistry sources, SourceCatalogRepository repository, ManagementFacade comics, ComicValidator validator,
            CatalogThumbnailService thumbnails, @Qualifier("catalogTaskExecutor") Executor executor) {
        this(sources, repository, comics, validator, thumbnails, executor, Clock.systemUTC());
    }

    SourceCatalogService(SourceRegistry sources, SourceCatalogRepository repository, ManagementFacade comics, ComicValidator validator,
            CatalogThumbnailService thumbnails, Executor executor, Clock clock) {
        this.sources = sources;
        this.repository = repository;
        this.comics = comics;
        this.validator = validator;
        this.thumbnails = thumbnails;
        this.executor = executor;
        this.clock = clock;
    }

    // =========================================================================
    // Refresh
    // =========================================================================

    /**
     * How a catalog refresh went.
     */
    public enum RefreshStatus {
        ALREADY_RUNNING,
        FAILED,
        NO_CATALOG,
        REFRESHED,
        UNKNOWN_SOURCE
    }

    /**
     * A catalog refresh's outcome, with what changed when it worked and why not when it didn't.
     */
    public record RefreshResult(RefreshStatus status, MergeResult changes, String error) {
    }

    /**
     * Reads a source's catalog and merges it into {@code source-catalog.json}. One refresh per source at a time; a second caller gets
     * {@link RefreshStatus#ALREADY_RUNNING} at once. A failure keeps the stored catalog and records the error.
     */
    public RefreshResult refresh(String sourceId) {
        Optional<ComicSource> found = sources.find(sourceId);
        if (found.isEmpty()) {
            return new RefreshResult(RefreshStatus.UNKNOWN_SOURCE, null, "Unknown source " + sourceId);
        }
        ComicSource source = found.get();
        Optional<SourceCatalog> catalog = source.catalog();
        if (catalog.isEmpty()) {
            return new RefreshResult(RefreshStatus.NO_CATALOG, null, source.displayName() + " has no catalog");
        }
        ReentrantLock lock = refreshLocks.computeIfAbsent(sourceId, _ -> new ReentrantLock());
        if (!lock.tryLock()) {
            return new RefreshResult(RefreshStatus.ALREADY_RUNNING, null, null);
        }
        long start = System.nanoTime();
        try {
            List<SourceCatalogEntry> entries = catalog.get().fetch();
            MergeResult changes = repository.merge(sourceId, entries);
            log.info("Catalog refresh {}: {} entries (+{} new, {} removed, {} back) in {}s", sourceId, changes.total(), changes.added(), changes.removed(),
                    changes.returned(), Duration.ofNanos(System.nanoTime() - start).toSeconds());
            applyCatalogStarts(source);
            return new RefreshResult(RefreshStatus.REFRESHED, changes, null);
        } catch (IOException | RuntimeException e) {
            // Expected when the source is down, rate limits us or changes its layout: one line, no stack trace
            log.warn("Catalog refresh {} failed after {}s: {}", sourceId, Duration.ofNanos(System.nanoTime() - start).toSeconds(), e.toString());
            repository.recordFailure(sourceId, e.getMessage() != null ? e.getMessage() : e.toString());
            return new RefreshResult(RefreshStatus.FAILED, null, e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * True while a catalog refresh for the source is running.
     */
    public boolean isRefreshing(String sourceId) {
        ReentrantLock lock = refreshLocks.get(sourceId);
        return lock != null && lock.isLocked();
    }

    /**
     * True when the source has a catalog that was never read, or was last read longer ago than {@code maxAge}.
     */
    public boolean isStale(String sourceId, Duration maxAge) {
        if (sources.find(sourceId).flatMap(ComicSource::catalog).isEmpty()) {
            return false;
        }
        return repository.find(sourceId)
                .map(SourceEntries::getLastRefreshed)
                .map(refreshed -> refreshed.plus(maxAge).isBefore(OffsetDateTime.now(clock)))
                .orElse(true);
    }

    /**
     * Gives configured comics with no start value the start their catalog entry reports (Comics Kingdom lists every comic's oldest strip).
     */
    private void applyCatalogStarts(ComicSource source) {
        Map<String, Entry> entries = repository.find(source.id()).map(SourceEntries::getEntries).orElse(Map.of());
        for (ComicItem comic : comicsOf(source)) {
            Entry entry = entries.get(source.identifierFor(comic));
            if (entry != null && entry.getStartDate() != null && comic.getStartSource() == null && comic.getSourceStartDate() == null && !source.indexed()) {
                applyStart(comic, StartInfo.ofDate(entry.getStartDate()));
            }
        }
    }

    // =========================================================================
    // Reading
    // =========================================================================

    /**
     * A source and how its catalog and comics stand.
     */
    public record SourceSummary(ComicSource source, int configuredCount, int activeCount, int catalogCount, OffsetDateTime lastRefreshed,
            OffsetDateTime lastAttempt, String lastError) {
    }

    /**
     * Every source's summary, by display name.
     */
    public List<SourceSummary> summaries() {
        return sources.all().stream().map(this::summary).toList();
    }

    /**
     * One source's summary.
     */
    public Optional<SourceSummary> summary(String sourceId) {
        return sources.find(sourceId).map(this::summary);
    }

    private SourceSummary summary(ComicSource source) {
        List<ComicItem> configured = comicsOf(source);
        Optional<SourceEntries> catalog = repository.find(source.id());
        int listed = catalog.map(c -> (int) c.getEntries().values().stream().filter(e -> e.getRemovedAt() == null).count()).orElse(0);
        return new SourceSummary(source, configured.size(), (int) configured.stream().filter(ComicItem::isActive).count(), listed,
                catalog.map(SourceEntries::getLastRefreshed).orElse(null),
                catalog.map(SourceEntries::getLastAttempt).orElse(null),
                catalog.map(SourceEntries::getLastError).orElse(null));
    }

    /**
     * One catalog entry, with the configured comic it matches (or null).
     */
    public record CatalogRow(String identifier, Entry entry, ComicItem comic) {
    }

    /**
     * A source's catalog by name, each entry joined to the configured comic with the same identifier.
     */
    public List<CatalogRow> catalog(String sourceId) {
        Optional<ComicSource> source = sources.find(sourceId);
        if (source.isEmpty()) {
            return List.of();
        }
        Map<String, ComicItem> byIdentifier = configuredByIdentifier(source.get());
        return repository.find(sourceId).map(SourceEntries::getEntries).orElse(Map.of()).entrySet().stream()
                .map(e -> new CatalogRow(e.getKey(), e.getValue(), byIdentifier.get(e.getKey())))
                .sorted(Comparator.comparing((CatalogRow row) -> Objects.requireNonNullElse(row.entry().getName(), row.identifier()), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Configured comics of a source whose identifier its catalog doesn't list. Empty when the catalog was never read.
     */
    public List<ComicItem> orphans(String sourceId) {
        Optional<ComicSource> source = sources.find(sourceId);
        Optional<SourceEntries> catalog = repository.find(sourceId);
        if (source.isEmpty() || catalog.isEmpty() || catalog.get().getLastRefreshed() == null) {
            return List.of();
        }
        Set<String> listed = catalog.get().getEntries().keySet();
        return comicsOf(source.get()).stream()
                .filter(comic -> !listed.contains(source.get().identifierFor(comic)))
                .toList();
    }

    /**
     * Where the source says a configured comic starts, when known: from its catalog entry or a past detection.
     */
    public Optional<StartInfo> reportedStart(ComicItem comic) {
        return sources.find(comic.getSource())
                .flatMap(source -> repository.find(source.id()).map(c -> c.getEntries().get(source.identifierFor(comic))))
                .filter(entry -> entry.getStartDate() != null || entry.getStartStripNumber() != null)
                .map(entry -> new StartInfo(entry.getStartDate(), entry.getStartStripNumber()));
    }

    /**
     * True while an avatar download for this comic is queued or running.
     */
    public boolean isAvatarPending(int comicId) {
        return pendingAvatars.contains(comicId);
    }

    /**
     * True while start detection for this comic is queued or running.
     */
    public boolean isStartPending(int comicId) {
        return pendingStarts.contains(comicId);
    }

    // =========================================================================
    // Changes
    // =========================================================================

    /**
     * The comic added from a catalog, or the problems that stopped it.
     */
    public record AddResult(ComicItem comic, List<Problem> problems) {
    }

    /**
     * Configures a catalog comic with defaults: its catalog name and author, the source and identifier, daily publication, and the start its catalog
     * entry reports. Then queues its avatar (copied from the catalog thumbnail when that is cached) and, when no start is known, start detection.
     */
    public AddResult addFromCatalog(String sourceId, String identifier, boolean active, boolean enabled) {
        Optional<ComicSource> found = sources.find(sourceId);
        if (found.isEmpty()) {
            return new AddResult(null, List.of(new Problem("source", "Unknown source \"" + sourceId + "\"")));
        }
        ComicSource source = found.get();
        Optional<Entry> entry = repository.find(sourceId).map(c -> c.getEntries().get(identifier));
        if (entry.isEmpty()) {
            return new AddResult(null, List.of(new Problem("identifier", "\"" + identifier + "\" is not in the " + source.displayName() + " catalog")));
        }

        ComicItem.ComicItemBuilder builder = ComicItem.builder()
                .name(entry.get().getName())
                .author(entry.get().getAuthor())
                .source(sourceId)
                .sourceIdentifier(identifier)
                .active(active)
                .enabled(enabled);
        if (source.indexed()) {
            builder.firstStripNumber(Objects.requireNonNullElse(entry.get().getStartStripNumber(), 1)).startSource(StartSource.DETECTED);
        } else if (entry.get().getStartDate() != null) {
            builder.sourceStartDate(entry.get().getStartDate()).startSource(StartSource.DETECTED);
        }
        ComicItem comic = builder.build();

        List<Problem> problems = validator.validateNew(comic);
        if (!problems.isEmpty()) {
            return new AddResult(null, problems);
        }
        Optional<ComicItem> created = comics.createComic(comic);
        if (created.isEmpty()) {
            return new AddResult(null, List.of(new Problem("identifier", "The comic could not be saved")));
        }
        log.info("AUDIT comic added from catalog: id={}, name={}, source={}/{}, active={}, enabled={}", created.get().getId(), created.get().getName(), sourceId,
                identifier, active, enabled);
        requestAvatar(created.get().getId());
        if (created.get().getStartSource() == null) {
            requestStartDetection(created.get().getId());
        }
        return new AddResult(created.get(), List.of());
    }

    /**
     * Queues an avatar download for a configured comic. Uses the cached catalog thumbnail when there is one, so no request is made. Returns false
     * when the comic doesn't exist or the queue is full.
     */
    public boolean requestAvatar(int comicId) {
        if (comics.getComic(comicId).isEmpty()) {
            return false;
        }
        if (!pendingAvatars.add(comicId)) {
            return true;
        }
        return submit(pendingAvatars, comicId, () -> fetchAvatar(comicId));
    }

    /**
     * Queues start detection for a configured comic. Returns false when the comic doesn't exist, its source can't detect starts, or the queue is full.
     */
    public boolean requestStartDetection(int comicId) {
        Optional<ComicItem> comic = comics.getComic(comicId);
        if (comic.isEmpty() || sources.find(comic.get().getSource()).flatMap(ComicSource::startDetector).isEmpty()) {
            return false;
        }
        if (!pendingStarts.add(comicId)) {
            return true;
        }
        return submit(pendingStarts, comicId, () -> comics.getComic(comicId).ifPresent(this::detectStart));
    }

    private boolean submit(Set<Integer> pending, int comicId, Runnable work) {
        try {
            executor.execute(() -> {
                try (var _ = MDC.putCloseable(LogContext.COMIC, comics.getComic(comicId).map(ComicItem::getName).orElse("#" + comicId))) {
                    work.run();
                } catch (RuntimeException e) {
                    log.error("Background source work for comic {} failed", comicId, e);
                } finally {
                    pending.remove(comicId);
                }
            });
            return true;
        } catch (TaskRejectedException e) {
            pending.remove(comicId);
            log.warn("Catalog work queue is full; dropped background work for comic {}", comicId);
            return false;
        }
    }

    private void fetchAvatar(int comicId) {
        Optional<ComicItem> comic = comics.getComic(comicId);
        if (comic.isEmpty()) {
            return;
        }
        Optional<ComicSource> source = sources.find(comic.get().getSource());
        Optional<CatalogThumbnailService.Thumbnail> thumbnail = source.flatMap(s -> thumbnails.load(s.id(), s.identifierFor(comic.get())));
        if (thumbnail.isPresent() && comics.saveAvatar(comicId, thumbnail.get().data())) {
            log.info("Avatar for {} copied from its catalog thumbnail", comic.get().getName());
            return;
        }
        comics.fetchAvatar(comicId);
    }

    /**
     * What start detection found.
     */
    public enum DetectionOutcome {
        /** Saved on the comic. */
        APPLIED,
        /** The source can't detect starts, or the request failed. */
        FAILED,
        /** The source didn't say. */
        NOT_FOUND,
        /** Recorded only: the comic has a value an admin set. */
        RECORDED
    }

    /**
     * Asks the comic's source where it starts, records the answer on its catalog entry, and saves it on the comic unless an admin set the start.
     */
    public DetectionOutcome detectStart(ComicItem comic) {
        Optional<ComicSource> source = sources.find(comic.getSource());
        Optional<StartDetector> detector = source.flatMap(ComicSource::startDetector);
        if (detector.isEmpty()) {
            return DetectionOutcome.FAILED;
        }
        try {
            Optional<StartInfo> start = detector.get().detect(comic);
            if (start.isEmpty()) {
                log.info("Start of {} not found at {}", comic.getName(), source.get().displayName());
                return DetectionOutcome.NOT_FOUND;
            }
            repository.recordStart(source.get().id(), source.get().identifierFor(comic), start.get());
            ComicItem current = comics.getComic(comic.getId()).orElse(comic);
            if (current.getStartSource() == StartSource.MANUAL) {
                log.info("Start of {} at {}: {} (kept the admin's value)", comic.getName(), source.get().displayName(), describe(start.get()));
                return DetectionOutcome.RECORDED;
            }
            applyStart(current, start.get());
            log.info("Start of {} detected at {}: {}", comic.getName(), source.get().displayName(), describe(start.get()));
            return DetectionOutcome.APPLIED;
        } catch (IOException | RuntimeException e) {
            log.warn("Start detection for {} failed: {}", comic.getName(), e.toString());
            return DetectionOutcome.FAILED;
        }
    }

    /**
     * Detects starts for up to {@code limit} configured comics of each source that have none yet, on the calling thread. Returns how many were saved.
     */
    public int detectMissingStarts(int limit) {
        int applied = 0;
        for (ComicSource source : sources.all()) {
            if (source.startDetector().isEmpty()) {
                continue;
            }
            List<ComicItem> missing = comicsOf(source).stream()
                    .filter(comic -> comic.getStartSource() == null && (source.indexed() ? comic.getFirstStripNumber() == null : comic.getSourceStartDate() == null))
                    .limit(limit)
                    .toList();
            for (ComicItem comic : missing) {
                if (detectStart(comic) == DetectionOutcome.APPLIED) {
                    applied++;
                }
            }
        }
        return applied;
    }

    private void applyStart(ComicItem comic, StartInfo start) {
        ComicItem.ComicItemBuilder builder = comic.toBuilder().startSource(StartSource.DETECTED);
        boolean changed = false;
        if (start.date() != null && !start.date().equals(comic.getSourceStartDate())) {
            builder.sourceStartDate(start.date());
            changed = true;
        }
        if (start.stripNumber() != null && !start.stripNumber().equals(comic.getFirstStripNumber())) {
            builder.firstStripNumber(start.stripNumber());
            changed = true;
        }
        if (changed || comic.getStartSource() == null) {
            comics.updateComic(comic.getId(), builder.build());
        }
    }

    private static String describe(StartInfo start) {
        return start.date() != null ? start.date().toString() : "#" + start.stripNumber();
    }

    private List<ComicItem> comicsOf(ComicSource source) {
        return comics.getAllComics().stream().filter(comic -> source.id().equals(comic.getSource())).toList();
    }

    private Map<String, ComicItem> configuredByIdentifier(ComicSource source) {
        Map<String, ComicItem> byIdentifier = new HashMap<>();
        for (ComicItem comic : comicsOf(source)) {
            byIdentifier.putIfAbsent(source.identifierFor(comic), comic);
        }
        return byIdentifier;
    }
}
