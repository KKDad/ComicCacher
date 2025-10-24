package org.stapledon.metrics.service;

import org.springframework.stereotype.Service;
import org.stapledon.common.dto.ImageCacheStats;
import org.stapledon.metrics.collector.AccessMetricsCollector;
import org.stapledon.metrics.collector.StorageMetricsCollector;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.metrics.repository.AccessMetricsRepository;
import org.stapledon.metrics.repository.CombinedMetricsRepository;
import org.stapledon.metrics.repository.MetricsArchiver;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of MetricsService.
 * Delegates to collectors and repositories to provide unified metrics access.
 */
@Slf4j
@ToString
@Service
@RequiredArgsConstructor
public class MetricsServiceImpl implements MetricsService {

    private final StorageMetricsCollector storageMetricsCollector;
    private final AccessMetricsCollector accessMetricsCollector;
    private final AccessMetricsRepository accessMetricsRepository;
    private final CombinedMetricsRepository combinedMetricsRepository;
    private final MetricsArchiver metricsArchiver;
    private final MetricsUpdateService metricsUpdateService;

    @Override
    public ImageCacheStats getStorageMetrics() {
        log.debug("Getting storage metrics");
        return storageMetricsCollector.cacheStats();
    }

    @Override
    public AccessMetricsData getAccessMetrics() {
        log.debug("Getting access metrics");
        return accessMetricsRepository.get();
    }

    @Override
    public CombinedMetricsData getCombinedMetrics() {
        log.debug("Getting combined metrics");
        return combinedMetricsRepository.get();
    }

    @Override
    public ImageCacheStats refreshStorageMetrics() {
        log.info("Refreshing storage metrics");
        storageMetricsCollector.updateStats();
        return storageMetricsCollector.cacheStats();
    }

    @Override
    public void refreshAllMetrics() {
        log.info("Refreshing all metrics");
        metricsUpdateService.forceRefreshAll();
    }

    @Override
    public boolean archiveCurrentMetrics() {
        log.info("Archiving current metrics");
        try {
            CombinedMetricsData currentMetrics = combinedMetricsRepository.get();
            return metricsArchiver.archiveMetrics(currentMetrics, LocalDate.now());
        } catch (Exception e) {
            log.error("Failed to archive metrics", e);
            return false;
        }
    }
}
