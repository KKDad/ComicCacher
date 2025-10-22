package org.stapledon.infrastructure.caching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.metrics.repository.AccessMetricsRepository;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

/**
 * Tests for the access tracking features of CacheUtils
 */
class AccessMetricsCollectorAccessTrackingTest {

    @TempDir
    Path tempDir;

    private CacheUtils cacheUtils;
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
        cacheUtils = new CacheUtils(cacheRoot.getAbsolutePath(), mockStorageFacade, mockAccessMetricsRepository);

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
        cacheUtils.findOldest(testComic1);
        
        // Assert
        Map<String, Integer> accessCounts = cacheUtils.getAccessCounts();
        
        // Verify access was tracked
        assertTrue(accessCounts.containsKey("DilbertTest"));
        assertEquals(1, accessCounts.get("DilbertTest").intValue());
        
        // Verify last access time was recorded
        Map<String, String> lastAccessTimes = cacheUtils.getLastAccessTimes();
        assertTrue(lastAccessTimes.containsKey("DilbertTest"));
        assertNotNull(lastAccessTimes.get("DilbertTest"));
    }
    
    @Test
    void findNewest_shouldTrackAccess() {
        // Act - find newest comic for Calvin
        cacheUtils.findNewest(testComic2);
        
        // Assert
        Map<String, Integer> accessCounts = cacheUtils.getAccessCounts();
        
        // Verify access was tracked
        assertTrue(accessCounts.containsKey("CalvinTest"));
        assertEquals(1, accessCounts.get("CalvinTest").intValue());
    }
    
    @Test
    void multipleFindCalls_shouldIncrementAccessCount() {
        // Act - multiple find calls for same comic
        cacheUtils.findOldest(testComic1);
        cacheUtils.findNewest(testComic1);
        cacheUtils.findOldest(testComic1);
        
        // Assert
        Map<String, Integer> accessCounts = cacheUtils.getAccessCounts();
        
        // Verify access count is correct
        assertEquals(3, accessCounts.get("DilbertTest").intValue());
    }
    
    @Test
    void findNext_shouldTrackAccess() {
        // Act - find next comic
        LocalDate firstDate = LocalDate.of(2023, 1, 1);
        cacheUtils.findNext(testComic1, firstDate);

        // Assert
        Map<String, Integer> accessCounts = cacheUtils.getAccessCounts();

        // Verify access was tracked
        assertTrue(accessCounts.containsKey("DilbertTest"));
        assertEquals(1, accessCounts.get("DilbertTest").intValue());

        // Verify hit ratio tracking
        Map<String, Double> hitRatios = cacheUtils.getHitRatios();
        assertTrue(hitRatios.containsKey("DilbertTest"));
        // Access is a hit
        assertEquals(1.0, hitRatios.get("DilbertTest"), 0.001);
    }

    @Test
    void findPrevious_shouldTrackAccess() {
        // Act - find previous comic
        LocalDate lastDate = LocalDate.of(2022, 6, 10);
        cacheUtils.findPrevious(testComic2, lastDate);

        // Assert
        Map<String, Integer> accessCounts = cacheUtils.getAccessCounts();

        // Verify access was tracked
        assertTrue(accessCounts.containsKey("CalvinTest"));
        assertEquals(1, accessCounts.get("CalvinTest").intValue());
    }

    @Test
    void unsuccessfulFind_shouldTrackAsMiss() {
        // Configure mock to return empty for a specific date
        LocalDate beyondAvailable = LocalDate.of(2023, 12, 31);
        
        // Act - find next comic with date beyond what's available
        cacheUtils.findNext(testComic1, beyondAvailable);

        // Assert
        Map<String, Double> hitRatios = cacheUtils.getHitRatios();

        // Verify it was tracked correctly as a miss (0.0 ratio)
        assertTrue(hitRatios.containsKey("DilbertTest"));
        assertEquals(0.0, hitRatios.get("DilbertTest"), 0.001);
    }
    
    @Test
    void accessTracking_shouldMaintainSeparateStatsPerComic() {
        // Act - find for both comics
        cacheUtils.findOldest(testComic1);
        cacheUtils.findNewest(testComic1);
        
        cacheUtils.findOldest(testComic2);
        
        // Assert
        Map<String, Integer> accessCounts = cacheUtils.getAccessCounts();
        
        // Verify each comic has separate counters
        assertEquals(2, accessCounts.get("DilbertTest").intValue());
        assertEquals(1, accessCounts.get("CalvinTest").intValue());
    }
    
    @Test
    void averageAccessTimes_shouldBeTracked() {
        // Act
        cacheUtils.findOldest(testComic1);
        cacheUtils.findNewest(testComic1);
        
        // Assert
        Map<String, Double> avgTimes = cacheUtils.getAverageAccessTimes();
        
        // Verify average time is tracked
        assertTrue(avgTimes.containsKey("DilbertTest"));
        assertTrue(avgTimes.get("DilbertTest") >= 0.0);
    }
}