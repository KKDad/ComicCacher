package org.stapledon.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stapledon.api.dto.health.HealthStatus;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicStorageMetrics;
import org.stapledon.common.dto.ImageCacheStats;
import org.stapledon.infrastructure.config.BuildVersion;
import org.stapledon.metrics.collector.AccessMetricsCollector;
import org.stapledon.metrics.collector.StorageMetricsCollector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for the SystemHealthService
 */
class SystemHealthServiceTest {

    @Mock
    private BuildVersion mockBuildVersion;

    @Mock
    private StorageMetricsCollector mockCacheStatsUpdater;

    @Mock
    private AccessMetricsCollector mockAccessMetricsCollector;

    @Mock
    private CacheProperties mockCacheProperties;

    @InjectMocks
    private SystemHealthService healthService;

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        // Create a temporary directory for testing
        tempDir = Files.createTempDirectory("healthServiceTest");

        // Mock build properties
        when(mockBuildVersion.getBuildProperty("build.name")).thenReturn("ComicAPI");
        when(mockBuildVersion.getBuildProperty("build.artifact")).thenReturn("comic-api");
        when(mockBuildVersion.getBuildProperty("build.group")).thenReturn("org.stapledon");
        when(mockBuildVersion.getBuildProperty("build.version")).thenReturn("1.0.0");
        when(mockBuildVersion.getBuildProperty("build.time")).thenReturn("2023-05-01T10:15:30Z");

        // Mock cache properties
        when(mockCacheProperties.getLocation()).thenReturn(tempDir.toString());

        // Mock cache stats
        ImageCacheStats mockStats = createMockImageCacheStats();
        when(mockCacheStatsUpdater.cacheStats()).thenReturn(mockStats);

        // Mock cache utils
        Map<String, Integer> accessCounts = new HashMap<>();
        accessCounts.put("Comic1", 10);
        accessCounts.put("Comic2", 5);

        Map<String, String> lastAccessTimes = new HashMap<>();
        lastAccessTimes.put("Comic1", "2023-05-01T10:15:30");
        lastAccessTimes.put("Comic2", "2023-05-02T11:20:45");

        Map<String, Double> avgAccessTimes = new HashMap<>();
        avgAccessTimes.put("Comic1", 15.5);
        avgAccessTimes.put("Comic2", 8.2);

        Map<String, Double> hitRatios = new HashMap<>();
        hitRatios.put("Comic1", 0.8);
        hitRatios.put("Comic2", 0.9);

        when(mockAccessMetricsCollector.getAccessCounts()).thenReturn(accessCounts);
        when(mockAccessMetricsCollector.getLastAccessTimes()).thenReturn(lastAccessTimes);
        when(mockAccessMetricsCollector.getAverageAccessTimes()).thenReturn(avgAccessTimes);
        when(mockAccessMetricsCollector.getHitRatios()).thenReturn(hitRatios);
    }

    @Test
    void getHealthStatus_shouldReturnBasicHealthInfo() {
        // Act
        HealthStatus status = healthService.getHealthStatus();

        // Assert
        assertThat(status).isNotNull();
        assertThat(status.getStatus()).isEqualTo(HealthStatus.Status.UP);
        assertThat(status.getTimestamp()).isNotNull();
        assertThat(status.getBuildInfo()).isNotNull();
        assertThat(status.getBuildInfo().getName()).isEqualTo("ComicAPI");
        assertThat(status.getBuildInfo().getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void getDetailedHealthStatus_shouldReturnDetailedHealthInfo() {
        // Act
        HealthStatus status = healthService.getDetailedHealthStatus();

        // Assert
        assertThat(status).isNotNull();
        assertThat(status.getStatus()).isEqualTo(HealthStatus.Status.UP);
        assertThat(status.getTimestamp()).isNotNull();
        assertThat(status.getBuildInfo()).isNotNull();
        assertThat(status.getSystemResources()).isNotNull();
        assertThat(status.getCacheStatus()).isNotNull();
        assertThat(status.getComponents()).isNotNull();

        // Verify system resources
        assertThat(status.getSystemResources().getAvailableProcessors()).isEqualTo(Runtime.getRuntime().availableProcessors());

        // Verify cache status
        assertThat(status.getCacheStatus().getTotalComics()).isEqualTo(2);
        assertThat(status.getCacheStatus().getTotalImages()).isEqualTo(150);
        assertThat(status.getCacheStatus().getTotalStorageBytes()).isEqualTo(1024 * 1024 * 15);
        assertThat(status.getCacheStatus().getOldestImage()).isEqualTo("/path/to/oldest.png");
        assertThat(status.getCacheStatus().getNewestImage()).isEqualTo("/path/to/newest.png");
        assertThat(status.getCacheStatus().getCacheLocation()).isEqualTo(tempDir.toString());

        // Verify components
        assertThat(status.getComponents().get("cache")).isNotNull();
        assertThat(status.getComponents().get("cache").getStatus()).isEqualTo(HealthStatus.Status.UP);
    }

    @Test
    void getDetailedHealthStatus_whenCacheDirectoryNotAccessible_shouldReportCacheDegraded() throws IOException {
        // Arrange - delete temp directory to simulate inaccessible cache
        Files.delete(tempDir);

        // Act
        HealthStatus status = healthService.getDetailedHealthStatus();

        // Assert
        assertThat(status).isNotNull();
        assertThat(status.getStatus()).isEqualTo(HealthStatus.Status.DOWN);
        assertThat(status.getComponents().get("cache")).isNotNull();
        assertThat(status.getComponents().get("cache").getStatus()).isEqualTo(HealthStatus.Status.DOWN);
        assertThat(status.getComponents().get("cache").getMessage()).isEqualTo("Cache directory is not accessible or writable");
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