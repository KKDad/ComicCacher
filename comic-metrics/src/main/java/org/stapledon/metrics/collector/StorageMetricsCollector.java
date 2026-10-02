package org.stapledon.metrics.collector;

import org.springframework.beans.factory.annotation.Qualifier;
import org.stapledon.common.config.CacheLayout;
import org.stapledon.common.dto.ComicStorageMetrics;
import org.stapledon.common.dto.ImageCacheStats;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Collector for storage metrics. Scans the cache directory and computes storage
 * utilization statistics. This collector only computes metrics in-memory;
 * they are combined on demand by MetricsUpdateService.
 *
 * <p>
 * The scan walks the tree once and reads each image's size once. On NFS every
 * stat is a round trip, so it filters by file name before touching attributes:
 * the metadata sidecars and indexes next to the strips are never stat'ed.
 */
@Slf4j
@ToString
public class StorageMetricsCollector {
    private static final Pattern YEAR_DIRECTORY = Pattern.compile("\\d{4}");

    private final String cacheDirectory;

    private volatile ImageCacheStats cacheStats;

    public StorageMetricsCollector(@Qualifier("cacheLocation") String targetDirectory) {
        this.cacheDirectory = targetDirectory;
    }

    /**
     * The latest scan, scanning first if there hasn't been one.
     */
    public ImageCacheStats cacheStats() {
        ImageCacheStats stats = cacheStats;
        if (stats != null) {
            return stats;
        }
        synchronized (this) {
            if (cacheStats == null) {
                updateStats();
            }
            return cacheStats;
        }
    }

    /**
     * The latest scan, or empty when none has finished yet. Never scans.
     */
    public Optional<ImageCacheStats> currentStats() {
        return Optional.ofNullable(cacheStats);
    }

    /**
     * Scan the cache and replace the stats. One scan runs at a time; a second caller waits for it and then scans again.
     *
     * @return True if successful
     */
    public synchronized boolean updateStats() {
        long startTime = System.currentTimeMillis();
        log.info("Starting storage metrics scan...");

        Path root = Paths.get(cacheDirectory);
        if (!Files.isDirectory(root)) {
            log.error("{} doesn't exist", cacheDirectory);
            return false;
        }

        Map<String, ComicStorageMetrics> perComicMetrics = new HashMap<>();
        Map<String, Integer> imageCountByYear = new HashMap<>();
        Map<String, Long> storageBytesByYear = new HashMap<>();
        long totalStorageBytes = 0;
        String oldestImage = null;
        String newestImage = null;

        try {
            for (Path comicDir : comicDirectories(root)) {
                ComicScan scan = scanComic(comicDir);
                String comicName = comicDir.getFileName().toString();
                perComicMetrics.put(comicName, scan.metrics(comicName));
                totalStorageBytes += scan.storageBytes;
                scan.storageByYear.forEach((year, bytes) -> storageBytesByYear.merge(year, bytes, Long::sum));
                scan.imageCountByYear.forEach((year, count) -> imageCountByYear.merge(year, count, Integer::sum));
                // Image names are yyyy-MM-dd, so the path from the comic directory down orders by date
                if (scan.oldest != null && (oldestImage == null || date(scan.oldest).compareTo(date(oldestImage)) < 0)) {
                    oldestImage = scan.oldest;
                }
                if (scan.newest != null && (newestImage == null || date(scan.newest).compareTo(date(newestImage)) > 0)) {
                    newestImage = scan.newest;
                }
            }
        } catch (IOException e) {
            log.error("Storage metrics scan of {} failed", cacheDirectory, e);
            return false;
        }

        if (perComicMetrics.isEmpty()) {
            log.warn("No comic directories found in {}", cacheDirectory);
        }

        cacheStats = ImageCacheStats.builder()
                .years(new ArrayList<>(new TreeSet<>(storageBytesByYear.keySet())))
                .oldestImage(oldestImage == null ? "" : oldestImage)
                .newestImage(newestImage == null ? "" : newestImage)
                .totalStorageBytes(totalStorageBytes).perComicMetrics(perComicMetrics)
                .imageCountByYear(imageCountByYear).storageBytesByYear(storageBytesByYear).build();

        long duration = System.currentTimeMillis() - startTime;
        log.info("Storage metrics scan completed in {}ms: {} comics, {} total bytes", duration, perComicMetrics.size(),
                totalStorageBytes);
        return true;
    }

    private List<Path> comicDirectories(Path root) throws IOException {
        List<Path> comicDirs = new ArrayList<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(root,
                entry -> CacheLayout.isComicDirectory(entry.getFileName().toString()) && Files.isDirectory(entry))) {
            entries.forEach(comicDirs::add);
        }
        return comicDirs;
    }

    /**
     * Sizes and counts the images in one comic's year directories.
     */
    private ComicScan scanComic(Path comicDir) throws IOException {
        ComicScan scan = new ComicScan();
        try (DirectoryStream<Path> years = Files.newDirectoryStream(comicDir,
                entry -> YEAR_DIRECTORY.matcher(entry.getFileName().toString()).matches() && Files.isDirectory(entry))) {
            for (Path yearDir : years) {
                String year = yearDir.getFileName().toString();
                long yearBytes = 0;
                int yearCount = 0;
                try (DirectoryStream<Path> images = Files.newDirectoryStream(yearDir, StorageMetricsCollector::isImageName)) {
                    for (Path image : images) {
                        yearBytes += Files.size(image);
                        yearCount++;
                        String path = image.toString();
                        if (scan.oldest == null || date(path).compareTo(date(scan.oldest)) < 0) {
                            scan.oldest = path;
                        }
                        if (scan.newest == null || date(path).compareTo(date(scan.newest)) > 0) {
                            scan.newest = path;
                        }
                    }
                }
                scan.storageByYear.put(year, yearBytes);
                scan.imageCountByYear.put(year, yearCount);
                scan.storageBytes += yearBytes;
                scan.imageCount += yearCount;
            }
        }
        return scan;
    }

    private static boolean isImageName(Path entry) {
        String name = entry.getFileName().toString();
        return name.endsWith(".png") || name.endsWith(".jpg");
    }

    /** The image's file name, which is its date (yyyy-MM-dd). */
    private static String date(String imagePath) {
        return Paths.get(imagePath).getFileName().toString();
    }

    /** What one comic's directory holds. */
    private static final class ComicScan {
        private final Map<String, Long> storageByYear = new TreeMap<>();
        private final Map<String, Integer> imageCountByYear = new TreeMap<>();
        private long storageBytes;
        private int imageCount;
        private String oldest;
        private String newest;

        private ComicStorageMetrics metrics(String comicName) {
            return ComicStorageMetrics.builder().comicName(comicName).storageBytes(storageBytes)
                    .imageCount(imageCount)
                    .averageImageSize(imageCount > 0 ? (double) storageBytes / imageCount : 0)
                    .storageByYear(storageByYear).imageCountByYear(imageCountByYear)
                    .build();
        }
    }
}
