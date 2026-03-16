package org.stapledon.api.dto.metrics;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Typed wrapper for StorageMetrics data from CombinedMetrics.
 * Eliminates the need for instanceof checks in StorageMetricsTypeResolver.
 */
public record StorageMetricsView(
        double totalBytes,
        int comicCount,
        List<ComicStorageMetricView> comics,
        OffsetDateTime lastUpdated) {
}
