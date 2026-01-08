package org.stapledon.engine.storage;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicDateIndex;
import org.stapledon.common.util.GsonUtils;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ComicIndexServiceTest {

    @TempDir
    Path tempDir;

    private ComicIndexService indexService;
    private final Gson gson = GsonUtils.createGson();

    @Mock
    private CacheProperties cacheProperties;

    @Mock
    private ImageMetadataRepository metadataRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(cacheProperties.getLocation()).thenReturn(tempDir.toString());
        indexService = new ComicIndexService(gson, cacheProperties, metadataRepository);
    }

    @Test
    void getNextDate_shouldReturnNextDate() {
        // Arrange
        int comicId = 1;
        String comicName = "TestComic";
        LocalDate d1 = LocalDate.of(2023, 1, 1);
        LocalDate d2 = LocalDate.of(2023, 1, 5);
        LocalDate d3 = LocalDate.of(2023, 1, 10);

        setupIndex(comicId, comicName, Arrays.asList(d1, d2, d3));

        // Act
        Optional<LocalDate> next = indexService.getNextDate(comicId, comicName, d1);

        // Assert
        assertThat(next).hasValue(d2);
    }

    @Test
    void getNextDate_shouldHandleMissingDate() {
        // Arrange
        int comicId = 1;
        String comicName = "TestComic";
        LocalDate d1 = LocalDate.of(2023, 1, 1);
        LocalDate d3 = LocalDate.of(2023, 1, 10);

        setupIndex(comicId, comicName, Arrays.asList(d1, d3));

        // Act
        Optional<LocalDate> next = indexService.getNextDate(comicId, comicName, LocalDate.of(2023, 1, 5));

        // Assert
        assertThat(next).hasValue(d3);
    }

    @Test
    void getPreviousDate_shouldReturnPreviousDate() {
        // Arrange
        int comicId = 1;
        String comicName = "TestComic";
        LocalDate d1 = LocalDate.of(2023, 1, 1);
        LocalDate d2 = LocalDate.of(2023, 1, 5);
        LocalDate d3 = LocalDate.of(2023, 1, 10);

        setupIndex(comicId, comicName, Arrays.asList(d1, d2, d3));

        // Act
        Optional<LocalDate> prev = indexService.getPreviousDate(comicId, comicName, d3);

        // Assert
        assertThat(prev).hasValue(d2);
    }

    @Test
    void addDateToIndex_shouldUpdateAndSave() {
        // Arrange
        int comicId = 1;
        String comicName = "TestComic";
        LocalDate d1 = LocalDate.of(2023, 1, 1);
        LocalDate d2 = LocalDate.of(2023, 1, 10);

        setupIndex(comicId, comicName, Arrays.asList(d1));

        // Act
        indexService.addDateToIndex(comicId, comicName, d2);

        // Assert
        Optional<LocalDate> next = indexService.getNextDate(comicId, comicName, d1);
        assertThat(next).hasValue(d2);

        // Check file exists
        File indexFile = new File(tempDir.toFile(), "TestComic/available-dates.json");
        assertThat(indexFile).exists();
    }

    private void setupIndex(int comicId, String comicName, List<LocalDate> dates) {
        ComicDateIndex index = ComicDateIndex.builder()
                .comicId(comicId)
                .comicName(comicName)
                .availableDates(dates)
                .lastUpdated(LocalDate.now())
                .build();

        File comicDir = new File(tempDir.toFile(), comicName);
        comicDir.mkdirs();
        File indexFile = new File(comicDir, "available-dates.json");

        try (FileWriter writer = new FileWriter(indexFile)) {
            gson.toJson(index, writer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void addDateToIndex_shouldHandleConcurrentWrites() throws Exception {
        // Arrange
        int comicId = 1;
        String comicName = "TestComic";
        LocalDate baseDate = LocalDate.of(2023, 1, 1);

        setupIndex(comicId, comicName, Arrays.asList(baseDate));

        // Act - add 50 dates concurrently
        int numThreads = 10;
        int datesPerThread = 5;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(numThreads);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(numThreads);

        for (int t = 0; t < numThreads; t++) {
            final int threadNum = t;
            executor.submit(() -> {
                try {
                    for (int d = 0; d < datesPerThread; d++) {
                        LocalDate date = baseDate.plusDays(threadNum * datesPerThread + d + 1);
                        indexService.addDateToIndex(comicId, comicName, date);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        // Assert - all dates should be present (1 original + 50 added)
        Optional<LocalDate> oldest = indexService.getOldestDate(comicId, comicName);
        Optional<LocalDate> newest = indexService.getNewestDate(comicId, comicName);

        assertThat(oldest).hasValue(baseDate);
        assertThat(newest).hasValue(baseDate.plusDays(numThreads * datesPerThread));
    }

    @Test
    void getNextDate_shouldHandleEmptyComicName() {
        // Arrange
        int comicId = 99;
        String comicName = ""; // Empty name
        LocalDate d1 = LocalDate.of(2023, 1, 1);
        LocalDate d2 = LocalDate.of(2023, 1, 5);

        // Create directory with fallback name
        File comicDir = new File(tempDir.toFile(), "comic_99");
        comicDir.mkdirs();
        ComicDateIndex index = ComicDateIndex.builder()
                .comicId(comicId)
                .comicName(comicName)
                .availableDates(Arrays.asList(d1, d2))
                .lastUpdated(LocalDate.now())
                .build();
        File indexFile = new File(comicDir, "available-dates.json");
        try (FileWriter writer = new FileWriter(indexFile)) {
            gson.toJson(index, writer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Act
        Optional<LocalDate> next = indexService.getNextDate(comicId, comicName, d1);

        // Assert
        assertThat(next).hasValue(d2);
    }

    @Test
    void getNextDate_shouldReturnEmptyForNonExistentComic() {
        // Arrange - no setup, comic doesn't exist
        int comicId = 999;
        String comicName = "NonExistent";

        // Act
        Optional<LocalDate> result = indexService.getNextDate(comicId, comicName, LocalDate.now());

        // Assert - should return empty, not throw
        assertThat(result).isEmpty();
    }

    @Test
    void invalidateCache_shouldClearCachedIndex() {
        // Arrange
        int comicId = 1;
        String comicName = "TestComic";
        LocalDate d1 = LocalDate.of(2023, 1, 1);

        setupIndex(comicId, comicName, Arrays.asList(d1));

        // Load it into cache
        indexService.getNextDate(comicId, comicName, d1.minusDays(1));

        // Act - invalidate cache
        indexService.invalidateCache(comicId);

        // Now delete the file
        File indexFile = new File(tempDir.toFile(), comicName + "/available-dates.json");
        indexFile.delete();

        // Assert - should rebuild from empty directory
        Optional<LocalDate> result = indexService.getNextDate(comicId, comicName, d1.minusDays(1));
        assertThat(result).isEmpty();
    }
}
