package org.stapledon.api.resolver;

import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.common.dto.ImageCacheStats;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.metrics.service.MetricsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL resolver for Metrics queries and mutations.
 * Schema mappings for individual types are in dedicated type resolver classes.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MetricsResolver {

    private final MetricsService metricsService;

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Get storage metrics for the comic cache.
     */
    @QueryMapping
    public ImageCacheStats storageMetrics() {
        log.debug("Getting storage metrics");
        return metricsService.getStorageMetrics();
    }

    /**
     * Get access metrics for all comics.
     */
    @QueryMapping
    public AccessMetricsData accessMetrics() {
        log.debug("Getting access metrics");
        return metricsService.getAccessMetrics();
    }

    /**
     * Get combined storage and access metrics.
     */
    @QueryMapping
    public CombinedMetricsData combinedMetrics() {
        log.debug("Getting combined metrics");
        return metricsService.getCombinedMetrics();
    }

    // =========================================================================
    // Mutations
    // =========================================================================

    /**
     * Force a refresh of storage metrics.
     */
    @MutationMapping
    public ImageCacheStats refreshStorageMetrics() {
        log.info("Refreshing storage metrics");
        return metricsService.refreshStorageMetrics();
    }

    /**
     * Force a refresh of all metrics.
     */
    @MutationMapping
    public boolean refreshAllMetrics() {
        log.info("Refreshing all metrics");
        metricsService.refreshAllMetrics();
        return true;
    }
}
