package org.stapledon.metrics.collector;

import org.springframework.beans.factory.annotation.Qualifier;
import org.stapledon.api.dto.comic.ComicStorageMetrics;
import org.stapledon.api.dto.comic.ImageCacheStats;
import org.stapledon.infrastructure.config.JsonConfigWriter;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Collector for storage metrics.
 * Scans the cache directory and computes storage utilization statistics.
 * Configured as a bean in MetricsConfiguration when metrics are enabled.
 */
@Slf4j
public class StorageMetricsCollector {
    private final JsonConfigWriter statsUpdater;
    private final String cacheDirectory;

    private ImageCacheStats cacheStats;

    public StorageMetricsCollector(@Qualifier("cacheLocation") String targetDirectory, JsonConfigWriter statsUpdater) {
        this.cacheDirectory = targetDirectory;
        this.statsUpdater = statsUpdater;
    }

    public ImageCacheStats cacheStats() {
        if (cacheStats == null)
            updateStats();
        return cacheStats;
    }

    /**
     * Generate Statistics about the Images cached in a particular directory to speed up retrieval at a later date.
     *
     * @return True if successful
     */
    public boolean updateStats() {
        var root = new File(cacheDirectory);
        if (!root.exists()) {
            log.error("{} doesn't exist", cacheDirectory);
            return false;
        }

        // Initialize with empty metrics
        cacheStats = new ImageCacheStats();

        // Get all comic directories (one level down from root)
        File[] comicDirs = root.listFiles(file -> file.isDirectory() && !file.getName().equals("@eaDir"));
        if (comicDirs == null || comicDirs.length == 0) {
            log.warn("No comic directories found in {}", cacheDirectory);
            return statsUpdater.save(cacheStats, cacheDirectory);
        }

        // Process each comic directory to gather metrics
        Map<String, ComicStorageMetrics> perComicMetrics = new HashMap<>();
        long totalStorageBytes = 0;

        for (File comicDir : comicDirs) {
            ComicStorageMetrics metrics = calculateComicMetrics(comicDir);
            perComicMetrics.put(comicDir.getName(), metrics);
            totalStorageBytes += metrics.getStorageBytes();
        }

        // Images are stored by year, gather a list of all years that we have stored (from the first comic)
        String[] yearFolders = comicDirs[0].list((dir, name) -> new File(dir, name).isDirectory() && !name.equals("@eaDir"));
        if (yearFolders != null && yearFolders.length > 0) {
            var years = Arrays.asList(yearFolders);
            Arrays.sort(yearFolders, Comparator.comparing(Integer::valueOf));

            cacheStats = ImageCacheStats.builder()
                    .years(years)
                    // Find the first image in the first year. This is the oldest image available
                    .oldestImage(expand(yearFolders[0], firstImage(String.format("%s/%s", comicDirs[0].getAbsolutePath(), yearFolders[0]))))
                    // Find the last image in the last year. This is the newest image available
                    .newestImage(expand(yearFolders[yearFolders.length - 1], lastImage(String.format("%s/%s", comicDirs[0].getAbsolutePath(), yearFolders[yearFolders.length - 1]))))
                    .totalStorageBytes(totalStorageBytes)
                    .perComicMetrics(perComicMetrics)
                    .build();
        }

        return statsUpdater.save(cacheStats, cacheDirectory);
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
        File[] yearDirs = comicDir.listFiles(file -> file.isDirectory() && !file.getName().equals("@eaDir"));
        if (yearDirs != null) {
            for (File yearDir : yearDirs) {
                long yearSize = 0;
                File[] images = yearDir.listFiles(file ->
                    file.isFile() && (file.getName().endsWith(".png") || file.getName().endsWith(".jpg")));

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

        return ComicStorageMetrics.builder()
                .comicName(comicDir.getName())
                .storageBytes(totalSize)
                .imageCount(imageCount)
                .averageImageSize(imageCount > 0 ? (double)totalSize / imageCount : 0)
                .storageByYear(yearStorage)
                .build();
    }

    private String expand(String path, String image) {
        // image will be null if there are no matching images in the directory (eg: Caching failed).
        if (image == null)
            return "";
        return Paths.get(cacheDirectory, path, image).toFile().getAbsolutePath();
    }

    /**
     * Find the first image in the folder
     *
     * @param location Directory to look into
     * @return First item in the directory when sorted by filename
     */
    private String firstImage(String location) {
        // Find the first image in the folder
        String[] cachedStrips = images(location);
        if (cachedStrips.length < 1)
            return null;
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
        if (cachedStrips.length < 1)
            return null;
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
        var cachedStrips = folder.list((dir, name) ->
            new File(dir, name).isFile() &&
            (name.endsWith(".png") || name.endsWith(".jpg")) &&
            !name.equals("@eaDir"));

        if (cachedStrips == null || cachedStrips.length == 0)
            return new String[]{};
        Arrays.sort(cachedStrips, String::compareTo);
        return cachedStrips;
    }
}
