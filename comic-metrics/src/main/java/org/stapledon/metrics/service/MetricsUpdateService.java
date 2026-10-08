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
import org.stapledon.metrics.repository.MetricsArchiver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for scheduled metrics updates. Periodically persists access metrics
 * and rebuilds combined metrics.
 */
@Slf4j
@ToString
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "comics.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MetricsUpdateService {

    private final AccessMetricsCollector accessMetricsCollector;
    private final StorageMetricsCollector storageMetricsUpdater;
    private final AccessMetricsRepository accessMetricsRepository;
    private final MetricsArchiver metricsArchiver;

    /**
     * Force a refresh of all metrics immediately. This includes storage metrics and
     * access metrics persistence. Combined metrics are computed on-demand via
     * buildCombinedMetrics().
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
     * Build combined metrics from storage and access metrics. This combines data
     * from ImageCacheStats and AccessMetricsData into a single structure. The data
     * is computed on-demand and
     * returned without persisting.
     *
     * <p>
     * Until the first storage scan finishes (it runs in the background at startup), this returns the latest daily snapshot from
     * {@code metrics-history/} instead of waiting for the scan; its {@code lastUpdated} says how old it is. With no snapshot, it scans.
     *
     * @return Combined metrics data, or empty data if an error occurs
     */
    public CombinedMetricsData buildCombinedMetrics() {
        if (storageMetricsUpdater.currentStats().isEmpty()) {
            Optional<CombinedMetricsData> archived = metricsArchiver.latestArchive();
            if (archived.isPresent()) {
                log.info("Storage scan not finished yet, serving the metrics snapshot from {}", archived.get().getLastUpdated());
                return archived.get();
            }
        }
        long startTime = System.currentTimeMillis();
        try {
            // Get latest storage metrics
            ImageCacheStats storageStats = storageMetricsUpdater.cacheStats();

            // Get latest access metrics
            AccessMetricsData accessData = accessMetricsRepository.get();

            // Build global metrics from storage stats
            GlobalMetrics globalMetrics = buildGlobalMetrics(storageStats);

            // Build per-comic metrics, keyed by directory name. Storage is keyed by directory name ("MotherGoose&Grimm") and
            // access by display name ("Mother Goose & Grimm"), so access is re-keyed before the two are joined.
            Map<String, CombinedMetricsData.ComicCombinedMetrics> perComicMetrics = new HashMap<>();
            Map<String, AccessMetricsData.ComicAccessMetrics> accessByDirectory = accessByDirectory(accessData);

            // Start with all comics from storage metrics
            if (storageStats != null && storageStats.getPerComicMetrics() != null) {
                storageStats.getPerComicMetrics().forEach((directoryName, storageMetric) -> {
                    AccessMetricsData.ComicAccessMetrics accessMetric = accessByDirectory.get(directoryName);
                    CombinedMetricsData.ComicCombinedMetrics.ComicCombinedMetricsBuilder builder = CombinedMetricsData.ComicCombinedMetrics
                            .builder().comicName(accessMetric != null ? accessMetric.getComicName() : directoryName)
                            .storageBytes(storageMetric.getStorageBytes()).imageCount(storageMetric.getImageCount())
                            .averageImageSize(storageMetric.getAverageImageSize())
                            .yearlyStorage(buildYearlyStorage(storageMetric));

                    if (accessMetric != null) {
                        withAccess(builder, accessMetric);
                    }

                    perComicMetrics.put(directoryName, builder.build());
                });
            }

            // Add any comics that only have access metrics but no storage metrics
            accessByDirectory.forEach((directoryName, accessMetric) -> {
                if (!perComicMetrics.containsKey(directoryName)) {
                    perComicMetrics.put(directoryName,
                            withAccess(CombinedMetricsData.ComicCombinedMetrics.builder().comicName(accessMetric.getComicName()), accessMetric).build());
                }
            });

            // Build combined metrics (no longer saved to disk)
            CombinedMetricsData combinedData = CombinedMetricsData.builder().globalMetrics(globalMetrics)
                    .perComicMetrics(perComicMetrics).lastUpdated(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)).build();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Built combined metrics for {} comics in {}ms", perComicMetrics.size(), duration);
            return combinedData;
        } catch (Exception e) {
            log.error("Failed to build combined metrics", e);
            return CombinedMetricsData.builder().lastUpdated(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)).build();
        }
    }

    /**
     * The comic's directory name: its name with spaces removed, as {@code ComicIdentifier.getDirectoryName()} builds it.
     */
    private static String directoryKey(String comicName) {
        return comicName.replace(" ", "");
    }

    /**
     * Access metrics keyed by directory name. Two entries for one comic (older files hold some under the directory name) are added
     * together, keeping the display name.
     */
    private static Map<String, AccessMetricsData.ComicAccessMetrics> accessByDirectory(AccessMetricsData accessData) {
        Map<String, AccessMetricsData.ComicAccessMetrics> result = new HashMap<>();
        if (accessData == null || accessData.getComicMetrics() == null) {
            return result;
        }
        accessData.getComicMetrics().forEach((comicName, metric) -> {
            AccessMetricsData.ComicAccessMetrics keyed = AccessMetricsData.ComicAccessMetrics.builder()
                    .comicName(comicName).accessCount(metric.getAccessCount()).lastAccess(metric.getLastAccess())
                    .totalAccessTimeMs(metric.getTotalAccessTimeMs())
                    .cacheHits(metric.getCacheHits()).cacheMisses(metric.getCacheMisses()).build();
            result.merge(directoryKey(comicName), keyed, MetricsUpdateService::mergeAccess);
        });
        return result;
    }

    private static AccessMetricsData.ComicAccessMetrics mergeAccess(AccessMetricsData.ComicAccessMetrics a,
            AccessMetricsData.ComicAccessMetrics b) {
        String lastA = a.getLastAccess() == null ? "" : a.getLastAccess();
        String lastB = b.getLastAccess() == null ? "" : b.getLastAccess();
        return AccessMetricsData.ComicAccessMetrics.builder()
                .comicName(a.getComicName().contains(" ") ? a.getComicName() : b.getComicName())
                .accessCount(a.getAccessCount() + b.getAccessCount())
                .lastAccess(lastA.compareTo(lastB) >= 0 ? lastA : lastB)
                .totalAccessTimeMs(a.getTotalAccessTimeMs() + b.getTotalAccessTimeMs())
                .cacheHits(a.getCacheHits() + b.getCacheHits())
                .cacheMisses(a.getCacheMisses() + b.getCacheMisses())
                .build();
    }

    private static CombinedMetricsData.ComicCombinedMetrics.ComicCombinedMetricsBuilder withAccess(
            CombinedMetricsData.ComicCombinedMetrics.ComicCombinedMetricsBuilder builder, AccessMetricsData.ComicAccessMetrics accessMetric) {
        return builder.accessCount(accessMetric.getAccessCount()).lastAccess(accessMetric.getLastAccess())
                .averageAccessTime(accessMetric.getAverageAccessTime())
                .hitRatio(accessMetric.getHitRatio()).cacheHits(accessMetric.getCacheHits())
                .cacheMisses(accessMetric.getCacheMisses());
    }

    /**
     * Build global metrics from storage stats.
     */
    private GlobalMetrics buildGlobalMetrics(ImageCacheStats storageStats) {
        if (storageStats == null) {
            return GlobalMetrics.builder().build();
        }

        return GlobalMetrics.builder().oldestImage(storageStats.getOldestImage())
                .newestImage(storageStats.getNewestImage()).years(storageStats.getYears())
                .totalStorageBytes(storageStats.getTotalStorageBytes())
                .totalImageCount(calculateTotalImageCount(storageStats))
                .storageByYear(storageStats.getStorageBytesByYear())
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
            Map<String, Integer> counts = storageMetric.getImageCountByYear() != null ? storageMetric.getImageCountByYear() : Map.of();
            storageMetric.getStorageByYear().forEach((year, bytes) ->
                    yearlyStorage.put(year, YearlyStorageMetrics.builder().storageBytes(bytes)
                            .imageCount(counts.getOrDefault(year, 0)).build()));
        }

        return yearlyStorage;
    }
}
