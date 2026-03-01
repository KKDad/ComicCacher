package org.stapledon.api.resolver;

import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.common.dto.ImageCacheStats;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.metrics.service.MetricsService;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL resolver for Metrics operations.
 * Provides queries for storage, access, and combined metrics.
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
    // Schema Mappings - Type Conversions
    // =========================================================================

    /**
     * Return lastUpdated as OffsetDateTime for GraphQL DateTime scalar.
     * DTOs now use OffsetDateTime directly.
     */
    @SchemaMapping(typeName = "CombinedMetrics", field = "lastUpdated")
    public OffsetDateTime combinedMetricsLastUpdated(CombinedMetricsData data) {
        return data.getLastUpdated();
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
