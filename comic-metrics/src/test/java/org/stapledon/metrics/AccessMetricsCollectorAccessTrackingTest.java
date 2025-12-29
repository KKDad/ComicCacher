package org.stapledon.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.metrics.collector.AccessMetricsCollector;
import org.stapledon.metrics.repository.AccessMetricsRepository;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;

/**
 * Tests for the access tracking features of CacheUtils
 */
class AccessMetricsCollectorAccessTrackingTest {

    @TempDir
    Path tempDir;

    private AccessMetricsCollector accessMetricsCollector;
    private ComicItem testComic1;
    private ComicItem testComic2;

    @BeforeEach
    void setUp() {
        // Create cache directory structure
        File cacheRoot = tempDir.toFile();

        // Set up mock facade
        MockComicStorageFacade mockStorageFacade = new MockComicStorageFacade();
        AccessMetricsRepository mockAccessMetricsRepository = mock(AccessMetricsRepository.class);

        // Initialize CacheUtils with temp directory, mock facade, and mock metrics repository
        accessMetricsCollector = new AccessMetricsCollector(cacheRoot.getAbsolutePath(), mockStorageFacade, mockAccessMetricsRepository);

        // Create comic items for testing
        testComic1 = ComicItem.builder()
                .id(1)
                .name("DilbertTest")
                .newest(LocalDate.of(2023, 1, 5))
                .oldest(LocalDate.of(2023, 1, 1))
                .build();

        testComic2 = ComicItem.builder()
                .id(2)
                .name("CalvinTest")
                .newest(LocalDate.of(2022, 6, 10))
                .oldest(LocalDate.of(2022, 6, 1))
                .build();
                
        // Set up mock data
        mockStorageFacade.setupComic(
                testComic1.getId(),
                testComic1.getName(),
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 5),
                LocalDate.of(2023, 1, 2), 
                LocalDate.of(2023, 1, 3),
                LocalDate.of(2023, 1, 4)
        );
        
        mockStorageFacade.setupComic(
                testComic2.getId(),
                testComic2.getName(),
                LocalDate.of(2022, 6, 1),
                LocalDate.of(2022, 6, 10),
                LocalDate.of(2022, 6, 5)
        );
    }

    @Test
    void findOldest_shouldTrackAccess() {
        // Act - find oldest comic for Dilbert
        accessMetricsCollector.findOldest(testComic1);

        // Assert
        Map<String, Integer> accessCounts = accessMetricsCollector.getAccessCounts();

        // Verify access was tracked
        assertThat(accessCounts.containsKey("DilbertTest")).isTrue();
        assertThat(accessCounts.get("DilbertTest").intValue()).isEqualTo(1);

        // Verify last access time was recorded
        Map<String, String> lastAccessTimes = accessMetricsCollector.getLastAccessTimes();
        assertThat(lastAccessTimes.containsKey("DilbertTest")).isTrue();
        assertThat(lastAccessTimes.get("DilbertTest")).isNotNull();
    }

    @Test
    void findNewest_shouldTrackAccess() {
        // Act - find newest comic for Calvin
        accessMetricsCollector.findNewest(testComic2);

        // Assert
        Map<String, Integer> accessCounts = accessMetricsCollector.getAccessCounts();

        // Verify access was tracked
        assertThat(accessCounts.containsKey("CalvinTest")).isTrue();
        assertThat(accessCounts.get("CalvinTest").intValue()).isEqualTo(1);
    }
    
    @Test
    void multipleFindCalls_shouldIncrementAccessCount() {
        // Act - multiple find calls for same comic
        accessMetricsCollector.findOldest(testComic1);
        accessMetricsCollector.findNewest(testComic1);
        accessMetricsCollector.findOldest(testComic1);

        // Assert
        Map<String, Integer> accessCounts = accessMetricsCollector.getAccessCounts();

        // Verify access count is correct
        assertThat(accessCounts.get("DilbertTest").intValue()).isEqualTo(3);
    }
    
    @Test
    void findNext_shouldTrackAccess() {
        // Act - find next comic
        LocalDate firstDate = LocalDate.of(2023, 1, 1);
        accessMetricsCollector.findNext(testComic1, firstDate);

        // Assert
        Map<String, Integer> accessCounts = accessMetricsCollector.getAccessCounts();

        // Verify access was tracked
        assertThat(accessCounts.containsKey("DilbertTest")).isTrue();
        assertThat(accessCounts.get("DilbertTest").intValue()).isEqualTo(1);

        // Verify hit ratio tracking
        Map<String, Double> hitRatios = accessMetricsCollector.getHitRatios();
        assertThat(hitRatios.containsKey("DilbertTest")).isTrue();
        // Access is a hit
        assertThat(hitRatios.get("DilbertTest")).isCloseTo(1.0, within(0.001));
    }

    @Test
    void findPrevious_shouldTrackAccess() {
        // Act - find previous comic
        LocalDate lastDate = LocalDate.of(2022, 6, 10);
        accessMetricsCollector.findPrevious(testComic2, lastDate);

        // Assert
        Map<String, Integer> accessCounts = accessMetricsCollector.getAccessCounts();

        // Verify access was tracked
        assertThat(accessCounts.containsKey("CalvinTest")).isTrue();
        assertThat(accessCounts.get("CalvinTest").intValue()).isEqualTo(1);
    }

    @Test
    void unsuccessfulFind_shouldTrackAsMiss() {
        // Configure mock to return empty for a specific date
        LocalDate beyondAvailable = LocalDate.of(2023, 12, 31);

        // Act - find next comic with date beyond what's available
        accessMetricsCollector.findNext(testComic1, beyondAvailable);

        // Assert
        Map<String, Double> hitRatios = accessMetricsCollector.getHitRatios();

        // Verify it was tracked correctly as a miss (0.0 ratio)
        assertThat(hitRatios.containsKey("DilbertTest")).isTrue();
        assertThat(hitRatios.get("DilbertTest")).isCloseTo(0.0, within(0.001));
    }
    
    @Test
    void accessTracking_shouldMaintainSeparateStatsPerComic() {
        // Act - find for both comics
        accessMetricsCollector.findOldest(testComic1);
        accessMetricsCollector.findNewest(testComic1);

        accessMetricsCollector.findOldest(testComic2);

        // Assert
        Map<String, Integer> accessCounts = accessMetricsCollector.getAccessCounts();

        // Verify each comic has separate counters
        assertThat(accessCounts.get("DilbertTest").intValue()).isEqualTo(2);
        assertThat(accessCounts.get("CalvinTest").intValue()).isEqualTo(1);
    }
    
    @Test
    void averageAccessTimes_shouldBeTracked() {
        // Act
        accessMetricsCollector.findOldest(testComic1);
        accessMetricsCollector.findNewest(testComic1);

        // Assert
        Map<String, Double> avgTimes = accessMetricsCollector.getAverageAccessTimes();

        // Verify average time is tracked
        assertThat(avgTimes.containsKey("DilbertTest")).isTrue();
        assertThat(avgTimes.get("DilbertTest") >= 0.0).isTrue();
    }
}