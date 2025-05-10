package org.stapledon.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.caching.ImageCacheStatsUpdater;
import org.stapledon.dto.ComicStorageMetrics;
import org.stapledon.dto.ImageCacheStats;
import org.stapledon.utils.CacheUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the MetricsController
 */
class MetricsControllerTest {

    @Mock
    private ImageCacheStatsUpdater mockCacheStatsUpdater;

    @Mock
    private CacheUtils mockCacheUtils;

    @InjectMocks
    private MetricsController metricsController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getStorageMetrics_shouldReturnCacheStats() {
        // Arrange
        ImageCacheStats mockStats = createMockImageCacheStats();
        when(mockCacheStatsUpdater.cacheStats()).thenReturn(mockStats);

        // Act
        ResponseEntity<ApiResponse<ImageCacheStats>> response = metricsController.getStorageMetrics();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals(mockStats, response.getBody().getData());

        // Verify CacheStatsUpdater was called
        verify(mockCacheStatsUpdater).cacheStats();
    }

    @Test
    void getAccessMetrics_shouldReturnAccessMetrics() {
        // Arrange
        Map<String, Integer> mockAccessCounts = new HashMap<>();
        mockAccessCounts.put("Comic1", 10);
        mockAccessCounts.put("Comic2", 5);

        Map<String, String> mockLastAccessTimes = new HashMap<>();
        mockLastAccessTimes.put("Comic1", "2023-05-01T10:15:30");
        mockLastAccessTimes.put("Comic2", "2023-05-02T11:20:45");

        Map<String, Double> mockAvgAccessTimes = new HashMap<>();
        mockAvgAccessTimes.put("Comic1", 15.5);
        mockAvgAccessTimes.put("Comic2", 8.2);

        Map<String, Double> mockHitRatios = new HashMap<>();
        mockHitRatios.put("Comic1", 0.8);
        mockHitRatios.put("Comic2", 0.9);

        when(mockCacheUtils.getAccessCounts()).thenReturn(mockAccessCounts);
        when(mockCacheUtils.getLastAccessTimes()).thenReturn(mockLastAccessTimes);
        when(mockCacheUtils.getAverageAccessTimes()).thenReturn(mockAvgAccessTimes);
        when(mockCacheUtils.getHitRatios()).thenReturn(mockHitRatios);

        // Act
        ResponseEntity<ApiResponse<Map<String, AccessMetricsDto>>> response = metricsController.getAccessMetrics();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());

        // Verify the metrics for Comic1
        Map<String, AccessMetricsDto> accessMetrics = response.getBody().getData();
        assertTrue(accessMetrics.containsKey("Comic1"));

        // Verify CacheUtils methods were called
        verify(mockCacheUtils).getAccessCounts();
        verify(mockCacheUtils).getLastAccessTimes();
        verify(mockCacheUtils).getAverageAccessTimes();
        verify(mockCacheUtils).getHitRatios();
    }

    @SuppressWarnings("unchecked")
    @Test
    void getCombinedMetrics_shouldCombineStorageAndAccessMetrics() {
        // Arrange
        // Setup storage metrics
        ImageCacheStats mockStats = createMockImageCacheStats();
        when(mockCacheStatsUpdater.cacheStats()).thenReturn(mockStats);

        // Setup access metrics (simplified to avoid duplication)
        Map<String, Integer> mockAccessCounts = new HashMap<>();
        mockAccessCounts.put("Comic1", 10);
        mockAccessCounts.put("Comic2", 5);
        mockAccessCounts.put("Comic3", 3); // Comic not in storage metrics

        when(mockCacheUtils.getAccessCounts()).thenReturn(mockAccessCounts);
        when(mockCacheUtils.getLastAccessTimes()).thenReturn(new HashMap<>());
        when(mockCacheUtils.getAverageAccessTimes()).thenReturn(new HashMap<>());
        when(mockCacheUtils.getHitRatios()).thenReturn(new HashMap<>());

        // Create a mock response that would be returned by getAccessMetrics
        ApiResponse<Map<String, AccessMetricsDto>> mockApiResponse = new ApiResponse<>();
        Map<String, AccessMetricsDto> accessMetricsMap = new HashMap<>();
        accessMetricsMap.put("Comic1", AccessMetricsDto.builder().comicName("Comic1").build());
        accessMetricsMap.put("Comic2", AccessMetricsDto.builder().comicName("Comic2").build());
        accessMetricsMap.put("Comic3", AccessMetricsDto.builder().comicName("Comic3").build());
        mockApiResponse.setData(accessMetricsMap);
        ResponseEntity<ApiResponse<Map<String, AccessMetricsDto>>> mockResponse =
                ResponseEntity.ok(mockApiResponse);

        // Use spy to return a predetermined response from getAccessMetrics
        MetricsController spyController = spy(metricsController);
        doReturn(mockResponse).when(spyController).getAccessMetrics();

        // Act
        ResponseEntity<ApiResponse<Map<String, CombinedMetricsDto>>> response =
                spyController.getCombinedMetrics();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());

        // Verify that storage metrics and access metrics were fetched
        verify(mockCacheStatsUpdater).cacheStats();
        verify(spyController).getAccessMetrics();
    }

    @Test
    void refreshStorageMetrics_shouldUpdateStatsBeforeReturning() {
        // Arrange
        ImageCacheStats mockStats = createMockImageCacheStats();
        when(mockCacheStatsUpdater.cacheStats()).thenReturn(mockStats);
        when(mockCacheStatsUpdater.updateStats()).thenReturn(true);

        // Act
        ResponseEntity<ApiResponse<ImageCacheStats>> response = metricsController.refreshStorageMetrics();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify update was called first, then stats were fetched
        verify(mockCacheStatsUpdater).updateStats();
        verify(mockCacheStatsUpdater).cacheStats();
    }

    /**
     * Creates a mock ImageCacheStats object with test data
     */
    private ImageCacheStats createMockImageCacheStats() {
        Map<String, ComicStorageMetrics> perComicMetrics = new HashMap<>();

        // Add Comic1 metrics
        ComicStorageMetrics comic1Metrics = ComicStorageMetrics.builder()
                .comicName("Comic1")
                .storageBytes(1024 * 1024 * 10)
                .imageCount(100)
                .averageImageSize(102400)
                .build();
        perComicMetrics.put("Comic1", comic1Metrics);

        // Add Comic2 metrics
        ComicStorageMetrics comic2Metrics = ComicStorageMetrics.builder()
                .comicName("Comic2")
                .storageBytes(1024 * 1024 * 5)
                .imageCount(50)
                .averageImageSize(102400)
                .build();
        perComicMetrics.put("Comic2", comic2Metrics);

        return ImageCacheStats.builder()
                .oldestImage("/path/to/oldest.png")
                .newestImage("/path/to/newest.png")
                .years(List.of("2022", "2023"))
                .totalStorageBytes(1024 * 1024 * 15)
                .perComicMetrics(perComicMetrics)
                .build();
    }
}