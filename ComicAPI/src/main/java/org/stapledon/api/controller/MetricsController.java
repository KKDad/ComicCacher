package org.stapledon.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stapledon.api.dto.comic.ImageCacheStats;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.model.ResponseBuilder;
import org.stapledon.infrastructure.caching.CacheUtils;
import org.stapledon.infrastructure.caching.ImageCacheStatsUpdater;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;

/**
 * Controller for accessing cache metrics and statistics.
 * Provides endpoints for storage utilization and access patterns.
 */
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricsController {
    private final ImageCacheStatsUpdater cacheStatsUpdater;
    private final CacheUtils cacheUtils;

    /**
     * Get storage metrics for all comics.
     * Includes total bytes, per-comic metrics, and detailed breakdowns by year.
     *
     * @return Storage metrics for the cache
     */
    @GetMapping("/storage")
    public ResponseEntity<ApiResponse<ImageCacheStats>> getStorageMetrics() {
        ImageCacheStats stats = cacheStatsUpdater.cacheStats();
        return ResponseBuilder.ok(stats);
    }

    /**
     * Get access metrics for all comics.
     * Includes access counts, last access times, average access times, and hit ratios.
     *
     * @return Access metrics for all comics
     */
    @GetMapping("/access")
    public ResponseEntity<ApiResponse<Map<String, AccessMetricsDto>>> getAccessMetrics() {
        Map<String, Integer> accessCounts = cacheUtils.getAccessCounts();
        Map<String, String> lastAccessTimes = cacheUtils.getLastAccessTimes();
        Map<String, Double> avgAccessTimes = cacheUtils.getAverageAccessTimes();
        Map<String, Double> hitRatios = cacheUtils.getHitRatios();

        Map<String, AccessMetricsDto> result = new HashMap<>();

        // Combine all metrics for each comic
        accessCounts.forEach((comic, count) -> {
            AccessMetricsDto metrics = AccessMetricsDto.builder()
                    .comicName(comic)
                    .accessCount(count)
                    .lastAccess(lastAccessTimes.getOrDefault(comic, ""))
                    .averageAccessTime(avgAccessTimes.getOrDefault(comic, 0.0))
                    .hitRatio(hitRatios.getOrDefault(comic, 0.0))
                    .build();

            result.put(comic, metrics);
        });

        return ResponseBuilder.ok(result);
    }

    /**
     * Get combined metrics including both storage and access metrics.
     *
     * @return Combined metrics for all comics
     */
    @GetMapping("/combined")
    public ResponseEntity<ApiResponse<Map<String, CombinedMetricsDto>>> getCombinedMetrics() {
        ImageCacheStats storageStats = cacheStatsUpdater.cacheStats();
        Map<String, AccessMetricsDto> accessMetrics = getAccessMetrics().getBody().getData();

        Map<String, CombinedMetricsDto> combinedMetrics = new HashMap<>();

        // First add all comics from storage metrics
        if (storageStats.getPerComicMetrics() != null) {
            storageStats.getPerComicMetrics().forEach((comicName, storageMetric) -> {
                AccessMetricsDto accessMetric = accessMetrics.getOrDefault(comicName,
                        AccessMetricsDto.builder().comicName(comicName).build());

                CombinedMetricsDto combined = CombinedMetricsDto.builder()
                        .comicName(comicName)
                        .storageBytes(storageMetric.getStorageBytes())
                        .imageCount(storageMetric.getImageCount())
                        .averageImageSize(storageMetric.getAverageImageSize())
                        .storageByYear(storageMetric.getStorageByYear())
                        .accessCount(accessMetric.getAccessCount())
                        .lastAccess(accessMetric.getLastAccess())
                        .averageAccessTime(accessMetric.getAverageAccessTime())
                        .hitRatio(accessMetric.getHitRatio())
                        .build();

                combinedMetrics.put(comicName, combined);
            });
        }

        // Then add any comics that only have access metrics but no storage metrics
        accessMetrics.forEach((comicName, accessMetric) -> {
            if (!combinedMetrics.containsKey(comicName)) {
                CombinedMetricsDto combined = CombinedMetricsDto.builder()
                        .comicName(comicName)
                        .accessCount(accessMetric.getAccessCount())
                        .lastAccess(accessMetric.getLastAccess())
                        .averageAccessTime(accessMetric.getAverageAccessTime())
                        .hitRatio(accessMetric.getHitRatio())
                        .build();

                combinedMetrics.put(comicName, combined);
            }
        });

        return ResponseBuilder.ok(combinedMetrics);
    }

    /**
     * Force an update of the storage metrics before returning them.
     * This is useful when storage changes have been made and metrics need to be refreshed.
     *
     * @return Updated storage metrics for the cache
     */
    @GetMapping("/storage/refresh")
    public ResponseEntity<ApiResponse<ImageCacheStats>> refreshStorageMetrics() {
        cacheStatsUpdater.updateStats();
        return getStorageMetrics();
    }
}

/**
 * DTO for access metrics
 */
@lombok.Builder
@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
class AccessMetricsDto {
    private String comicName;
    @lombok.Builder.Default
    private int accessCount = 0;
    @lombok.Builder.Default
    private String lastAccess = "";
    @lombok.Builder.Default
    private double averageAccessTime = 0.0;
    @lombok.Builder.Default
    private double hitRatio = 0.0;
}

/**
 * DTO for combined storage and access metrics
 */
@lombok.Builder
@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
class CombinedMetricsDto {
    private String comicName;

    // Storage metrics
    @lombok.Builder.Default
    private long storageBytes = 0;
    @lombok.Builder.Default
    private int imageCount = 0;
    @lombok.Builder.Default
    private double averageImageSize = 0.0;
    private Map<String, Long> storageByYear;

    // Access metrics
    @lombok.Builder.Default
    private int accessCount = 0;
    @lombok.Builder.Default
    private String lastAccess = "";
    @lombok.Builder.Default
    private double averageAccessTime = 0.0;
    @lombok.Builder.Default
    private double hitRatio = 0.0;
}