package org.stapledon.engine.storage;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicDateIndex;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage a persistent index of available comic dates.
 * This avoids expensive day-by-day directory scans on NFS/RAID storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComicIndexService {
    public static final String INDEX_FILENAME = "available-dates.json";

    @Qualifier("gsonWithLocalDate")
    private final Gson gson;

    private final CacheProperties cacheProperties;
    private final ImageMetadataRepository metadataRepository;

    // In-memory cache of the indexes to avoid repeated disk reads.
    private final Map<Integer, ComicDateIndex> indexCache = new ConcurrentHashMap<>();

    /**
     * Get the next available date with a comic strip.
     */
    public Optional<LocalDate> getNextDate(int comicId, String comicName, LocalDate fromDate) {
        ComicDateIndex index = getOrLoadIndex(comicId, comicName);
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
    }

    /**
     * Get the previous available date with a comic strip.
     */
    public Optional<LocalDate> getPreviousDate(int comicId, String comicName, LocalDate fromDate) {
        ComicDateIndex index = getOrLoadIndex(comicId, comicName);
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
    }

    /**
     * Get the newest available date for a comic.
     */
    public Optional<LocalDate> getNewestDate(int comicId, String comicName) {
        ComicDateIndex index = getOrLoadIndex(comicId, comicName);
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
        List<LocalDate> dates = index.getAvailableDates();
        if (dates == null || dates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(dates.get(0));
    }

    /**
     * Mark a date as available and update the index.
     */
    public void addDateToIndex(int comicId, String comicName, LocalDate date) {
        ComicDateIndex index = getOrLoadIndex(comicId, comicName);
        List<LocalDate> dates = new ArrayList<>(index.getAvailableDates());
        if (!dates.contains(date)) {
            dates.add(date);
            Collections.sort(dates);
            index.setAvailableDates(dates);
            index.setLastUpdated(LocalDate.now());
            saveIndex(index, comicName);
        }
    }

    private ComicDateIndex getOrLoadIndex(int comicId, String comicName) {
        return indexCache.computeIfAbsent(comicId, id -> {
            ComicDateIndex index = loadIndex(id, comicName);
            if (index.getAvailableDates().isEmpty()) {
                // If index is empty, try to rebuild once
                return rebuildAndGetIndex(id, comicName);
            }
            return index;
        });
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
                log.error("Failed to load index for {}: {}", comicName, e.getMessage());
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
        rebuildIndex(comicId, comicName);
        return indexCache.get(comicId);
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
            log.error("Failed to save index for {}: {}", comicName, e.getMessage());
        }
    }

    private File getIndexFile(int comicId, String comicName) {
        String parsedName = comicName != null ? comicName.replace(" ", "") : "comic_" + comicId;
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
        String parsedName = comicName != null ? comicName.replace(" ", "") : "comic_" + comicId;
        File comicDir = new File(cacheProperties.getLocation(), parsedName);

        Set<LocalDate> dateSet = new HashSet<>();
        if (comicDir.exists() && comicDir.isDirectory()) {
            File[] yearDirs = comicDir.listFiles(File::isDirectory);
            if (yearDirs != null) {
                for (File yearDir : yearDirs) {
                    if (yearDir.getName().startsWith("@"))
                        continue;

                    File[] images = yearDir.listFiles((dir, name) -> name.endsWith(".png"));
                    if (images != null) {
                        for (File image : images) {
                            String name = image.getName();
                            try {
                                String dateStr = name.substring(0, name.lastIndexOf('.'));
                                LocalDate date = LocalDate.parse(dateStr);

                                if (validateMetadata) {
                                    // Verify that the sidecar .json exists and contains the correct comicId
                                    if (metadataRepository.metadataExists(image.getAbsolutePath())) {
                                        metadataRepository.loadMetadata(image.getAbsolutePath()).ifPresent(md -> {
                                            if (md.getComicId() != comicId) {
                                                log.error(
                                                        "MISMATCH: Comic ID mismatch for {} on {}. Expected {}, found {}",
                                                        comicName, date, comicId, md.getComicId());
                                            }
                                        });
                                    } else {
                                        log.warn("MISSING: Metadata sidecar missing for {} on {}", comicName, date);
                                    }
                                }

                                dateSet.add(date);
                            } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
                                log.warn("Skipping invalid file {}: {}", name, e.getMessage());
                            } catch (Exception e) {
                                log.error("Error processing file {}: {}", name, e.getMessage(), e);
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
        log.info("Rebuilt index for {} with {} available dates on disk", comicName, sortedDates.size());
    }
}
