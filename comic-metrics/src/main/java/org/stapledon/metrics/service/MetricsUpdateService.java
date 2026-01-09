package org.stapledon.metrics.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.stapledon.common.dto.ComicStorageMetrics;
import org.stapledon.common.dto.ImageCacheStats;
import org.stapledon.metrics.collector.AccessMetricsCollector;
import org.stapledon.metrics.collector.StorageMetricsCollector;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.metrics.dto.GlobalMetrics;
import org.stapledon.metrics.dto.YearlyStorageMetrics;
import org.stapledon.metrics.repository.AccessMetricsRepository;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for scheduled metrics updates. Periodically persists access metrics and rebuilds combined metrics.
 */
@Slf4j @ToString @Service @RequiredArgsConstructor @ConditionalOnProperty(prefix = "comics.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MetricsUpdateService {

    private final AccessMetricsCollector accessMetricsCollector;
    private final StorageMetricsCollector storageMetricsUpdater;
    private final AccessMetricsRepository accessMetricsRepository;

    /**
     * Force a refresh of all metrics immediately. This includes storage metrics and access metrics persistence. Combined metrics are computed on-demand via buildCombinedMetrics().
     */
    public void forceRefreshAll() {
        try {
            log.info("Force refreshing all metrics");

            // Refresh storage metrics by scanning filesystem
            storageMetricsUpdater.updateStats();

            // Persist current access metrics
            accessMetricsCollector.persistAccessMetrics();

            log.info("All metrics refreshed successfully");
        } catch (Exception e) {
            log.error("Failed to force refresh all metrics", e);
        }
    }

    /**
     * Build combined metrics from storage and access metrics. This combines data from ImageCacheStats and AccessMetricsData into a single structure. The data is computed on-demand and
     * returned without persisting.
     *
     * @return Combined metrics data, or empty data if an error occurs
     */
    public CombinedMetricsData buildCombinedMetrics() {
        try {
            // Get latest storage metrics
            ImageCacheStats storageStats = storageMetricsUpdater.cacheStats();

            // Get latest access metrics
            AccessMetricsData accessData = accessMetricsRepository.get();

            // Build global metrics from storage stats
            GlobalMetrics globalMetrics = buildGlobalMetrics(storageStats);

            // Build per-comic metrics
            Map<String, CombinedMetricsData.ComicCombinedMetrics> perComicMetrics = new HashMap<>();

            // Start with all comics from storage metrics
            if (storageStats != null && storageStats.getPerComicMetrics() != null) {
                storageStats.getPerComicMetrics().forEach((comicName, storageMetric) -> {
                    CombinedMetricsData.ComicCombinedMetrics.ComicCombinedMetricsBuilder builder = CombinedMetricsData.ComicCombinedMetrics.builder().comicName(comicName)
                            .storageBytes(storageMetric.getStorageBytes()).imageCount(storageMetric.getImageCount()).averageImageSize(storageMetric.getAverageImageSize())
                            .yearlyStorage(buildYearlyStorage(storageMetric));

                    // Add access metrics if available
                    if (accessData != null && accessData.getComicMetrics() != null) {
                        AccessMetricsData.ComicAccessMetrics accessMetric = accessData.getComicMetrics().get(comicName);

                        if (accessMetric != null) {
                            builder.accessCount(accessMetric.getAccessCount()).lastAccess(accessMetric.getLastAccess()).averageAccessTime(accessMetric.getAverageAccessTime())
                                    .hitRatio(accessMetric.getHitRatio()).cacheHits(accessMetric.getCacheHits()).cacheMisses(accessMetric.getCacheMisses());
                        }
                    }

                    perComicMetrics.put(comicName, builder.build());
                });
            }

            // Add any comics that only have access metrics but no storage metrics
            if (accessData != null && accessData.getComicMetrics() != null) {
                accessData.getComicMetrics().forEach((comicName, accessMetric) -> {
                    if (!perComicMetrics.containsKey(comicName)) {
                        CombinedMetricsData.ComicCombinedMetrics combined = CombinedMetricsData.ComicCombinedMetrics.builder().comicName(comicName).accessCount(accessMetric.getAccessCount())
                                .lastAccess(accessMetric.getLastAccess()).averageAccessTime(accessMetric.getAverageAccessTime()).hitRatio(accessMetric.getHitRatio())
                                .cacheHits(accessMetric.getCacheHits()).cacheMisses(accessMetric.getCacheMisses()).build();

                        perComicMetrics.put(comicName, combined);
                    }
                });
            }

            // Build combined metrics (no longer saved to disk)
            CombinedMetricsData combinedData = CombinedMetricsData.builder().globalMetrics(globalMetrics).perComicMetrics(perComicMetrics).lastUpdated(java.time.LocalDateTime.now()).build();

            log.debug("Built combined metrics for {} comics", perComicMetrics.size());
            return combinedData;
        } catch (Exception e) {
            log.error("Failed to build combined metrics", e);
            return CombinedMetricsData.builder().lastUpdated(java.time.LocalDateTime.now()).build();
        }
    }

    /**
     * Build global metrics from storage stats.
     */
    private GlobalMetrics buildGlobalMetrics(ImageCacheStats storageStats) {
        if (storageStats == null) {
            return GlobalMetrics.builder().build();
        }

        return GlobalMetrics.builder().oldestImage(storageStats.getOldestImage()).newestImage(storageStats.getNewestImage()).years(storageStats.getYears())
                .totalStorageBytes(storageStats.getTotalStorageBytes()).totalImageCount(calculateTotalImageCount(storageStats)).storageByYear(storageStats.getStorageBytesByYear())
                .imageCountByYear(storageStats.getImageCountByYear()).build();
    }

    /**
     * Calculate total image count from per-comic metrics.
     */
    private int calculateTotalImageCount(ImageCacheStats stats) {
        if (stats.getPerComicMetrics() == null) {
            return 0;
        }
        return stats.getPerComicMetrics().values().stream().mapToInt(ComicStorageMetrics::getImageCount).sum();
    }

    /**
     * Build yearly storage metrics from comic storage metrics.
     */
    private Map<String, YearlyStorageMetrics> buildYearlyStorage(ComicStorageMetrics storageMetric) {
        Map<String, YearlyStorageMetrics> yearlyStorage = new HashMap<>();

        if (storageMetric.getStorageByYear() != null) {
            storageMetric.getStorageByYear().forEach((year, bytes) -> {
                yearlyStorage.put(year, YearlyStorageMetrics.builder().storageBytes(bytes)
                        // Note: Image count per year per comic not currently tracked in
                        // ComicStorageMetrics
                        // Would need to enhance scanning to get this
                        .imageCount(0).build());
            });
        }

        return yearlyStorage;
    }
}
