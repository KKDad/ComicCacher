package org.stapledon.metrics.service;

import org.stapledon.api.dto.comic.ImageCacheStats;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.dto.CombinedMetricsData;

/**
 * Facade interface for all metrics operations.
 * Provides a single entry point for accessing and managing metrics,
 * abstracting the underlying collectors, repositories, and services.
 *
 * This interface allows for easy conditional behavior when metrics are disabled.
 */
public interface MetricsService {

    /**
     * Get current storage metrics for all comics.
     * These metrics are computed from filesystem scans.
     *
     * @return Storage metrics including size and comic counts
     */
    ImageCacheStats getStorageMetrics();

    /**
     * Get access metrics for all comics.
     * These metrics track API access patterns and cache hit/miss rates.
     *
     * @return Access metrics data
     */
    AccessMetricsData getAccessMetrics();

    /**
     * Get combined metrics including both storage and access data.
     * This is a pre-computed aggregation for efficient retrieval.
     *
     * @return Combined metrics data
     */
    CombinedMetricsData getCombinedMetrics();

    /**
     * Force an immediate refresh of storage metrics.
     * Triggers a filesystem scan to update storage statistics.
     *
     * @return Updated storage metrics
     */
    ImageCacheStats refreshStorageMetrics();

    /**
     * Force an immediate refresh of all metrics.
     * This includes storage metrics scan, access metrics persistence,
     * and combined metrics rebuild.
     */
    void refreshAllMetrics();

    /**
     * Archive current metrics for historical analysis.
     * Creates a snapshot of combined metrics for the current day.
     *
     * @return true if archiving was successful
     */
    boolean archiveCurrentMetrics();
}
