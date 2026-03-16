package org.stapledon.api.dto.metrics;

import java.time.OffsetDateTime;

/**
 * Typed wrapper for per-comic access metric data.
 */
public record ComicAccessMetricView(
        String comicName,
        int accessCount,
        double averageAccessTimeMs,
        OffsetDateTime lastAccessed) {
}
