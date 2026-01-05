package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.common.dto.ComicStorageMetrics;
import org.stapledon.common.dto.ImageCacheStats;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.metrics.service.MetricsService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the MetricsController
 * Updated to use MetricsService facade
 */
class MetricsControllerTest {

    @Mock
    private MetricsService mockMetricsService;

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
        when(mockMetricsService.getStorageMetrics()).thenReturn(mockStats);

        // Act
        ResponseEntity<ApiResponse<ImageCacheStats>> response = metricsController.getStorageMetrics();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(mockStats);

        // Verify MetricsService was called
        verify(mockMetricsService).getStorageMetrics();
    }

    @Test
    void getAccessMetrics_shouldReturnAccessMetrics() {
        // Arrange
        AccessMetricsData mockAccessData = createMockAccessMetricsData();
        when(mockMetricsService.getAccessMetrics()).thenReturn(mockAccessData);

        // Act
        ResponseEntity<ApiResponse<AccessMetricsData>> response = metricsController.getAccessMetrics();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(mockAccessData);

        // Verify service was called
        verify(mockMetricsService).getAccessMetrics();
    }

    @Test
    void getCombinedMetrics_shouldReturnCombinedMetrics() {
        // Arrange
        CombinedMetricsData mockCombinedData = createMockCombinedMetricsData();
        when(mockMetricsService.getCombinedMetrics()).thenReturn(mockCombinedData);

        // Act
        ResponseEntity<ApiResponse<CombinedMetricsData>> response = metricsController.getCombinedMetrics();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(mockCombinedData);

        // Verify service was called
        verify(mockMetricsService).getCombinedMetrics();
    }

    @Test
    void refreshStorageMetrics_shouldUpdateStatsBeforeReturning() {
        // Arrange
        ImageCacheStats mockStats = createMockImageCacheStats();
        when(mockMetricsService.refreshStorageMetrics()).thenReturn(mockStats);

        // Act
        ResponseEntity<ApiResponse<ImageCacheStats>> response = metricsController.refreshStorageMetrics();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        // Verify refresh was called
        verify(mockMetricsService).refreshStorageMetrics();
    }

    @Test
    void refreshAllMetrics_shouldTriggerForceRefresh() {
        // Act
        ResponseEntity<ApiResponse<String>> response = metricsController.refreshAllMetrics();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo("All metrics refreshed successfully");

        // Verify service method was called
        verify(mockMetricsService).refreshAllMetrics();
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

    /**
     * Creates a mock AccessMetricsData object with test data
     */
    private AccessMetricsData createMockAccessMetricsData() {
        Map<String, AccessMetricsData.ComicAccessMetrics> comicMetrics = new HashMap<>();

        // Add Comic1 access metrics
        AccessMetricsData.ComicAccessMetrics comic1Metrics = AccessMetricsData.ComicAccessMetrics.builder()
                .comicName("Comic1")
                .accessCount(10)
                .lastAccess("2023-05-01T10:15:30")
                .totalAccessTimeMs(155)
                .cacheHits(8)
                .cacheMisses(2)
                .build();
        comicMetrics.put("Comic1", comic1Metrics);

        // Add Comic2 access metrics
        AccessMetricsData.ComicAccessMetrics comic2Metrics = AccessMetricsData.ComicAccessMetrics.builder()
                .comicName("Comic2")
                .accessCount(5)
                .lastAccess("2023-05-02T11:20:45")
                .totalAccessTimeMs(41)
                .cacheHits(4)
                .cacheMisses(1)
                .build();
        comicMetrics.put("Comic2", comic2Metrics);

        return AccessMetricsData.builder()
                .comicMetrics(comicMetrics)
                .build();
    }

    /**
     * Creates a mock CombinedMetricsData object with test data
     */
    private CombinedMetricsData createMockCombinedMetricsData() {
        Map<String, CombinedMetricsData.ComicCombinedMetrics> comics = new HashMap<>();

        // Add Comic1 combined metrics
        CombinedMetricsData.ComicCombinedMetrics comic1Metrics = CombinedMetricsData.ComicCombinedMetrics.builder()
                .comicName("Comic1")
                .storageBytes(1024 * 1024 * 10)
                .imageCount(100)
                .averageImageSize(102400)
                .accessCount(10)
                .lastAccess("2023-05-01T10:15:30")
                .averageAccessTime(15.5)
                .hitRatio(0.8)
                .cacheHits(8)
                .cacheMisses(2)
                .build();
        comics.put("Comic1", comic1Metrics);

        // Add Comic2 combined metrics
        CombinedMetricsData.ComicCombinedMetrics comic2Metrics = CombinedMetricsData.ComicCombinedMetrics.builder()
                .comicName("Comic2")
                .storageBytes(1024 * 1024 * 5)
                .imageCount(50)
                .averageImageSize(102400)
                .accessCount(5)
                .lastAccess("2023-05-02T11:20:45")
                .averageAccessTime(8.2)
                .hitRatio(0.8)
                .cacheHits(4)
                .cacheMisses(1)
                .build();
        comics.put("Comic2", comic2Metrics);

        return CombinedMetricsData.builder()
                .perComicMetrics(comics)
                .build();
    }
}
