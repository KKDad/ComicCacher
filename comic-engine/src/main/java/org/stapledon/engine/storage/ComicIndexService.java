package org.stapledon.engine.storage;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicDateIndex;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

/**
 * Service to manage a persistent index of available comic dates.
 * This avoids expensive day-by-day directory scans on NFS/RAID storage.
 */
@Slf4j
@Service
public class ComicIndexService {
    public static final String INDEX_FILENAME = "available-dates.json";

    /** Synology NAS metadata directories - excluded from scanning */
    private static final String SYNOLOGY_METADATA_PREFIX = "@";

    /**
     * Pattern to validate comic names - alphanumeric, spaces, hyphens, underscores
     * only
     */
    private static final Pattern VALID_COMIC_NAME = Pattern.compile("^[a-zA-Z0-9 _-]+$");

    /** Marker for comics that have been verified as legitimately empty */
    private static final Set<Integer> VERIFIED_EMPTY_COMICS = ConcurrentHashMap.newKeySet();

    private final Gson gson;
    private final CacheProperties cacheProperties;
    private final ImageMetadataRepository metadataRepository;

    // In-memory cache of the indexes to avoid repeated disk reads.
    private final Map<Integer, ComicDateIndex> indexCache = new ConcurrentHashMap<>();

    // Per-comic locks for thread-safe index updates
    private final Map<Integer, ReadWriteLock> comicLocks = new ConcurrentHashMap<>();

    public ComicIndexService(
            @Qualifier("gsonWithLocalDate") Gson gson,
            CacheProperties cacheProperties,
            ImageMetadataRepository metadataRepository) {
        this.gson = gson;
        this.cacheProperties = cacheProperties;
        this.metadataRepository = metadataRepository;
    }

    /**
     * Sanitizes a comic name to prevent path traversal attacks.
     * Removes any characters that could be used for directory traversal.
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
        ReadWriteLock lock = getLock(comicId);
        lock.readLock().lock();
        try {
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
            int nextIndex = (pos >= 0) ? pos + 1 : -(pos + 1);

            if (nextIndex < dates.size()) {
                return Optional.of(dates.get(nextIndex));
            }
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the previous available date with a comic strip.
     */
    public Optional<LocalDate> getPreviousDate(int comicId, String comicName, LocalDate fromDate) {
        ReadWriteLock lock = getLock(comicId);
        lock.readLock().lock();
        try {
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
            int insertionPoint = (pos >= 0) ? pos : -(pos + 1);
            int prevIndex = insertionPoint - 1;

            if (prevIndex >= 0 && prevIndex < dates.size()) {
                return Optional.of(dates.get(prevIndex));
            }
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the newest available date for a comic.
     */
    public Optional<LocalDate> getNewestDate(int comicId, String comicName) {
        ReadWriteLock lock = getLock(comicId);
        lock.readLock().lock();
        try {
            ComicDateIndex index = getOrLoadIndex(comicId, comicName);
            if (index == null) {
                return Optional.empty();
            }
            List<LocalDate> dates = index.getAvailableDates();
            if (dates == null || dates.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(dates.get(dates.size() - 1));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the oldest available date for a comic.
     */
    public Optional<LocalDate> getOldestDate(int comicId, String comicName) {
        ReadWriteLock lock = getLock(comicId);
        lock.readLock().lock();
        try {
            ComicDateIndex index = getOrLoadIndex(comicId, comicName);
            if (index == null) {
                return Optional.empty();
            }
            List<LocalDate> dates = index.getAvailableDates();
            if (dates == null || dates.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(dates.get(0));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Mark a date as available and update the index.
     * Thread-safe: uses write lock to prevent concurrent modification.
     */
    public void addDateToIndex(int comicId, String comicName, LocalDate date) {
        ReadWriteLock lock = getLock(comicId);
        lock.writeLock().lock();
        try {
            ComicDateIndex index = getOrLoadIndexUnsafe(comicId, comicName);
            if (index == null) {
                // Create new index
                index = ComicDateIndex.builder()
                        .comicId(comicId)
                        .comicName(comicName)
                        .availableDates(new ArrayList<>())
                        .lastUpdated(LocalDate.now())
                        .build();
            }

            List<LocalDate> dates = index.getAvailableDates();
            if (dates == null) {
                dates = new ArrayList<>();
            }

            // Use binary search for O(log n) existence check
            int pos = Collections.binarySearch(dates, date);
            if (pos < 0) {
                // Not found - insert at correct position to maintain sorted order
                int insertionPoint = -(pos + 1);
                List<LocalDate> newDates = new ArrayList<>(dates);
                newDates.add(insertionPoint, date);
                index.setAvailableDates(newDates);
                index.setLastUpdated(LocalDate.now());

                // Update cache and persist
                indexCache.put(comicId, index);
                saveIndex(index, comicName);

                // Clear verified empty marker since we now have data
                VERIFIED_EMPTY_COMICS.remove(comicId);
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
                saveIndex(index, comicName);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Invalidate the cache for a comic (e.g., when comic is deleted).
     */
    public void invalidateCache(int comicId) {
        ReadWriteLock lock = getLock(comicId);
        lock.writeLock().lock();
        try {
            indexCache.remove(comicId);
            VERIFIED_EMPTY_COMICS.remove(comicId);
            log.debug("Invalidated cache for comic {}", comicId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private ComicDateIndex getOrLoadIndex(int comicId, String comicName) {
        // Check if this comic was verified as legitimately empty
        if (VERIFIED_EMPTY_COMICS.contains(comicId)) {
            ComicDateIndex cached = indexCache.get(comicId);
            if (cached != null) {
                return cached;
            }
        }

        return indexCache.computeIfAbsent(comicId, id -> {
            ComicDateIndex index = loadIndex(id, comicName);
            if (index.getAvailableDates().isEmpty()) {
                // Try to rebuild once
                ComicDateIndex rebuilt = rebuildAndGetIndex(id, comicName);
                if (rebuilt != null && rebuilt.getAvailableDates().isEmpty()) {
                    // Mark as verified empty to prevent future rebuilds
                    VERIFIED_EMPTY_COMICS.add(comicId);
                    log.debug("Comic {} verified as empty, will not attempt rebuild again", comicName);
                }
                return rebuilt != null ? rebuilt : index;
            }
            return index;
        });
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
            rebuildIndex(comicId, comicName);
            return indexCache.get(comicId);
        }
        indexCache.put(comicId, index);
        return index;
    }

    private ComicDateIndex loadIndex(int comicId, String comicName) {
        File indexFile = getIndexFile(comicId, comicName);
        if (indexFile.exists()) {
            try (FileReader reader = new FileReader(indexFile)) {
                ComicDateIndex index = gson.fromJson(reader, ComicDateIndex.class);
                if (index != null && index.getAvailableDates() != null) {
                    return index;
                }
            } catch (IOException e) {
                log.error("Failed to load index for comic '{}' (id={}): {}",
                        comicName, comicId, e.getMessage());
            }
        }

        return ComicDateIndex.builder()
                .comicId(comicId)
                .comicName(comicName)
                .availableDates(new ArrayList<>())
                .lastUpdated(LocalDate.now())
                .build();
    }

    private ComicDateIndex rebuildAndGetIndex(int comicId, String comicName) {
        try {
            rebuildIndex(comicId, comicName);
            ComicDateIndex result = indexCache.get(comicId);
            if (result == null) {
                // Rebuild didn't populate cache - return empty index
                log.warn("Rebuild for comic '{}' (id={}) did not populate cache, returning empty index",
                        comicName, comicId);
                result = ComicDateIndex.builder()
                        .comicId(comicId)
                        .comicName(comicName)
                        .availableDates(new ArrayList<>())
                        .lastUpdated(LocalDate.now())
                        .build();
                indexCache.put(comicId, result);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to rebuild index for comic '{}' (id={}): {}",
                    comicName, comicId, e.getMessage(), e);
            // Return empty index rather than null to prevent NPE
            ComicDateIndex emptyIndex = ComicDateIndex.builder()
                    .comicId(comicId)
                    .comicName(comicName)
                    .availableDates(new ArrayList<>())
                    .lastUpdated(LocalDate.now())
                    .build();
            indexCache.put(comicId, emptyIndex);
            return emptyIndex;
        }
    }

    private void saveIndex(ComicDateIndex index, String comicName) {
        File indexFile = getIndexFile(index.getComicId(), comicName);
        File parent = indexFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            log.error("Failed to create directory for index: {}", parent.getAbsolutePath());
            return;
        }

        try (FileWriter writer = new FileWriter(indexFile)) {
            gson.toJson(index, writer);
            writer.flush();
            log.debug("Saved index for {} with {} dates", comicName, index.getAvailableDates().size());
        } catch (IOException e) {
            log.error("Failed to save index for comic '{}': {}", comicName, e.getMessage());
        }
    }

    private File getIndexFile(int comicId, String comicName) {
        String parsedName = sanitizeComicName(comicName, comicId);
        return new File(String.format("%s/%s/%s", cacheProperties.getLocation(), parsedName, INDEX_FILENAME));
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
        String parsedName = sanitizeComicName(comicName, comicId);
        File comicDir = new File(cacheProperties.getLocation(), parsedName);

        Set<LocalDate> dateSet = new HashSet<>();
        if (comicDir.exists() && comicDir.isDirectory()) {
            File[] yearDirs = comicDir.listFiles(File::isDirectory);
            if (yearDirs != null) {
                for (File yearDir : yearDirs) {
                    // Skip Synology metadata directories
                    if (yearDir.getName().startsWith(SYNOLOGY_METADATA_PREFIX)) {
                        continue;
                    }

                    File[] images = yearDir.listFiles((dir, name) -> name.endsWith(".png"));
                    if (images != null) {
                        for (File image : images) {
                            String name = image.getName();
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
            }
        }

        List<LocalDate> sortedDates = new ArrayList<>(dateSet);
        Collections.sort(sortedDates);

        ComicDateIndex index = ComicDateIndex.builder()
                .comicId(comicId)
                .comicName(comicName)
                .availableDates(sortedDates)
                .lastUpdated(LocalDate.now())
                .build();

        indexCache.put(comicId, index);
        saveIndex(index, comicName);
        log.info("Rebuilt index for '{}' with {} available dates on disk", comicName, sortedDates.size());
    }

    /**
     * Validates that an image's metadata matches the expected comic ID.
     */
    private void validateImageMetadata(File image, int comicId, String comicName, LocalDate date) {
        if (metadataRepository.metadataExists(image.getAbsolutePath())) {
            metadataRepository.loadMetadata(image.getAbsolutePath()).ifPresent(md -> {
                if (md.getComicId() != comicId) {
                    log.error("MISMATCH: Comic ID mismatch for '{}' on {}. Expected {}, found {}",
                            comicName, date, comicId, md.getComicId());
                }
            });
        } else {
            log.warn("MISSING: Metadata sidecar missing for '{}' on {}", comicName, date);
        }
    }
}
