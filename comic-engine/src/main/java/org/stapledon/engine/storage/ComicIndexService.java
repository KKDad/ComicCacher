package org.stapledon.engine.storage;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicDateIndex;
import org.stapledon.common.util.NfsFileOperations;

/**
 * Service to manage a persistent index of available comic dates. This avoids
 * expensive day-by-day directory scans on NFS/RAID storage.
 */
@Slf4j
@Service
@lombok.RequiredArgsConstructor
public class ComicIndexService {
    public static final String INDEX_FILENAME = "available-dates.json";
    public static final String STRIP_INDEX_FILENAME = "downloaded-strips.json";

    /** Synology NAS metadata directories - excluded from scanning */
    private static final String SYNOLOGY_METADATA_PREFIX = "@";

    /**
     * Pattern to validate comic names - alphanumeric, spaces, hyphens, underscores
     * only
     */
    private static final Pattern VALID_COMIC_NAME = Pattern.compile("^[a-zA-Z0-9 _-]+$");

    @Qualifier("gsonWithLocalDate")
    private final Gson gson;
    private final CacheProperties cacheProperties;
    private final ImageMetadataRepository metadataRepository;

    // In-memory cache of the indexes to avoid repeated disk reads.
    private final Map<Integer, ComicDateIndex> indexCache = new ConcurrentHashMap<>();

    // In-memory cache of downloaded strip numbers for indexed comics.
    private final Map<Integer, Set<Integer>> stripIndexCache = new ConcurrentHashMap<>();

    /** Marker for comics that have been verified as legitimately empty */
    private final Set<Integer> verifiedEmptyComics = ConcurrentHashMap.newKeySet();

    // Per-comic locks for thread-safe index updates
    private final Map<Integer, ReadWriteLock> comicLocks = new ConcurrentHashMap<>();

    /**
     * Sanitizes a comic name to prevent path traversal attacks. Removes any
     * characters that could be used for directory traversal.
     *
     * @param comicName the comic name to sanitize
     * @param comicId   fallback ID if name is invalid
     * @return sanitized name safe for filesystem operations
     */
    private String sanitizeComicName(String comicName, int comicId) {
        if (comicName == null || comicName.trim().isEmpty()) {
            return "comic_" + comicId;
        }

        String trimmed = comicName.trim();

        // Validate against allowed pattern
        if (!VALID_COMIC_NAME.matcher(trimmed).matches()) {
            log.warn("Comic name '{}' contains invalid characters, using fallback", comicName);
            return "comic_" + comicId;
        }

        // Remove spaces for directory name
        return trimmed.replace(" ", "");
    }

    /**
     * Get the lock for a specific comic, creating if necessary.
     */
    private ReadWriteLock getLock(int comicId) {
        return comicLocks.computeIfAbsent(comicId, id -> new ReentrantReadWriteLock());
    }

    /**
     * Get the next available date with a comic strip.
     */
    public Optional<LocalDate> getNextDate(int comicId, String comicName, LocalDate fromDate) {
        ComicDateIndex index = getOrLoadIndex(comicId, comicName);
        if (index == null) {
            return Optional.empty();
        }
        List<LocalDate> dates = index.getAvailableDates();
        if (dates == null || dates.isEmpty()) {
            return Optional.empty();
        }

        int pos = Collections.binarySearch(dates, fromDate);

        // If fromDate is found, the next one is at index + 1
        // If fromDate is NOT found, binarySearch returns (-(insertion point) - 1)
        int nextIndex = pos >= 0 ? pos + 1 : -(pos + 1);

        if (nextIndex < dates.size()) {
            return Optional.of(dates.get(nextIndex));
        }
        return Optional.empty();
    }

    /**
     * Get the previous available date with a comic strip.
     */
    public Optional<LocalDate> getPreviousDate(int comicId, String comicName, LocalDate fromDate) {
        ComicDateIndex index = getOrLoadIndex(comicId, comicName);
        if (index == null) {
            return Optional.empty();
        }
        List<LocalDate> dates = index.getAvailableDates();
        if (dates == null || dates.isEmpty()) {
            return Optional.empty();
        }

        int pos = Collections.binarySearch(dates, fromDate);

        // If found, previous is at index - 1.
        // If not found, pos is (-(insertion point) - 1).
        // Insertion point is the first element greater than the key.
        // We want the element immediately before the insertion point.
        int insertionPoint = pos >= 0 ? pos : -(pos + 1);
        int prevIndex = insertionPoint - 1;

        if (prevIndex >= 0 && prevIndex < dates.size()) {
            return Optional.of(dates.get(prevIndex));
        }
        return Optional.empty();
    }

    /**
     * Get the newest available date for a comic.
     */
    public Optional<LocalDate> getNewestDate(int comicId, String comicName) {
        ComicDateIndex index = getOrLoadIndex(comicId, comicName);
        if (index == null) {
            return Optional.empty();
        }
        List<LocalDate> dates = index.getAvailableDates();
        if (dates == null || dates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(dates.get(dates.size() - 1));
    }

    /**
     * Get the oldest available date for a comic.
     */
    public Optional<LocalDate> getOldestDate(int comicId, String comicName) {
        ComicDateIndex index = getOrLoadIndex(comicId, comicName);
        if (index == null) {
            return Optional.empty();
        }
        List<LocalDate> dates = index.getAvailableDates();
        if (dates == null || dates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(dates.get(0));
    }

    /**
     * Get all available dates for a comic, sorted in ascending order.
     */
    public List<LocalDate> getAvailableDates(int comicId, String comicName) {
        ComicDateIndex index = getOrLoadIndex(comicId, comicName);
        if (index == null) {
            return List.of();
        }
        List<LocalDate> dates = index.getAvailableDates();
        if (dates == null || dates.isEmpty()) {
            return List.of();
        }
        return List.copyOf(dates);
    }

    /**
     * Mark a date as available and update the index. Thread-safe: uses write lock
     * to prevent concurrent modification.
     */
    public void addDateToIndex(int comicId, String comicName, LocalDate date) {
        log.debug("addDateToIndex called: comicId={}, comicName={}, date={}", comicId, comicName, date);
        ReadWriteLock lock = getLock(comicId);
        lock.writeLock().lock();
        try {
            ComicDateIndex index = getOrLoadIndexUnsafe(comicId, comicName);
            if (index == null) {
                log.debug("Index is null, creating new index for comic {} (id={})", comicName, comicId);
                // Create new index
                index = ComicDateIndex.builder().comicId(comicId).comicName(comicName).availableDates(new ArrayList<>())
                        .lastUpdated(LocalDate.now()).build();
            }

            List<LocalDate> dates = index.getAvailableDates();
            if (dates == null) {
                dates = new ArrayList<>();
            }

            log.debug("Index before update: {} dates", dates.size());

            // Use binary search for O(log n) existence check
            int pos = Collections.binarySearch(dates, date);
            if (pos < 0) {
                // Not found - insert at correct position to maintain sorted order
                int insertionPoint = -(pos + 1);
                List<LocalDate> newDates = new ArrayList<>(dates);
                newDates.add(insertionPoint, date);
                index.setAvailableDates(newDates);
                index.setLastUpdated(LocalDate.now());

                log.debug("Index after update: {} dates", newDates.size());

                // Write to disk FIRST - this may throw IOException
                try {
                    saveIndex(index, comicName);
                } catch (IOException e) {
                    log.error("Failed to persist index for {}, cache NOT updated", comicName, e);
                    // Don't update cache - keep old state consistent with disk
                    throw new RuntimeException("Failed to persist index to disk", e);
                }

                // Only update cache if disk write succeeded
                indexCache.put(comicId, index);

                // Clear verified empty marker since we now have data
                verifiedEmptyComics.remove(comicId);

                log.info("Added date {} to index for comic {}", date, comicName);
            } else {
                log.debug("Date {} already exists in index at position {}, skipping", date, pos);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove a date from the index (e.g., when an image is deleted).
     */
    public void removeDateFromIndex(int comicId, String comicName, LocalDate date) {
        ReadWriteLock lock = getLock(comicId);
        lock.writeLock().lock();
        try {
            ComicDateIndex index = indexCache.get(comicId);
            if (index == null) {
                return;
            }

            List<LocalDate> dates = new ArrayList<>(index.getAvailableDates());
            if (dates.remove(date)) {
                index.setAvailableDates(dates);
                index.setLastUpdated(LocalDate.now());
                try {
                    saveIndex(index, comicName);
                } catch (IOException e) {
                    log.error("Failed to persist index after removing date {} for {}", date, comicName, e);
                    // Note: Cache will be out of sync with disk until next rebuild
                    throw new RuntimeException("Failed to persist index to disk", e);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the set of downloaded strip numbers for an indexed comic.
     * Loads from disk on first access and caches in memory.
     */
    public Set<Integer> getDownloadedStripNumbers(int comicId, String comicName) {
        return stripIndexCache.computeIfAbsent(comicId, id -> loadStripIndex(id, comicName));
    }

    /**
     * Records a downloaded strip number in the index.
     * Thread-safe: uses write lock to prevent concurrent modification.
     */
    public void addStripNumberToIndex(int comicId, String comicName, int stripNumber) {
        ReadWriteLock lock = getLock(comicId);
        lock.writeLock().lock();
        try {
            Set<Integer> strips = stripIndexCache.computeIfAbsent(comicId, id -> loadStripIndex(id, comicName));

            if (strips.add(stripNumber)) {
                List<Integer> sorted = new ArrayList<>(strips);
                Collections.sort(sorted);
                try {
                    saveStripIndex(sorted, comicName, comicId);
                } catch (IOException e) {
                    // Roll back cache on disk failure
                    strips.remove(stripNumber);
                    log.error("Failed to persist strip index for {}: {}", comicName, e.getMessage());
                    throw new RuntimeException("Failed to persist strip index to disk", e);
                }
                log.debug("Added strip #{} to index for comic {}", stripNumber, comicName);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Set<Integer> loadStripIndex(int comicId, String comicName) {
        Path indexFile = getStripIndexFile(comicId, comicName);
        if (NfsFileOperations.exists(indexFile)) {
            try (Reader reader = Files.newBufferedReader(indexFile)) {
                int[] numbers = gson.fromJson(reader, int[].class);
                if (numbers != null) {
                    Set<Integer> result = ConcurrentHashMap.newKeySet();
                    for (int n : numbers) {
                        result.add(n);
                    }
                    return result;
                }
            } catch (IOException e) {
                log.error("Failed to load strip index for '{}' (id={}): {}", comicName, comicId, e.getMessage());
            }
        }
        return ConcurrentHashMap.newKeySet();
    }

    private void saveStripIndex(List<Integer> sortedStrips, String comicName, int comicId) throws IOException {
        Path indexFile = getStripIndexFile(comicId, comicName);
        String json = gson.toJson(sortedStrips);
        NfsFileOperations.atomicWrite(indexFile, json);
    }

    private Path getStripIndexFile(int comicId, String comicName) {
        String parsedName = sanitizeComicName(comicName, comicId);
        return NfsFileOperations.resolvePath(cacheProperties.getLocation(), parsedName, STRIP_INDEX_FILENAME);
    }

    /**
     * Invalidate the cache for a comic (e.g., when comic is deleted).
     */
    public void invalidateCache(int comicId) {
        ReadWriteLock lock = getLock(comicId);
        lock.writeLock().lock();
        try {
            indexCache.remove(comicId);
            stripIndexCache.remove(comicId);
            verifiedEmptyComics.remove(comicId);
            log.debug("Invalidated cache for comic {}", comicId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Rebuild the entire index from scratch by scanning the filesystem.
     */
    public void rebuildIndex(int comicId, String comicName) {
        rebuildIndex(comicId, comicName, false);
    }

    /**
     * Rebuild the entire index from scratch by scanning the filesystem.
     *
     * @param validateMetadata If true, reads each sidecar JSON to verify the
     *                         comicId matches.
     */
    public void rebuildIndex(int comicId, String comicName, boolean validateMetadata) {
        ReadWriteLock lock = getLock(comicId);
        lock.writeLock().lock();
        try {
            ComicDateIndex index = rebuildIndexInternal(comicId, comicName, validateMetadata);

            // Write to disk first, then update cache
            try {
                saveIndex(index, comicName);
            } catch (IOException e) {
                log.error("Failed to persist rebuilt index for {}, cache NOT updated", comicName, e);
                throw new RuntimeException("Failed to persist rebuilt index to disk", e);
            }

            indexCache.put(comicId, index);
            log.info("Rebuilt index for '{}' with {} available dates on disk", comicName, index.getAvailableDates().size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Internal rebuild method that does not acquire locks.
     * Caller must hold write lock.
     */
    private ComicDateIndex rebuildIndexInternal(int comicId, String comicName) {
        return rebuildIndexInternal(comicId, comicName, false);
    }

    /**
     * Internal rebuild method that does not acquire locks.
     * Caller must hold write lock.
     *
     * @param validateMetadata If true, reads each sidecar JSON to verify the comicId matches.
     */
    private ComicDateIndex rebuildIndexInternal(int comicId, String comicName, boolean validateMetadata) {
        String parsedName = sanitizeComicName(comicName, comicId);
        Path comicDir = NfsFileOperations.resolvePath(cacheProperties.getLocation(), parsedName);

        Set<LocalDate> dateSet = new HashSet<>();
        if (Files.isDirectory(comicDir)) {
            try (DirectoryStream<Path> yearDirs = Files.newDirectoryStream(comicDir, Files::isDirectory)) {
                for (Path yearDir : yearDirs) {
                    // Skip Synology metadata directories
                    if (yearDir.getFileName().toString().startsWith(SYNOLOGY_METADATA_PREFIX)) {
                        continue;
                    }

                    try (DirectoryStream<Path> images = Files.newDirectoryStream(yearDir, "*.png")) {
                        for (Path image : images) {
                            String name = image.getFileName().toString();
                            try {
                                String dateStr = name.substring(0, name.lastIndexOf('.'));
                                LocalDate date = LocalDate.parse(dateStr);

                                if (validateMetadata) {
                                    validateImageMetadata(image, comicId, comicName, date);
                                }

                                dateSet.add(date);
                            } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
                                log.warn("Skipping invalid file '{}': {}", name, e.getMessage());
                            } catch (Exception e) {
                                log.error("Error processing file '{}': {}", name, e.getMessage(), e);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Failed to scan directory {}: {}", comicDir, e.getMessage());
            }
        }

        List<LocalDate> sortedDates = new ArrayList<>(dateSet);
        Collections.sort(sortedDates);

        return ComicDateIndex.builder()
                .comicId(comicId)
                .comicName(comicName)
                .availableDates(sortedDates)
                .lastUpdated(LocalDate.now())
                .build();
    }

    private ComicDateIndex getOrLoadIndex(int comicId, String comicName) {
        ReadWriteLock lock = getLock(comicId);

        // First try with read lock (fast path for already-cached case)
        lock.readLock().lock();
        try {
            ComicDateIndex cached = indexCache.get(comicId);
            if (cached != null) {
                return cached;
            }
        } finally {
            lock.readLock().unlock();
        }

        // Need to load/rebuild - acquire write lock
        lock.writeLock().lock();
        try {
            // Double-check after acquiring write lock (another thread may have loaded it)
            ComicDateIndex cached = indexCache.get(comicId);
            if (cached != null) {
                return cached;
            }

            // Load index from disk
            ComicDateIndex index = loadIndex(comicId, comicName);

            // If empty, try to rebuild from filesystem
            if (index.getAvailableDates().isEmpty()) {
                // Check if this comic was already verified as empty
                if (!verifiedEmptyComics.contains(comicId)) {
                    index = rebuildIndexInternal(comicId, comicName);

                    // If still empty after rebuild, mark as verified empty
                    if (index.getAvailableDates().isEmpty()) {
                        verifiedEmptyComics.add(comicId);
                        log.debug("Comic {} verified as empty, will not attempt rebuild again", comicName);
                    }
                }
            }

            // Update cache and return
            indexCache.put(comicId, index);
            return index;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Unsafe version for use within write lock (avoids double-locking).
     */
    private ComicDateIndex getOrLoadIndexUnsafe(int comicId, String comicName) {
        ComicDateIndex cached = indexCache.get(comicId);
        if (cached != null) {
            return cached;
        }

        ComicDateIndex index = loadIndex(comicId, comicName);
        if (index.getAvailableDates().isEmpty()) {
            // Call internal method directly — caller already holds the write lock
            index = rebuildIndexInternal(comicId, comicName);
            if (!index.getAvailableDates().isEmpty()) {
                try {
                    saveIndex(index, comicName);
                } catch (IOException e) {
                    log.error("Failed to persist rebuilt index for {}", comicName, e);
                }
            }
        }
        indexCache.put(comicId, index);
        return index;
    }

    private ComicDateIndex loadIndex(int comicId, String comicName) {
        Path indexFile = getIndexFile(comicId, comicName);
        if (NfsFileOperations.exists(indexFile)) {
            try (Reader reader = Files.newBufferedReader(indexFile)) {
                ComicDateIndex index = gson.fromJson(reader, ComicDateIndex.class);
                if (index != null && index.getAvailableDates() != null) {
                    return index;
                }
            } catch (IOException e) {
                log.error("Failed to load index for comic '{}' (id={}): {}", comicName, comicId, e.getMessage());
            }
        }

        return ComicDateIndex.builder().comicId(comicId).comicName(comicName).availableDates(new ArrayList<>())
                .lastUpdated(LocalDate.now()).build();
    }

    /**
     * Saves index to disk using atomic write for NFS safety.
     *
     * @throws IOException if the index cannot be written to disk
     */
    private void saveIndex(ComicDateIndex index, String comicName) throws IOException {
        Path indexFile = getIndexFile(index.getComicId(), comicName);

        log.info("Saving index to: {}", indexFile);
        log.info("Index contains {} dates: {}", index.getAvailableDates().size(),
                 index.getAvailableDates().isEmpty() ? "[]"
                 : "[" + index.getAvailableDates().get(0) + "..."
                 + index.getAvailableDates().get(index.getAvailableDates().size() - 1) + "]");

        String json = gson.toJson(index);
        NfsFileOperations.atomicWrite(indexFile, json);
        log.info("Successfully saved index for {} with {} dates", comicName, index.getAvailableDates().size());
    }

    private Path getIndexFile(int comicId, String comicName) {
        String parsedName = sanitizeComicName(comicName, comicId);
        return NfsFileOperations.resolvePath(cacheProperties.getLocation(), parsedName, INDEX_FILENAME);
    }

    /**
     * Validates that an image's metadata matches the expected comic ID.
     */
    private void validateImageMetadata(Path image, int comicId, String comicName, LocalDate date) {
        String imagePath = image.toAbsolutePath().toString();
        if (metadataRepository.metadataExists(imagePath)) {
            metadataRepository.loadMetadata(imagePath).ifPresent(md -> {
                if (md.getComicId() != comicId) {
                    log.error("MISMATCH: Comic ID mismatch for '{}' on {}. Expected {}, found {}", comicName, date,
                            comicId, md.getComicId());
                }
            });
        } else {
            log.warn("MISSING: Metadata sidecar missing for '{}' on {}", comicName, date);
        }
    }
}
