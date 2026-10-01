package org.stapledon.engine.source;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.util.NfsFileOperations;
import org.stapledon.engine.source.SourceCatalogState.Entry;
import org.stapledon.engine.source.SourceCatalogState.SourceEntries;

/**
 * Reads and writes {@code source-catalog.json} in the cache root. The state is loaded once and kept in memory; every change is written straight
 * back, atomically. Readers get copies, so a refresh running on another thread never changes what they hold.
 */
@Slf4j
@Repository
public class SourceCatalogRepository {

    static final String FILENAME = "source-catalog.json";

    private final CacheProperties cacheProperties;
    private final Gson gson;
    private final Clock clock;

    private SourceCatalogState state;

    @Autowired
    public SourceCatalogRepository(CacheProperties cacheProperties, @Qualifier("gsonWithLocalDate") Gson gson) {
        this(cacheProperties, gson, Clock.systemUTC());
    }

    SourceCatalogRepository(CacheProperties cacheProperties, Gson gson, Clock clock) {
        this.cacheProperties = cacheProperties;
        this.gson = gson;
        this.clock = clock;
    }

    /**
     * A copy of one source's catalog, or empty before its first refresh attempt.
     */
    public synchronized Optional<SourceEntries> find(String source) {
        return Optional.ofNullable(load().getSources().get(source)).map(SourceCatalogRepository::copy);
    }

    /**
     * Merges a fresh read of a source's catalog: new identifiers are added, listed ones updated and un-removed, and ones the source no longer lists are
     * marked removed. Clears the last error.
     */
    public synchronized MergeResult merge(String source, List<SourceCatalogEntry> fetched) {
        OffsetDateTime now = now();
        SourceEntries entries = load().getSources().computeIfAbsent(source, _ -> new SourceEntries());
        Set<String> seen = new HashSet<>();
        int added = 0;
        int returned = 0;
        for (SourceCatalogEntry fetchedEntry : fetched) {
            seen.add(fetchedEntry.identifier());
            Entry existing = entries.getEntries().get(fetchedEntry.identifier());
            if (existing == null) {
                added++;
                existing = Entry.builder().firstSeen(now).build();
                entries.getEntries().put(fetchedEntry.identifier(), existing);
            } else if (existing.getRemovedAt() != null) {
                returned++;
            }
            existing.setName(fetchedEntry.name());
            existing.setAuthor(fetchedEntry.author());
            existing.setThumbnailUrl(fetchedEntry.thumbnailUrl());
            if (fetchedEntry.startDate() != null) {
                existing.setStartDate(fetchedEntry.startDate());
                existing.setStartCheckedAt(now);
            }
            if (fetchedEntry.details() != null) {
                existing.setDescription(fetchedEntry.details().description());
                existing.setTags(fetchedEntry.details().tags());
                existing.setDetailsCheckedAt(now);
            }
            existing.setLastSeen(now);
            existing.setRemovedAt(null);
        }
        int removed = 0;
        for (Map.Entry<String, Entry> e : entries.getEntries().entrySet()) {
            if (!seen.contains(e.getKey()) && e.getValue().getRemovedAt() == null) {
                e.getValue().setRemovedAt(now);
                removed++;
            }
        }
        entries.setLastRefreshed(now);
        entries.setLastAttempt(now);
        entries.setLastError(null);
        save();
        return new MergeResult(fetched.size(), added, removed, returned);
    }

    /**
     * Records a failed refresh. The catalog itself is kept as it was.
     */
    public synchronized void recordFailure(String source, String error) {
        SourceEntries entries = load().getSources().computeIfAbsent(source, _ -> new SourceEntries());
        entries.setLastAttempt(now());
        entries.setLastError(error);
        save();
    }

    /**
     * Records where the source says a comic starts. Does nothing for a comic the catalog doesn't list.
     */
    public synchronized void recordStart(String source, String identifier, StartInfo start) {
        OffsetDateTime now = now();
        SourceEntries entries = load().getSources().computeIfAbsent(source, _ -> new SourceEntries());
        Entry entry = entries.getEntries().get(identifier);
        if (entry == null) {
            return;
        }
        entry.setStartDate(start.date());
        entry.setStartStripNumber(start.stripNumber());
        entry.setStartCheckedAt(now);
        save();
    }

    /**
     * Records a comic's details read from its own page, and when to read them again. Null {@code details} records a failed read: the stored details
     * are kept and only the time of the attempt and the retry change. Does nothing for a comic the catalog doesn't list.
     */
    public synchronized void recordDetails(String source, String identifier, CatalogDetails details, OffsetDateTime expireAt) {
        Entry entry = entry(source, identifier);
        if (entry == null) {
            return;
        }
        if (details != null) {
            entry.setDescription(details.description());
            entry.setTags(details.tags());
        }
        entry.setDetailsCheckedAt(now());
        entry.setDetailsExpireAt(expireAt.withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
        save();
    }

    /**
     * What happened to a comic's thumbnail file.
     */
    public enum ThumbnailEvent {
        SAVED,
        FAILED,
        DELETED
    }

    /**
     * Records a thumbnail download or deletion, so the job knows which thumbnails are due. Does nothing for a comic the catalog doesn't list.
     */
    public synchronized void recordThumbnail(String source, String identifier, ThumbnailEvent event) {
        Entry entry = entry(source, identifier);
        if (entry == null) {
            return;
        }
        if (event == ThumbnailEvent.SAVED) {
            entry.setThumbnailSavedAt(now());
            entry.setThumbnailFailedAt(null);
        } else if (event == ThumbnailEvent.FAILED) {
            entry.setThumbnailFailedAt(now());
        } else if (entry.getThumbnailSavedAt() != null) {
            entry.setThumbnailSavedAt(null);
        } else {
            // Deleting a file the catalog didn't record as saved changes nothing
            return;
        }
        save();
    }

    /**
     * True when a listed entry's thumbnail is due: it has an image URL, none was saved, and no download failed in the last {@code failureRetry}.
     */
    public static boolean isThumbnailDue(Entry entry, OffsetDateTime now, Duration failureRetry) {
        return entry.getRemovedAt() == null && entry.getThumbnailUrl() != null && entry.getThumbnailSavedAt() == null
                && (entry.getThumbnailFailedAt() == null || !entry.getThumbnailFailedAt().plus(failureRetry).isAfter(now));
    }

    /**
     * True when a listed entry's details are due: never read, or past their expiry.
     */
    public static boolean isDetailsDue(Entry entry, OffsetDateTime now) {
        return entry.getRemovedAt() == null && (entry.getDetailsExpireAt() == null || !entry.getDetailsExpireAt().isAfter(now));
    }

    /**
     * True when some thumbnail is due, or details are due for a source in {@code withDetails}. Reads only the stored catalog, never the disk or a source.
     */
    public synchronized boolean hasDueWork(Set<String> withDetails, Duration thumbnailFailureRetry) {
        OffsetDateTime now = now();
        return load().getSources().entrySet().stream().anyMatch(source -> source.getValue().getEntries().values().stream()
                .anyMatch(entry -> isThumbnailDue(entry, now, thumbnailFailureRetry) || withDetails.contains(source.getKey()) && isDetailsDue(entry, now)));
    }

    private Entry entry(String source, String identifier) {
        SourceEntries entries = load().getSources().get(source);
        return entries == null ? null : entries.getEntries().get(identifier);
    }

    /**
     * Counts of what a refresh changed.
     */
    public record MergeResult(int total, int added, int removed, int returned) {
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }

    private SourceCatalogState load() {
        if (state != null) {
            return state;
        }
        Path file = file();
        state = new SourceCatalogState();
        if (!NfsFileOperations.exists(file)) {
            return state;
        }
        try {
            SourceCatalogState loaded = gson.fromJson(NfsFileOperations.readAsString(file), SourceCatalogState.class);
            if (loaded != null && loaded.getSources() != null) {
                loaded.getSources().forEach((source, entries) -> {
                    if (entries.getEntries() == null) {
                        entries.setEntries(new TreeMap<>());
                    }
                    state.getSources().put(source, entries);
                });
            }
        } catch (IOException | JsonSyntaxException e) {
            log.error("Failed to read {}, starting with an empty source catalog", file, e);
        }
        return state;
    }

    private void save() {
        Path file = file();
        try {
            NfsFileOperations.atomicWrite(file, gson.toJson(load()));
        } catch (IOException e) {
            log.error("Failed to write the source catalog {}", file, e);
        }
    }

    private Path file() {
        return Path.of(cacheProperties.getLocation(), FILENAME);
    }

    private static SourceEntries copy(SourceEntries entries) {
        Map<String, Entry> copied = new TreeMap<>();
        entries.getEntries().forEach((id, entry) -> copied.put(id, entry.toBuilder().build()));
        return entries.toBuilder().entries(copied).build();
    }
}
