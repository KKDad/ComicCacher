package org.stapledon.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stapledon.common.dto.ComicStorageMetrics;
import org.stapledon.common.dto.ImageCacheStats;
import org.stapledon.metrics.collector.StorageMetricsCollector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StorageMetricsCollectorTest {

    @TempDir
    Path tempDir;

    private StorageMetricsCollector cacheStatsUpdater;
    private File cacheRoot;

    @BeforeEach
    void setUp() throws IOException {
        // Create cache directory structure
        cacheRoot = tempDir.toFile();

        // Create comic directory 1 with years and images
        File comicDir1 = new File(cacheRoot, "CalvinAndHobbes");
        comicDir1.mkdir();

        File year2010 = new File(comicDir1, "2010");
        year2010.mkdir();
        createDummyImage(year2010, "2010-01-01.png", 1024 * 10);
        createDummyImage(year2010, "2010-01-02.png", 1024 * 15);

        File year2011 = new File(comicDir1, "2011");
        year2011.mkdir();
        createDummyImage(year2011, "2011-06-15.png", 1024 * 12);

        // Create comic directory 2 with years and images
        File comicDir2 = new File(cacheRoot, "Garfield");
        comicDir2.mkdir();

        File year2020 = new File(comicDir2, "2020");
        year2020.mkdir();
        createDummyImage(year2020, "2020-01-01.png", 1024 * 20);
        createDummyImage(year2020, "2020-12-31.png", 1024 * 25);

        // Initialize the cache stats updater (no longer requires StatsWriter)
        cacheStatsUpdater = new StorageMetricsCollector(cacheRoot.getAbsolutePath());
    }

    private void createDummyImage(File directory, String fileName, int size) throws IOException {
        File imageFile = new File(directory, fileName);
        byte[] data = new byte[size];
        Arrays.fill(data, (byte) 1);
        Files.write(imageFile.toPath(), data);
    }

    @Test
    void cacheStats_whenCacheIsEmpty_returnsEmptyStats() {
        // Arrange - Clear the cache root
        deleteDirectory(cacheRoot);
        cacheRoot.mkdir();

        // Act
        ImageCacheStats stats = cacheStatsUpdater.cacheStats();

        // Assert
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalStorageBytes()).isEqualTo(0);
        assertThat(stats.getPerComicMetrics() == null || stats.getPerComicMetrics().isEmpty()).isTrue();
    }

    @Test
    void updateStats_calculatesCorrectTotalStorageBytes() {
        // Act
        boolean result = cacheStatsUpdater.updateStats();

        // Assert
        assertThat(result).isTrue();

        ImageCacheStats stats = cacheStatsUpdater.cacheStats();

        // Expected total: 10KB + 15KB + 12KB + 20KB + 25KB = 82KB
        assertThat(stats.getTotalStorageBytes()).isEqualTo(82 * 1024);
    }

    @Test
    void updateStats_createsPerComicMetrics() {
        // Act
        boolean result = cacheStatsUpdater.updateStats();

        // Assert
        assertThat(result).isTrue();

        ImageCacheStats stats = cacheStatsUpdater.cacheStats();
        Map<String, ComicStorageMetrics> perComicMetrics = stats.getPerComicMetrics();

        assertThat(perComicMetrics).isNotNull();
        assertThat(perComicMetrics.size()).isEqualTo(2);

        // Check Calvin and Hobbes metrics
        ComicStorageMetrics calvin = perComicMetrics.get("CalvinAndHobbes");
        assertThat(calvin).isNotNull();
        assertThat(calvin.getComicName()).isEqualTo("CalvinAndHobbes");
        assertThat(calvin.getImageCount()).isEqualTo(3);
        assertThat(calvin.getStorageBytes()).isEqualTo((10 + 15 + 12) * 1024);
        assertThat(calvin.getAverageImageSize()).isCloseTo((10 + 15 + 12) * 1024 / 3.0, within(0.1));

        // Check Garfield metrics
        ComicStorageMetrics garfield = perComicMetrics.get("Garfield");
        assertThat(garfield).isNotNull();
        assertThat(garfield.getComicName()).isEqualTo("Garfield");
        assertThat(garfield.getImageCount()).isEqualTo(2);
        assertThat(garfield.getStorageBytes()).isEqualTo((20 + 25) * 1024);
        assertThat(garfield.getAverageImageSize()).isCloseTo((20 + 25) * 1024 / 2.0, within(0.1));
    }

    @Test
    void updateStats_createsStorageByYearMetrics() {
        // Act
        boolean result = cacheStatsUpdater.updateStats();

        // Assert
        assertThat(result).isTrue();

        ImageCacheStats stats = cacheStatsUpdater.cacheStats();
        Map<String, ComicStorageMetrics> perComicMetrics = stats.getPerComicMetrics();

        // Check Calvin and Hobbes yearly breakdown
        ComicStorageMetrics calvin = perComicMetrics.get("CalvinAndHobbes");
        Map<String, Long> calvinYearStorage = calvin.getStorageByYear();
        assertThat(calvinYearStorage).isNotNull();
        assertThat(calvinYearStorage.size()).isEqualTo(2);
        assertThat(calvinYearStorage.get("2010")).isEqualTo((10 + 15) * 1024);
        assertThat(calvinYearStorage.get("2011")).isEqualTo(12 * 1024);

        // Check Garfield yearly breakdown
        ComicStorageMetrics garfield = perComicMetrics.get("Garfield");
        Map<String, Long> garfieldYearStorage = garfield.getStorageByYear();
        assertThat(garfieldYearStorage).isNotNull();
        assertThat(garfieldYearStorage.size()).isEqualTo(1);
        assertThat(garfieldYearStorage.get("2020")).isEqualTo((20 + 25) * 1024);
    }

    @Test
    void updateStats_populatesOldestAndNewestImages() {
        // Act
        boolean result = cacheStatsUpdater.updateStats();

        // Assert
        assertThat(result).isTrue();

        ImageCacheStats stats = cacheStatsUpdater.cacheStats();

        // Should contain the comic name in the path (bug fix verification)
        assertThat(stats.getOldestImage()).contains("CalvinAndHobbes");
        assertThat(stats.getOldestImage()).contains("2010");
        assertThat(stats.getOldestImage()).contains("2010-01-01.png");

        assertThat(stats.getNewestImage()).contains("CalvinAndHobbes");
        assertThat(stats.getNewestImage()).contains("2011");
        assertThat(stats.getNewestImage()).contains("2011-06-15.png");
    }

    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }
}