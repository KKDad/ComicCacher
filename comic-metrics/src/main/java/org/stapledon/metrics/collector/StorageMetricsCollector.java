package org.stapledon.metrics.collector;

import org.springframework.beans.factory.annotation.Qualifier;
import org.stapledon.common.dto.ComicStorageMetrics;
import org.stapledon.common.dto.ImageCacheStats;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Collector for storage metrics. Scans the cache directory and computes storage
 * utilization statistics. This collector only computes metrics in-memory;
 * persistence is handled by
 * MetricsRepository.
 */
@Slf4j
@ToString
public class StorageMetricsCollector {
    private final String cacheDirectory;

    private ImageCacheStats cacheStats;

    public StorageMetricsCollector(@Qualifier("cacheLocation") String targetDirectory) {
        this.cacheDirectory = targetDirectory;
    }

    public ImageCacheStats cacheStats() {
        if (cacheStats == null) {
            updateStats();
        }
        return cacheStats;
    }

    /**
     * Generate Statistics about the Images cached in a particular directory.
     *
     * @return True if successful
     */
    public boolean updateStats() {
        long startTime = System.currentTimeMillis();
        log.info("Starting storage metrics scan...");

        var root = new File(cacheDirectory);
        if (!root.exists()) {
            log.error("{} doesn't exist", cacheDirectory);
            return false;
        }

        // Initialize with empty metrics
        cacheStats = new ImageCacheStats();

        // Get all comic directories (one level down from root)
        File[] comicDirs = root.listFiles(file -> file.isDirectory() && !"@eaDir".equals(file.getName()));
        if (comicDirs == null || comicDirs.length == 0) {
            log.warn("No comic directories found in {}", cacheDirectory);
            return true;
        }

        // Process each comic directory to gather metrics
        Map<String, ComicStorageMetrics> perComicMetrics = new HashMap<>();
        long totalStorageBytes = 0;

        for (File comicDir : comicDirs) {
            ComicStorageMetrics metrics = calculateComicMetrics(comicDir);
            perComicMetrics.put(comicDir.getName(), metrics);
            totalStorageBytes += metrics.getStorageBytes();
        }

        // Gather all years across all comics
        java.util.Set<String> allYearsSet = new java.util.TreeSet<>();
        for (File comicDir : comicDirs) {
            String[] years = comicDir
                    .list((dir, name) -> new File(dir, name).isDirectory() && !"@eaDir".equals(name));
            if (years != null) {
                allYearsSet.addAll(Arrays.asList(years));
            }
        }

        if (!allYearsSet.isEmpty()) {
            String[] allYearsSorted = allYearsSet.toArray(new String[0]);
            Arrays.sort(allYearsSorted, Comparator.comparing(Integer::valueOf));
            var years = Arrays.asList(allYearsSorted);

            // Aggregate year-based statistics across all comics
            Map<String, Integer> imageCountByYear = new HashMap<>();
            Map<String, Long> storageBytesByYear = new HashMap<>();

            for (ComicStorageMetrics metrics : perComicMetrics.values()) {
                if (metrics.getStorageByYear() != null) {
                    metrics.getStorageByYear().forEach((year, bytes) ->
                            storageBytesByYear.merge(year, bytes, Long::sum));
                }
            }

            // Calculate image counts per year by scanning each year directory
            for (String year : years) {
                int yearImageCount = 0;
                for (File comicDir : comicDirs) {
                    File yearDir = new File(comicDir, year);
                    if (yearDir.exists() && yearDir.isDirectory()) {
                        File[] images = yearDir.listFiles(file -> file.isFile()
                                && (file.getName().endsWith(".png") || file.getName().endsWith(".jpg")));
                        if (images != null) {
                            yearImageCount += images.length;
                        }
                    }
                }
                imageCountByYear.put(year, yearImageCount);
            }

            // Find oldest and newest images across all comics
            String oldestComicName = null;
            String oldestYear = allYearsSorted[0];
            String oldestImage = null;
            String newestComicName = null;
            String newestYear = allYearsSorted[allYearsSorted.length - 1];
            String newestImage = null;

            // Scan all comics to find the one with the oldest image
            for (File comicDir : comicDirs) {
                File yearDir = new File(comicDir, oldestYear);
                if (yearDir.exists()) {
                    String first = firstImage(yearDir.getAbsolutePath());
                    if (first != null && (oldestImage == null || first.compareTo(oldestImage) < 0)) {
                        oldestImage = first;
                        oldestComicName = comicDir.getName();
                    }
                }
            }

            // Scan all comics to find the one with the newest image
            for (File comicDir : comicDirs) {
                File yearDir = new File(comicDir, newestYear);
                if (yearDir.exists()) {
                    String last = lastImage(yearDir.getAbsolutePath());
                    if (last != null && (newestImage == null || last.compareTo(newestImage) > 0)) {
                        newestImage = last;
                        newestComicName = comicDir.getName();
                    }
                }
            }

            cacheStats = ImageCacheStats.builder().years(years)
                    .oldestImage(buildImagePath(oldestComicName, oldestYear, oldestImage))
                    .newestImage(buildImagePath(newestComicName, newestYear, newestImage))
                    .totalStorageBytes(totalStorageBytes).perComicMetrics(perComicMetrics)
                    .imageCountByYear(imageCountByYear).storageBytesByYear(storageBytesByYear).build();
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Storage metrics scan completed in {}ms: {} comics, {} total bytes", duration, perComicMetrics.size(),
                totalStorageBytes);
        return true;
    }

    /**
     * Calculate storage metrics for a specific comic directory
     *
     * @param comicDir Comic directory to analyze
     * @return Metrics for the comic
     */
    private ComicStorageMetrics calculateComicMetrics(File comicDir) {
        long totalSize = 0;
        int imageCount = 0;
        Map<String, Long> yearStorage = new HashMap<>();

        // Process each year directory
        File[] yearDirs = comicDir.listFiles(file -> file.isDirectory() && !"@eaDir".equals(file.getName()));
        if (yearDirs != null) {
            for (File yearDir : yearDirs) {
                long yearSize = 0;
                File[] images = yearDir.listFiles(
                        file -> file.isFile() && (file.getName().endsWith(".png") || file.getName().endsWith(".jpg")));

                if (images != null) {
                    for (File image : images) {
                        long fileSize = image.length();
                        yearSize += fileSize;
                        imageCount++;
                    }
                }

                yearStorage.put(yearDir.getName(), yearSize);
                totalSize += yearSize;
            }
        }

        return ComicStorageMetrics.builder().comicName(comicDir.getName()).storageBytes(totalSize)
                .imageCount(imageCount)
                .averageImageSize(imageCount > 0 ? (double) totalSize / imageCount : 0).storageByYear(yearStorage)
                .build();
    }

    /**
     * Build a proper image path including comic name, year, and filename.
     *
     * @param comicName The comic directory name
     * @param year      The year directory name
     * @param imageName The image filename (or null if no images)
     * @return Full path string, or empty string if imageName is null
     */
    private String buildImagePath(String comicName, String year, String imageName) {
        if (imageName == null) {
            return "";
        }
        return String.format("%s/%s/%s/%s", cacheDirectory, comicName, year, imageName);
    }

    /**
     * Find the first image in the folder
     *
     * @param location Directory to look into
     * @return First item in the directory when sorted by filename
     */
    private String firstImage(String location) {
        String[] cachedStrips = images(location);
        if (cachedStrips.length < 1) {
            return null;
        }
        return cachedStrips[0];
    }

    /**
     * Find the last image in the folder
     *
     * @param location Directory to look into
     * @return Last item in the directory when sorted by filename
     */
    private String lastImage(String location) {
        String[] cachedStrips = images(location);
        if (cachedStrips.length < 1) {
            return null;
        }
        return cachedStrips[cachedStrips.length - 1];
    }

    /**
     * Get the list of Images in the selected folder
     *
     * @param location Path to search
     * @return List of folders or null if none were found
     */
    private String[] images(String location) {
        var folder = new File(location);
        var cachedStrips = folder.list((dir, name) -> new File(dir, name).isFile()
                && (name.endsWith(".png") || name.endsWith(".jpg")) && !"@eaDir".equals(name));

        if (cachedStrips == null || cachedStrips.length == 0) {
            return new String[]{};
        }
        Arrays.sort(cachedStrips, String::compareTo);
        return cachedStrips;
    }
}
