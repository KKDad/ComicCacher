package org.stapledon.metrics.repository;

import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.metrics.dto.GlobalMetrics;

import java.util.Optional;

/**
 * Repository interface for metrics persistence.
 * Implementations handle the actual storage mechanism (JSON, Prometheus, etc.).
 */
public interface MetricsRepository {

    /**
     * Save combined metrics data to the backing store.
     *
     * @param data The combined metrics to persist
     * @return true if successful, false otherwise
     */
    boolean save(CombinedMetricsData data);

    /**
     * Load combined metrics from the backing store.
     *
     * @return The loaded metrics, or an empty object if none exist
     */
    CombinedMetricsData load();

    /**
     * Get the current combined metrics (may be cached).
     *
     * @return Current combined metrics data
     */
    CombinedMetricsData get();

    /**
     * Get global aggregate metrics.
     *
     * @return Global metrics, or null if not available
     */
    default GlobalMetrics getGlobalMetrics() {
        CombinedMetricsData data = get();
        return data != null ? data.getGlobalMetrics() : null;
    }

    /**
     * Get metrics for a specific comic.
     *
     * @param comicName The comic name
     * @return Optional containing comic metrics if found
     */
    default Optional<CombinedMetricsData.ComicCombinedMetrics> getComicMetrics(String comicName) {
        CombinedMetricsData data = get();
        if (data == null || data.getPerComicMetrics() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(data.getPerComicMetrics().get(comicName));
    }
}
