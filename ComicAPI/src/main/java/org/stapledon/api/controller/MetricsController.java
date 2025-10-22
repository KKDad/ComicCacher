package org.stapledon.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stapledon.api.dto.comic.ImageCacheStats;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.model.ResponseBuilder;
import org.stapledon.infrastructure.caching.ImageCacheStatsUpdater;
import org.stapledon.metrics.repository.AccessMetricsRepository;
import org.stapledon.metrics.repository.CombinedMetricsRepository;
import org.stapledon.infrastructure.metrics.MetricsUpdateService;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;

/**
 * Controller for accessing cache metrics and statistics.
 * Provides endpoints for storage utilization and access patterns.
 * All metrics are served from pre-computed JSON files for optimal performance.
 */
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricsController {
    private final ImageCacheStatsUpdater cacheStatsUpdater;
    private final AccessMetricsRepository accessMetricsRepository;
    private final CombinedMetricsRepository combinedMetricsRepository;
    private final MetricsUpdateService metricsUpdateService;

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
     * Reads from cached access-metrics.json file.
     *
     * @return Access metrics for all comics
     */
    @GetMapping("/access")
    public ResponseEntity<ApiResponse<AccessMetricsData>> getAccessMetrics() {
        AccessMetricsData metrics = accessMetricsRepository.get();
        return ResponseBuilder.ok(metrics);
    }

    /**
     * Get combined metrics including both storage and access metrics.
     * Reads from pre-computed combined-metrics.json file.
     *
     * @return Combined metrics for all comics
     */
    @GetMapping("/combined")
    public ResponseEntity<ApiResponse<CombinedMetricsData>> getCombinedMetrics() {
        CombinedMetricsData metrics = combinedMetricsRepository.get();
        return ResponseBuilder.ok(metrics);
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

    /**
     * Force a refresh of all metrics (storage, access, and combined).
     * This triggers filesystem scanning, access metrics persistence, and combined metrics rebuild.
     *
     * @return Success message
     */
    @GetMapping("/refresh-all")
    public ResponseEntity<ApiResponse<String>> refreshAllMetrics() {
        metricsUpdateService.forceRefreshAll();
        return ResponseBuilder.ok("All metrics refreshed successfully");
    }
}