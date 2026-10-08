package org.stapledon.engine.management;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicSaveData;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.dto.SaveResult;
import org.stapledon.common.dto.StartSource;
import org.stapledon.common.dto.StripLoaderKey;
import org.stapledon.common.dto.StripLoaderKey.DateStripKey;
import org.stapledon.common.dto.StripLoaderKey.BoundaryStripKey;
import org.stapledon.common.service.ComicConfigurationService;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.common.service.RetrievalStatusService;
import org.stapledon.common.util.Direction;
import org.stapledon.common.util.LogContext;
import org.stapledon.engine.downloader.DownloaderConstants;
import org.stapledon.engine.downloader.DownloaderFacade;

/**
 * Implementation of the ManagementFacade interface. Acts as the central
 * coordinator between all other facades in the application.
 */
@Slf4j
@ToString
@Component
public class ComicManagementFacade implements ManagementFacade {

    private final ComicStorageFacade storageFacade;
    private final ComicConfigurationService configFacade;
    private final DownloaderFacade downloaderFacade;
    private final RetrievalStatusService retrievalStatusService;
    private final Executor sourceDownloadExecutor;
    private final Clock clock;
    private final ObjectProvider<CacheManager> cacheManager;

    /** Serialises every write to comics.json (load, change, save), and id assignment. */
    private final Object configLock = new Object();

    /**
     * In-memory cache of comics for O(1) lookups.
     * <p>
     * This is the PRIMARY SOURCE during runtime - the config file serves as the
     * persistence layer. Changes are written through to config but reads are
     * always served from this cache.
     * </p>
     * <p>
     * IMPORTANT:
     * <ul>
     *   <li>If config file is modified externally, call {@link #refreshComicList()}</li>
     *   <li>Multi-instance deployments should use distributed cache instead (Redis, Hazelcast, etc.)</li>
     *   <li>All mutation methods automatically update both cache and config file</li>
     * </ul>
     * </p>
     * <p>
     * Thread safety: ConcurrentHashMap provides thread-safe read/write operations.
     * Every write-through to the config file goes through {@link #persist(ComicItem)}, which holds {@link #configLock}.
     * </p>
     */
    private final Map<Integer, ComicItem> comics = new ConcurrentHashMap<>();

    /** The Caffeine cache holding {@link #getAllComics()}; comic-api's CaffeineCacheConfiguration creates it. */
    static final String COMIC_METADATA_CACHE = "comicMetadata";

    public ComicManagementFacade(ComicStorageFacade storageFacade, ComicConfigurationService configFacade,
            DownloaderFacade downloaderFacade, RetrievalStatusService retrievalStatusService,
            @Qualifier("sourceDownloadExecutor") Executor sourceDownloadExecutor, Clock clock,
            ObjectProvider<CacheManager> cacheManager) {
        this.storageFacade = storageFacade;
        this.configFacade = configFacade;
        this.downloaderFacade = downloaderFacade;
        this.retrievalStatusService = retrievalStatusService;
        this.sourceDownloadExecutor = sourceDownloadExecutor;
        this.clock = clock;
        this.cacheManager = cacheManager;

        // Load comics from configuration
        refreshComicList();

        log.info("Comic configuration loaded.");
    }

    @Override
    @Cacheable(value = COMIC_METADATA_CACHE, key = "'allComics'")
    public List<ComicItem> getAllComics() {
        List<ComicItem> result = new ArrayList<>(comics.values());
        Collections.sort(result);
        return result;
    }

    @Override
    public Optional<ComicItem> getComic(int comicId) {
        return Optional.ofNullable(comics.get(comicId));
    }

    @Override
    public Optional<ComicItem> getComicByName(String comicName) {
        if (comicName == null) {
            return Optional.empty();
        }

        return comics.values().stream()
                .filter(comic -> Optional.ofNullable(comic.getName())
                                        .map(name -> name.equalsIgnoreCase(comicName))
                                        .orElse(false))
                .findFirst();
    }

    /**
     * {@inheritDoc}
     * <p>
     * An id of 0 means "assign one": the new comic gets the highest existing id plus one. An explicit id that is already taken fails.
     */
    @Override
    public Optional<ComicItem> createComic(ComicItem comicItem) {
        synchronized (configLock) {
            ComicItem toCreate = comicItem;
            if (toCreate.getId() == 0) {
                int nextId = comics.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
                toCreate = toCreate.toBuilder().id(nextId).build();
            }
            if (comics.containsKey(toCreate.getId())) {
                return Optional.empty();
            }
            return Optional.of(persist(toCreate));
        }
    }

    @Override
    public Optional<ComicItem> updateComic(int comicId, ComicItem comicItem) {
        ComicItem toSave = comicItem.getId() == comicId ? comicItem : comicItem.toBuilder().id(comicId).build();
        return Optional.of(persist(toSave));
    }

    @Override
    public boolean updateComic(int comicId) {
        // Check if comic exists, return false if not
        Optional<ComicItem> comicOpt = getComic(comicId);
        if (comicOpt.isEmpty()) {
            log.warn("Comic with ID {} not found, cannot update", comicId);
            return false;
        }

        try {
            ComicItem comic = comicOpt.get();
            // Create download request
            ComicDownloadRequest request = ComicDownloadRequest.builder().comicId(comic.getId())
                    .comicName(comic.getName()).source(comic.getSource())
                    .sourceIdentifier(comic.getSourceIdentifier()).date(LocalDate.now(clock)).build();

            // Download the comic
            ComicDownloadResult result = downloaderFacade.downloadComic(request);

            if (!result.isSuccessful()) {
                // The strategy already logged the cause at the right level
                log.debug("Failed to download comic {}: {}", comic.getName(), result.getErrorMessage());
                return false;
            }
            if (!saveDownloadResult(comic, request.getDate(), result)) {
                return false;
            }
            ComicItem updated = comic.toBuilder().newest(request.getDate()).build();
            updateComic(comic.getId(), updated);
            return true;
        } catch (Exception e) {
            log.error("Error occurred while updating comic with ID {}", comicId, e);
            return false;
        }
    }

    @Override
    public boolean updateComic(String comicName) {
        return getComicByName(comicName).map(comic -> updateComic(comic.getId())).orElse(false);
    }

    /**
     * Saves a comic to memory and to comics.json under {@link #configLock}, then drops the cached comic list. Every comic write goes through here, so
     * the list never goes stale (the {@code @CacheEvict} annotations this replaces never fired for calls from inside this class). A start date that a
     * stored strip proves wrong is corrected on the way (see {@link #correctStart(ComicItem)}).
     */
    private ComicItem persist(ComicItem comic) {
        ComicItem toSave = correctStart(comic);
        synchronized (configLock) {
            comics.put(toSave.getId(), toSave);
            ComicConfig config = configFacade.loadComicConfig();
            config.getItems().put(toSave.getId(), toSave);
            configFacade.saveComicConfig(config);
        }
        evictComicList();
        return toSave;
    }

    /**
     * A stored strip older than the comic's start date proves the start date wrong, even one an admin set: moves {@code sourceStartDate} back to the
     * oldest stored strip. Keeps a manual origin, since the admin's value was only off by what is now on disk.
     */
    ComicItem correctStart(ComicItem comic) {
        LocalDate start = comic.getSourceStartDate();
        LocalDate oldest = comic.getOldest();
        if (start == null || oldest == null || !oldest.isBefore(start)) {
            return comic;
        }
        log.warn("Start date for {} corrected from {} to {} (strip on disk)", comic.getName(), start, oldest);
        log.info("AUDIT comic start date corrected: id={}, name={}, from={}, to={}, reason=strip on disk", comic.getId(), comic.getName(), start, oldest);
        return comic.toBuilder()
                .sourceStartDate(oldest)
                .startSource(comic.getStartSource() != null ? comic.getStartSource() : StartSource.DETECTED)
                .build();
    }

    /**
     * Lowers {@code firstStripNumber} when a strip with a lower number has been stored, which proves the recorded first strip wrong.
     */
    ComicItem correctFirstStripNumber(ComicItem comic, Integer storedStripNumber) {
        Integer first = comic.getFirstStripNumber();
        if (storedStripNumber == null || first == null || storedStripNumber >= first) {
            return comic;
        }
        log.warn("First strip number for {} corrected from #{} to #{} (strip on disk)", comic.getName(), first, storedStripNumber);
        log.info("AUDIT comic first strip corrected: id={}, name={}, from={}, to={}, reason=strip on disk", comic.getId(), comic.getName(), first, storedStripNumber);
        return comic.toBuilder()
                .firstStripNumber(storedStripNumber)
                .startSource(comic.getStartSource() != null ? comic.getStartSource() : StartSource.DETECTED)
                .build();
    }

    private void evictComicList() {
        CacheManager manager = cacheManager.getIfAvailable();
        if (manager == null) {
            return;
        }
        Cache cache = manager.getCache(COMIC_METADATA_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Override
    public boolean deleteComic(int comicId) {
        ComicItem removed;
        synchronized (configLock) {
            removed = comics.remove(comicId);
            if (removed == null) {
                return false;
            }
            ComicConfig config = configFacade.loadComicConfig();
            config.getItems().remove(comicId);
            configFacade.saveComicConfig(config);
        }
        evictComicList();

        // Also remove from storage
        storageFacade.deleteComic(ComicIdentifier.from(removed));
        return true;
    }

    @Override
    public ComicNavigationResult getComicStrip(int comicId, Direction direction) {
        return getComic(comicId)
                .map(comic -> getComicStripForDirection(comic, direction))
                .orElseGet(() -> ComicNavigationResult.notFound("NO_COMICS_AVAILABLE", null, null, null));
    }

    @Override
    public ComicNavigationResult getComicStrip(int comicId, Direction direction, LocalDate from) {
        return getComic(comicId)
                .map(comic -> getComicStripInternal(comicId, comic.getName(), direction, from))
                .orElseGet(() -> ComicNavigationResult.notFound("NO_COMICS_AVAILABLE", from, null, null));
    }

    private ComicNavigationResult getComicStripForDirection(ComicItem comic, Direction direction) {
        try {
            ComicIdentifier identifier = ComicIdentifier.from(comic);

            // Get the date based on direction
            Optional<LocalDate> dateOpt = direction == Direction.FORWARD
                    ? storageFacade.getOldestDateWithComic(identifier)
                    : storageFacade.getNewestDateWithComic(identifier);

            return dateOpt
                    .flatMap(date -> storageFacade.getComicStripInfo(identifier, date)
                            .map(image -> {
                                LocalDate nearestPrev = storageFacade.getPreviousDateWithComic(identifier, date)
                                        .orElse(null);
                                LocalDate nearestNext = storageFacade.getNextDateWithComic(identifier, date)
                                        .orElse(null);
                                return ComicNavigationResult.found(image, nearestPrev, nearestNext);
                            }))
                    .orElseGet(() -> ComicNavigationResult.notFound("NO_COMICS_AVAILABLE", null, null, null));
        } catch (Exception e) {
            log.error("Error retrieving comic strip for {}", comic.getName(), e);
            return ComicNavigationResult.notFound("ERROR", null, null, null);
        }
    }

    /**
     * Internal implementation of getComicStrip.
     */
    private ComicNavigationResult getComicStripInternal(int comicId, String comicName, Direction direction,
            LocalDate from) {
        log.debug("getComicStrip: comicId={}, comicName={}, direction={}, from={}", comicId, comicName, direction, from);

        ComicIdentifier identifier = new ComicIdentifier(comicId, comicName);

        try {
            // Get the next/previous date
            Optional<LocalDate> dateOpt = direction == Direction.FORWARD
                    ? storageFacade.getNextDateWithComic(identifier, from)
                    : storageFacade.getPreviousDateWithComic(identifier, from);

            log.debug("get{}DateWithComic returned: {} (from: {})",
                    direction == Direction.FORWARD ? "Next" : "Previous",
                    dateOpt.orElse(null), from);

            if (dateOpt.isEmpty()) {
                String reason = direction == Direction.FORWARD ? "AT_END" : "AT_BEGINNING";
                log.debug("No comic found going {} from {}, reason={}", direction, from, reason);

                // Get the nearest dates for navigation hints
                LocalDate nearestPrev = storageFacade.getPreviousDateWithComic(identifier, from).orElse(null);
                LocalDate nearestNext = storageFacade.getNextDateWithComic(identifier, from).orElse(null);

                log.debug("Returning navigation result: found=false, currentDate=null, nearestPrev={}, nearestNext={}",
                        nearestPrev, nearestNext);
                return ComicNavigationResult.notFound(reason, from, nearestPrev, nearestNext);
            }

            // Get the image for the found date
            LocalDate targetDate = dateOpt.get();
            log.debug("Found comic at {}, loading image and calculating boundaries...", targetDate);

            return storageFacade.getComicStripInfo(identifier, targetDate)
                    .map(image -> {
                        // Get boundary dates for navigation hints
                        LocalDate nearestPrev = storageFacade.getPreviousDateWithComic(identifier, targetDate)
                                .orElse(null);
                        LocalDate nearestNext = storageFacade.getNextDateWithComic(identifier, targetDate)
                                .orElse(null);

                        log.debug("Returning navigation result: found=true, currentDate={}, nearestPrev={}, nearestNext={}",
                                targetDate, nearestPrev, nearestNext);
                        return ComicNavigationResult.found(image, nearestPrev, nearestNext);
                    })
                    .orElseGet(() -> {
                        log.warn("Image date exists but image couldn't be loaded for {} on {}", comicName, targetDate);
                        return ComicNavigationResult.notFound("ERROR", targetDate, null, null);
                    });
        } catch (Exception e) {
            log.error("Error retrieving comic strip for {}", comicName, e);
            return ComicNavigationResult.notFound("ERROR", from, null, null);
        }
    }

    @Override
    public Optional<ImageDto> getComicStripOnDate(int comicId, LocalDate date) {
        return getComic(comicId).flatMap(comic -> storageFacade.getComicStrip(ComicIdentifier.from(comic), date));
    }

    @Override
    public Optional<ImageDto> getComicStripOnDate(String comicName, LocalDate date) {
        return getComicByName(comicName)
                .flatMap(comic -> storageFacade.getComicStrip(ComicIdentifier.from(comic), date));
    }

    @Override
    public ComicNavigationResult getComicStripWithNavigation(int comicId, LocalDate date) {
        return getComic(comicId)
                .map(comic -> getComicStripWithNavigationInternal(comic, date))
                .orElseGet(() -> ComicNavigationResult.notFound("NO_COMICS_AVAILABLE", date, null, null));
    }

    private ComicNavigationResult getComicStripWithNavigationInternal(ComicItem comic, LocalDate date) {
        ComicIdentifier identifier = ComicIdentifier.from(comic);
        log.debug("getComicStripWithNavigation: comicId={}, comicName={}, date={}", comic.getId(), comic.getName(), date);

        try {
            // Calculate navigation boundaries
            LocalDate nearestPrev = storageFacade.getPreviousDateWithComic(identifier, date).orElse(null);
            LocalDate nearestNext = storageFacade.getNextDateWithComic(identifier, date).orElse(null);

            // Get the image for the exact date requested
            return storageFacade.getComicStripInfo(identifier, date)
                    .map(image -> {
                        log.debug("Found comic strip for {} on {}, prev={}, next={}",
                                comic.getName(), date, nearestPrev, nearestNext);
                        return ComicNavigationResult.found(image, nearestPrev, nearestNext);
                    })
                    .orElseGet(() -> {
                        log.debug("No comic strip found for {} on {}, returning navigation hints: prev={}, next={}",
                                comic.getName(), date, nearestPrev, nearestNext);
                        return ComicNavigationResult.notFound("NOT_AVAILABLE", date, nearestPrev, nearestNext);
                    });
        } catch (Exception e) {
            log.error("Error retrieving comic strip on exact date: {}", e.getMessage(), e);
            return ComicNavigationResult.notFound("ERROR", date, null, null);
        }
    }

    @Override
    public Map<StripLoaderKey, ComicNavigationResult> getComicStripsWithNavigation(Set<StripLoaderKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("Batch loading {} comic strips", keys.size());

        // Group by comicId for efficient processing
        Map<Integer, List<StripLoaderKey>> byComic = keys.stream()
                .collect(Collectors.groupingBy(StripLoaderKey::comicId));

        Map<StripLoaderKey, ComicNavigationResult> results = new HashMap<>();

        byComic.forEach((comicId, comicKeys) ->
            getComic(comicId).ifPresentOrElse(
                comic -> comicKeys.forEach(key -> {
                    ComicNavigationResult result = switch (key) {
                        case DateStripKey dateKey -> getComicStripWithNavigation(comicId, dateKey.date());
                        case BoundaryStripKey boundaryKey -> getComicStrip(comicId, boundaryKey.direction());
                    };
                    results.put(key, result);
                }),
                () -> comicKeys.forEach(key -> {
                    LocalDate dateForResult = switch (key) {
                        case DateStripKey dateKey -> dateKey.date();
                        case BoundaryStripKey boundaryKey -> null;
                    };
                    results.put(key, ComicNavigationResult.notFound(
                            "NO_COMICS_AVAILABLE", dateForResult, null, null));
                })
            )
        );

        return results;
    }

    @Override
    public Optional<ImageDto> getAvatar(int comicId) {
        return getComic(comicId).flatMap(comic -> storageFacade.getAvatar(ComicIdentifier.from(comic)));
    }

    @Override
    public Optional<ImageDto> getAvatar(String comicName) {
        return getComicByName(comicName).flatMap(comic -> storageFacade.getAvatar(ComicIdentifier.from(comic)));
    }

    @Override
    public boolean updateAllComics() {
        try {
            updateComicsForDate(LocalDate.now(clock));
            return true;
        } catch (Exception e) {
            log.error("Error occurred while updating all comics", e);
            return false;
        }
    }

    @Override
    public List<ComicDownloadResult> updateComicsForDate(LocalDate date) {
        return updateComicsForDate(date, null);
    }

    @Override
    public List<ComicDownloadResult> updateComicsForDate(LocalDate date, String sourceFilter) {
        boolean hasSourceFilter = sourceFilter != null && !"ALL".equalsIgnoreCase(sourceFilter);

        try {
            // Log if attempting to download future dates
            LocalDate today = LocalDate.now(clock);
            if (date.isAfter(today)) {
                log.warn("Future date: attempting to download comics for {} which is AFTER today ({})",
                        date, today);
            } else {
                log.info("Downloading comics for date: {} (today: {}, sourceFilter: {})", date, today, sourceFilter);
            }

            // Get the current comic configuration
            ComicConfig config = configFacade.loadComicConfig();

            if (config == null || config.getComics() == null) {
                log.warn("Comic configuration is null or empty");
                return new ArrayList<>();
            }

            DayOfWeek dayOfWeek = date.getDayOfWeek();

            // Apply all eligibility filters and group remaining comics by source. LinkedHashMap to keep grouping deterministic for tests.
            Map<String, List<ComicItem>> bySource = new LinkedHashMap<>();
            for (ComicItem comic : config.getComics()) {
                if (!isEligibleForDownload(comic, date, dayOfWeek, sourceFilter, hasSourceFilter)) {
                    continue;
                }
                bySource.computeIfAbsent(comic.getSource(), s -> new ArrayList<>()).add(comic);
            }

            if (bySource.isEmpty()) {
                log.info("No comics eligible for download on {} (sourceFilter: {})", date, sourceFilter);
                return new ArrayList<>();
            }

            // Run one task per source in parallel; each task processes its source serially, paced by SourceThrottleService inside the strategy.
            log.info("Dispatching {} source download tasks: {}", bySource.size(), bySource.keySet());
            List<CompletableFuture<List<ComicDownloadResult>>> futures = bySource.entrySet().stream()
                    .map(entry -> CompletableFuture.supplyAsync(() -> downloadAllForSource(entry.getKey(), entry.getValue(), date), sourceDownloadExecutor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

            return futures.stream()
                    .flatMap(f -> f.join().stream())
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (Exception e) {
            // Let the caller fail (the batch step, so the job ends FAILED instead of COMPLETED with nothing downloaded)
            throw new IllegalStateException("Comic download run for " + date + " failed: " + e.getMessage(), e);
        }
    }

    private boolean isEligibleForDownload(ComicItem comic, LocalDate date, DayOfWeek dayOfWeek, String sourceFilter, boolean hasSourceFilter) {
        if (comic.getSource() == null || comic.getSource().isEmpty()) {
            log.warn("Skipping comic '{}' - has null or empty source", comic.getName());
            return false;
        }
        if (hasSourceFilter && !sourceFilter.equalsIgnoreCase(comic.getSource())) {
            return false;
        }
        if (!comic.isActive()) {
            log.info("Skipping comic '{}' - comic is inactive/discontinued", comic.getName());
            return false;
        }
        if (comic.getPublicationDays() != null && !comic.getPublicationDays().isEmpty()
                && !comic.getPublicationDays().contains(dayOfWeek)) {
            log.info("Skipping comic '{}' - not published on {} (publishes: {})", comic.getName(), dayOfWeek, comic.getPublicationDays());
            return false;
        }
        if (storageFacade.comicStripExists(ComicIdentifier.from(comic), date)) {
            log.info("Skipping comic '{}' for {} - already cached", comic.getName(), date);
            return false;
        }
        return true;
    }

    private List<ComicDownloadResult> downloadAllForSource(String source, List<ComicItem> comics, LocalDate date) {
        List<ComicDownloadResult> sourceResults = new ArrayList<>(comics.size());
        log.info("Source thread starting: {} ({} comics for {})", source, comics.size(), date);
        long start = System.currentTimeMillis();
        int blockedInARow = 0;
        int skipped = 0;
        try {
            boolean indexed = downloaderFacade.isIndexedSource(source);
            for (ComicItem comic : comics) {
                if (blockedInARow >= DownloaderConstants.BLOCKED_IN_A_ROW_TO_STOP_SOURCE) {
                    if (skipped++ == 0) {
                        log.warn("Source {} refused {} downloads in a row (HTTP 403); skipping its remaining comics for {}", source, blockedInARow, date);
                    }
                    continue;
                }
                try (var _ = MDC.putCloseable(LogContext.COMIC, comic.getName());
                        var _ = MDC.putCloseable(LogContext.DATE, date.toString())) {
                    if (indexed) {
                        Optional<ComicDownloadResult> indexedResult = downloadLatestIndexedComic(comic);
                        indexedResult.ifPresent(sourceResults::add);
                        blockedInARow = indexedResult.filter(ComicDownloadResult::isBlocked).isPresent() ? blockedInARow + 1 : 0;
                        continue;
                    }

                    ComicDownloadRequest request = ComicDownloadRequest.builder().comicId(comic.getId())
                            .comicName(comic.getName()).source(comic.getSource())
                            .sourceIdentifier(comic.getSourceIdentifier()).date(date).build();

                    ComicDownloadResult result = downloaderFacade.downloadComic(request);
                    sourceResults.add(result);
                    blockedInARow = result.isBlocked() ? blockedInARow + 1 : 0;

                    if (result.isSuccessful()) {
                        if (!saveDownloadResult(comic, date, result)) {
                            continue;
                        }
                        ComicItem updated = comic.toBuilder().newest(date).build();
                        updateComic(comic.getId(), updated);
                    } else {
                        // The strategy already logged the cause at the right level
                        log.debug("Failed to download comic {}: {}", comic.getName(), result.getErrorMessage());
                    }
                } catch (Exception e) {
                    log.error("Error processing comic {} on {}", comic.getName(), date, e);
                }
            }
        } finally {
            log.info("Source thread finished: {} ({} results, {} skipped as blocked, in {}ms)", source, sourceResults.size(), skipped,
                    System.currentTimeMillis() - start);
        }
        return sourceResults;
    }

    @Override
    public Optional<ComicDownloadResult> downloadComicForDate(ComicItem comic, LocalDate date, boolean failFastOnRateLimit) {
        // Validate comic has a source
        if (comic.getSource() == null || comic.getSource().isEmpty()) {
            log.warn("Cannot download comic '{}' - has null or empty source", comic.getName());
            return Optional.empty();
        }

        // Check if comic already exists on disk
        if (storageFacade.comicStripExists(ComicIdentifier.from(comic), date)) {
            log.debug("Comic '{}' for {} already cached", comic.getName(), date);
            return Optional.empty();
        }

        // Create download request and download
        ComicDownloadRequest request = ComicDownloadRequest.builder().comicId(comic.getId()).comicName(comic.getName())
                .source(comic.getSource())
                .sourceIdentifier(comic.getSourceIdentifier()).date(date).failFastOnRateLimit(failFastOnRateLimit).build();

        ComicDownloadResult result = downloaderFacade.downloadComic(request);

        if (result.isSuccessful()) {
            LocalDate saveDate = result.getActualDate() != null ? result.getActualDate() : date;
            SaveResult.Outcome outcome = saveDownloadResultWithOutcome(comic, saveDate, result);

            if (outcome == null) {
                return Optional.empty();
            }
            result = result.toBuilder().saveOutcome(outcome).build();
            if (outcome == SaveResult.Outcome.DUPLICATE_SKIPPED) {
                return Optional.of(result);
            }

            // Update oldest date if this is earlier than known
            LocalDate currentOldest = comic.getOldest();
            if (currentOldest == null || saveDate.isBefore(currentOldest)) {
                ComicItem updated = comic.toBuilder().oldest(saveDate).build();
                updateComic(comic.getId(), updated);
            }

            // Update strip number tracking for indexed comics
            updateStripNumberTracking(comic, result);
        }

        return Optional.of(result);
    }

    @Override
    public Optional<ComicDownloadResult> downloadLatestIndexedComic(ComicItem comic) {
        if (comic.getSource() == null || comic.getSource().isEmpty()) {
            log.warn("Cannot download comic '{}' - has null or empty source", comic.getName());
            return Optional.empty();
        }

        ComicDownloadResult result = downloaderFacade.downloadLatestStrip(comic);

        if (result.isSuccessful()) {
            LocalDate saveDate = result.getActualDate() != null ? result.getActualDate() : LocalDate.now(clock);

            // Check if this date already exists
            if (storageFacade.comicStripExists(ComicIdentifier.from(comic), saveDate)) {
                log.debug("Comic '{}' for {} already cached", comic.getName(), saveDate);
                return Optional.empty();
            }

            boolean saved = saveDownloadResult(comic, saveDate, result);
            if (!saved) {
                return Optional.empty();
            }

            // Update newest date
            ComicItem.ComicItemBuilder builder = comic.toBuilder().newest(saveDate);
            updateStripNumberOnBuilder(builder, result);
            updateComic(comic.getId(), correctFirstStripNumber(builder.build(), result.getStripNumber()));
        }

        return Optional.of(result);
    }

    @Override
    public Optional<ComicDownloadResult> downloadComicByStripNumber(ComicItem comic, int stripNumber) {
        if (comic.getSource() == null || comic.getSource().isEmpty()) {
            log.warn("Cannot download comic '{}' - has null or empty source", comic.getName());
            return Optional.empty();
        }

        ComicDownloadResult result = downloaderFacade.downloadStrip(comic, stripNumber);

        if (result.isSuccessful()) {
            LocalDate saveDate = result.getActualDate() != null ? result.getActualDate() : LocalDate.now(clock);

            // Check if this date already exists
            if (storageFacade.comicStripExists(ComicIdentifier.from(comic), saveDate)) {
                log.debug("Comic '{}' for {} already cached", comic.getName(), saveDate);
                return Optional.empty();
            }

            boolean saved = saveDownloadResult(comic, saveDate, result);
            if (!saved) {
                return Optional.empty();
            }

            // Update oldest date if this is earlier than known (backfill goes backwards), and the first strip number if this strip is lower
            LocalDate currentOldest = comic.getOldest();
            ComicItem updated = correctFirstStripNumber(comic, result.getStripNumber());
            if (currentOldest == null || saveDate.isBefore(currentOldest)) {
                updated = updated.toBuilder().oldest(saveDate).build();
            }
            if (updated != comic) {
                updateComic(comic.getId(), updated);
            }
        }

        return Optional.of(result);
    }

    /**
     * Saves a download result to storage via ComicSaveData DTO. The downloader has already
     * recorded the retrieval as SUCCESS, so a failed save replaces that record with STORAGE_ERROR.
     */
    private boolean saveDownloadResult(ComicItem comic, LocalDate date, ComicDownloadResult result) {
        return saveDownloadResultWithOutcome(comic, date, result) != null;
    }

    /**
     * Saves a download result like {@link #saveDownloadResult}, returning the save outcome (SAVED or DUPLICATE_SKIPPED), or null when the save failed.
     */
    private SaveResult.Outcome saveDownloadResultWithOutcome(ComicItem comic, LocalDate date, ComicDownloadResult result) {
        ComicSaveData saveData = ComicSaveData.builder()
                .imageData(result.getImageData())
                .transcript(result.getTranscript())
                .stripNumber(result.getStripNumber())
                .build();

        SaveResult saveResult = storageFacade.saveComicStripWithResult(ComicIdentifier.from(comic), date, saveData);
        if (saveResult.isSuccess()) {
            return saveResult.getOutcome();
        }

        log.error("Failed to save comic {} for {} to storage: {}", comic.getName(), date, saveResult.getMessage());
        retrievalStatusService.recordRetrievalResult(ComicRetrievalRecord.failure(
                comic.getId(), comic.getName(), date, comic.getSource(), ComicRetrievalStatus.STORAGE_ERROR,
                "Save failed: " + saveResult.getMessage(), 0, null));
        return null;
    }

    /**
     * Updates strip number tracking on the comic if the result contains strip number info.
     */
    private void updateStripNumberTracking(ComicItem comic, ComicDownloadResult result) {
        if (result.getStripNumber() != null) {
            Integer currentLast = comic.getLastStripNumber();
            if (currentLast == null || result.getStripNumber() > currentLast) {
                updateComic(comic.getId(),
                        comic.toBuilder().lastStripNumber(result.getStripNumber()).build());
            }
        }
    }

    /**
     * Updates strip number fields on a builder if the result contains strip number info.
     */
    private void updateStripNumberOnBuilder(ComicItem.ComicItemBuilder builder, ComicDownloadResult result) {
        if (result.getStripNumber() != null) {
            builder.lastStripNumber(result.getStripNumber());
        }
    }

    @Override
    public void refreshComicList() {
        long startTime = System.currentTimeMillis();
        try {
            // Load comic configuration
            ComicConfig comicConfig = configFacade.loadComicConfig();

            // Clear and reload comics
            comics.clear();
            if (comicConfig.getItems() != null) {
                comics.putAll(comicConfig.getItems());
            }

            // Sync oldest/newest dates and avatarAvailable flag from the actual index. The dates are derived from the index and only
            // refreshed in memory; comics.json is rewritten only when a stored setting changes (the avatar flag or a corrected start)
            int synced = 0;
            boolean storedSettingChanged = false;
            for (Map.Entry<Integer, ComicItem> entry : comics.entrySet()) {
                ComicItem comic = entry.getValue();
                ComicIdentifier id = ComicIdentifier.from(comic);
                Optional<LocalDate> actualOldest = storageFacade.getOldestDateWithComic(id);
                Optional<LocalDate> actualNewest = storageFacade.getNewestDateWithComic(id);
                boolean avatarExists = storageFacade.avatarExists(id);

                boolean datesStale = actualOldest.isPresent() && !actualOldest.get().equals(comic.getOldest())
                        || actualNewest.isPresent() && !actualNewest.get().equals(comic.getNewest());
                boolean avatarStale = avatarExists != comic.isAvatarAvailable();

                if (datesStale || avatarStale) {
                    ComicItem updated = correctStart(comic.toBuilder()
                            .oldest(actualOldest.orElse(comic.getOldest()))
                            .newest(actualNewest.orElse(comic.getNewest()))
                            .avatarAvailable(avatarExists)
                            .build());
                    entry.setValue(updated);
                    comicConfig.getItems().put(updated.getId(), updated);
                    synced++;
                    if (avatarStale || !Objects.equals(updated.getSourceStartDate(), comic.getSourceStartDate())) {
                        storedSettingChanged = true;
                    }
                }
            }
            if (storedSettingChanged) {
                synchronized (configLock) {
                    configFacade.saveComicConfig(comicConfig);
                }
                log.info("Synced comic metadata from index for {} comics and saved the configuration", synced);
            } else if (synced > 0) {
                log.info("Synced comic dates from index for {} comics", synced);
            }
            evictComicList();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Refreshed comic list: loaded {} comics in {}ms", comics.size(), duration);
        } catch (Exception e) {
            log.error("Error refreshing comic list: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean purgeOldImages(int daysToKeep) {
        boolean allSucceeded = true;

        for (ComicItem comic : comics.values()) {
            boolean success = storageFacade.purgeOldImages(ComicIdentifier.from(comic), daysToKeep);
            if (!success) {
                log.error("Failed to purge old images for comic {}", comic.getName());
                allSucceeded = false;
            }
        }

        return allSucceeded;
    }

    @Override
    public List<ComicNavigationResult> getStripWindow(int comicId, LocalDate center, int before, int after) {
        return getComic(comicId)
                .map(comic -> {
                    ComicIdentifier identifier = ComicIdentifier.from(comic);
                    List<LocalDate> dates = new ArrayList<>();

                    // Walk backward from center to collect `before` dates
                    LocalDate cursor = center;
                    for (int i = 0; i < before; i++) {
                        Optional<LocalDate> prev = storageFacade.getPreviousDateWithComic(identifier, cursor);
                        if (prev.isEmpty()) {
                            break;
                        }
                        dates.addFirst(prev.get());
                        cursor = prev.get();
                    }

                    // Add center
                    dates.add(center);

                    // Walk forward from center to collect `after` dates
                    cursor = center;
                    for (int i = 0; i < after; i++) {
                        Optional<LocalDate> next = storageFacade.getNextDateWithComic(identifier, cursor);
                        if (next.isEmpty()) {
                            break;
                        }
                        dates.add(next.get());
                        cursor = next.get();
                    }

                    // Fetch navigation results for each date
                    return dates.stream()
                            .map(date -> getComicStripWithNavigation(comicId, date))
                            .toList();
                })
                .orElse(List.of());
    }

    @Override
    public Optional<LocalDate> getRandomDate(int comicId) {
        return getComic(comicId)
                .flatMap(comic -> {
                    List<LocalDate> dates = storageFacade.getAvailableDates(ComicIdentifier.from(comic));
                    if (dates.isEmpty()) {
                        return Optional.empty();
                    }
                    int index = java.util.concurrent.ThreadLocalRandom.current().nextInt(dates.size());
                    return Optional.of(dates.get(index));
                });
    }

    @Override
    public Optional<LocalDate> getNewestDateWithComic(int comicId) {
        return getComic(comicId).flatMap(comic -> storageFacade.getNewestDateWithComic(ComicIdentifier.from(comic)));
    }

    @Override
    public Optional<LocalDate> getOldestDateWithComic(int comicId) {
        return getComic(comicId).flatMap(comic -> storageFacade.getOldestDateWithComic(ComicIdentifier.from(comic)));
    }

    @Override
    public List<ComicRetrievalRecord> getRetrievalRecords(String comicName, int limit) {
        return retrievalStatusService.getRetrievalRecords(comicName, null, null, null, limit);
    }

    @Override
    public Map<String, Object> getRetrievalSummary(LocalDate fromDate, LocalDate toDate) {
        return retrievalStatusService.getRetrievalSummary(fromDate, toDate);
    }

    @Override
    public int purgeOldRetrievalRecords(int daysToKeep) {
        return retrievalStatusService.purgeOldRecords(daysToKeep);
    }

    @Override
    public int downloadMissingAvatars() {
        int downloaded = 0;
        int skipped = 0;
        int failed = 0;

        for (ComicItem comic : List.copyOf(comics.values())) {
            // Skip if avatar already exists on disk
            if (storageFacade.getAvatar(ComicIdentifier.from(comic)).isPresent()) {
                skipped++;
                continue;
            }

            // Skip if no source configured
            if (comic.getSource() == null || comic.getSource().isEmpty()) {
                log.debug("Skipping avatar download for '{}' - no source configured", comic.getName());
                continue;
            }

            if (fetchAvatar(comic.getId())) {
                downloaded++;
            } else {
                failed++;
            }
        }

        log.info("Avatar backfill complete: {} downloaded, {} skipped (already exist), {} failed",
                downloaded, skipped, failed);
        return downloaded;
    }

    @Override
    public boolean fetchAvatar(int comicId) {
        Optional<ComicItem> found = getComic(comicId);
        if (found.isEmpty()) {
            log.warn("Comic with ID {} not found, cannot fetch its avatar", comicId);
            return false;
        }
        ComicItem comic = found.get();
        if (comic.getSource() == null || comic.getSource().isEmpty()) {
            log.debug("Skipping avatar download for '{}' - no source configured", comic.getName());
            return false;
        }

        log.info("Downloading avatar for '{}'", comic.getName());
        Optional<byte[]> avatarData = downloaderFacade.downloadAvatar(comic.getId(), comic.getName(), comic.getSource(), comic.getSourceIdentifier());
        if (avatarData.isPresent()) {
            return saveAvatar(comicId, avatarData.get());
        }

        // Download failed: make sure the flag reflects reality
        if (comic.isAvatarAvailable() && storageFacade.getAvatar(ComicIdentifier.from(comic)).isEmpty()) {
            persist(comic.toBuilder().avatarAvailable(false).build());
        }
        log.warn("Could not download avatar for '{}'", comic.getName());
        return false;
    }

    @Override
    public boolean saveAvatar(int comicId, byte[] imageData) {
        Optional<ComicItem> found = getComic(comicId);
        if (found.isEmpty()) {
            return false;
        }
        ComicItem comic = found.get();
        if (!storageFacade.saveAvatar(ComicIdentifier.from(comic), imageData)) {
            log.error("Failed to save avatar for '{}'", comic.getName());
            return false;
        }
        // Re-read: the comic may have changed while the avatar downloaded
        ComicItem current = getComic(comicId).orElse(comic);
        if (!current.isAvatarAvailable()) {
            persist(current.toBuilder().avatarAvailable(true).build());
        }
        log.info("Saved avatar for '{}'", comic.getName());
        return true;
    }
}
