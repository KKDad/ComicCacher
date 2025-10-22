package org.stapledon.metrics.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.stapledon.api.dto.comic.ComicStorageMetrics;
import org.stapledon.api.dto.comic.ImageCacheStats;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.infrastructure.caching.CacheUtils;
import org.stapledon.infrastructure.caching.ImageCacheStatsUpdater;
import org.stapledon.metrics.config.MetricsProperties;
import org.stapledon.metrics.repository.AccessMetricsRepository;
import org.stapledon.metrics.repository.CombinedMetricsRepository;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for scheduled metrics updates.
 * Periodically persists access metrics and rebuilds combined metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "comics.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MetricsUpdateService {

    private final CacheUtils cacheUtils;
    private final ImageCacheStatsUpdater storageMetricsUpdater;
    private final AccessMetricsRepository accessMetricsRepository;
    private final CombinedMetricsRepository combinedMetricsRepository;
    private final MetricsProperties metricsProperties;

    /**
     * Scheduled task to persist access metrics and rebuild combined metrics.
     * Runs every N seconds (configured via comics.metrics.persist-interval-seconds, default 5 minutes)
     */
    @Scheduled(fixedDelayString = "${comics.metrics.persist-interval-seconds:300}000")
    public void updateMetrics() {
        try {
            log.debug("Starting scheduled metrics update");

            // Step 1: Persist current access metrics from in-memory state
            cacheUtils.persistAccessMetrics();

            // Step 2: Rebuild combined metrics
            rebuildCombinedMetrics();

            log.debug("Scheduled metrics update completed successfully");
        } catch (Exception e) {
            log.error("Failed to update metrics", e);
        }
    }

    /**
     * Force a refresh of all metrics immediately.
     * This includes storage metrics, access metrics, and combined metrics.
     */
    public void forceRefreshAll() {
        try {
            log.info("Force refreshing all metrics");

            // Refresh storage metrics by scanning filesystem
            storageMetricsUpdater.updateStats();

            // Persist current access metrics
            cacheUtils.persistAccessMetrics();

            // Rebuild combined metrics
            rebuildCombinedMetrics();

            log.info("All metrics refreshed successfully");
        } catch (Exception e) {
            log.error("Failed to force refresh all metrics", e);
        }
    }

    /**
     * Rebuild combined metrics from storage and access metrics.
     * This combines data from ImageCacheStats and AccessMetricsData.
     */
    public void rebuildCombinedMetrics() {
        try {
            // Get latest storage metrics
            ImageCacheStats storageStats = storageMetricsUpdater.cacheStats();

            // Get latest access metrics
            AccessMetricsData accessData = accessMetricsRepository.get();

            // Build combined metrics
            CombinedMetricsData combinedData = CombinedMetricsData.builder().build();
            Map<String, CombinedMetricsData.ComicCombinedMetrics> combinedMap = new HashMap<>();

            // Start with all comics from storage metrics
            if (storageStats != null && storageStats.getPerComicMetrics() != null) {
                storageStats.getPerComicMetrics().forEach((comicName, storageMetric) -> {
                    CombinedMetricsData.ComicCombinedMetrics.ComicCombinedMetricsBuilder builder =
                        CombinedMetricsData.ComicCombinedMetrics.builder()
                            .comicName(comicName)
                            .storageBytes(storageMetric.getStorageBytes())
                            .imageCount(storageMetric.getImageCount())
                            .averageImageSize(storageMetric.getAverageImageSize())
                            .storageByYear(storageMetric.getStorageByYear());

                    // Add access metrics if available
                    if (accessData != null && accessData.getComicMetrics() != null) {
                        AccessMetricsData.ComicAccessMetrics accessMetric =
                            accessData.getComicMetrics().get(comicName);

                        if (accessMetric != null) {
                            builder.accessCount(accessMetric.getAccessCount())
                                   .lastAccess(accessMetric.getLastAccess())
                                   .averageAccessTime(accessMetric.getAverageAccessTime())
                                   .hitRatio(accessMetric.getHitRatio())
                                   .cacheHits(accessMetric.getCacheHits())
                                   .cacheMisses(accessMetric.getCacheMisses());
                        }
                    }

                    combinedMap.put(comicName, builder.build());
                });
            }

            // Add any comics that only have access metrics but no storage metrics
            if (accessData != null && accessData.getComicMetrics() != null) {
                accessData.getComicMetrics().forEach((comicName, accessMetric) -> {
                    if (!combinedMap.containsKey(comicName)) {
                        CombinedMetricsData.ComicCombinedMetrics combined =
                            CombinedMetricsData.ComicCombinedMetrics.builder()
                                .comicName(comicName)
                                .accessCount(accessMetric.getAccessCount())
                                .lastAccess(accessMetric.getLastAccess())
                                .averageAccessTime(accessMetric.getAverageAccessTime())
                                .hitRatio(accessMetric.getHitRatio())
                                .cacheHits(accessMetric.getCacheHits())
                                .cacheMisses(accessMetric.getCacheMisses())
                                .build();

                        combinedMap.put(comicName, combined);
                    }
                });
            }

            combinedData.setComics(combinedMap);

            // Save combined metrics
            combinedMetricsRepository.save(combinedData);

            log.debug("Rebuilt combined metrics for {} comics", combinedMap.size());
        } catch (Exception e) {
            log.error("Failed to rebuild combined metrics", e);
        }
    }
}
