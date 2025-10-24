package org.stapledon.metrics.service;

import org.stapledon.common.dto.ImageCacheStats;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.dto.CombinedMetricsData;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * No-operation implementation of MetricsService.
 * Used when metrics are disabled via configuration (comics.metrics.enabled=false).
 * Returns empty/default values for all operations.
 */
@Slf4j
@ToString
public class NoOpMetricsService implements MetricsService {

    public NoOpMetricsService() {
        log.info("Metrics are disabled - using NoOpMetricsService");
    }

    @Override
    public ImageCacheStats getStorageMetrics() {
        log.debug("Metrics disabled - returning empty storage metrics");
        return new ImageCacheStats();
    }

    @Override
    public AccessMetricsData getAccessMetrics() {
        log.debug("Metrics disabled - returning empty access metrics");
        return AccessMetricsData.builder().build();
    }

    @Override
    public CombinedMetricsData getCombinedMetrics() {
        log.debug("Metrics disabled - returning empty combined metrics");
        return CombinedMetricsData.builder().build();
    }

    @Override
    public ImageCacheStats refreshStorageMetrics() {
        log.debug("Metrics disabled - refresh storage metrics no-op");
        return new ImageCacheStats();
    }

    @Override
    public void refreshAllMetrics() {
        log.debug("Metrics disabled - refresh all metrics no-op");
    }

    @Override
    public boolean archiveCurrentMetrics() {
        log.debug("Metrics disabled - archive metrics no-op");
        return false;
    }
}
