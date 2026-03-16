package org.stapledon.api.dto.metrics;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Typed wrapper for AccessMetrics data from CombinedMetrics.
 * Eliminates the need for instanceof checks in AccessMetricsTypeResolver.
 */
public record AccessMetricsView(
        int totalAccesses,
        List<ComicAccessMetricView> comics,
        OffsetDateTime lastUpdated) {
}
