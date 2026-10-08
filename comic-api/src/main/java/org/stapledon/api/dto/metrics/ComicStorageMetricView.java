package org.stapledon.api.dto.metrics;

import java.util.Map;

/**
 * Typed wrapper for per-comic storage metric data.
 */
public record ComicStorageMetricView(
        Integer comicId,
        String comicName,
        double totalBytes,
        int imageCount,
        Map<String, Long> storageByYear) {
}
