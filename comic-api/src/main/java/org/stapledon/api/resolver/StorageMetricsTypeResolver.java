package org.stapledon.api.resolver;

import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.api.dto.metrics.ComicStorageMetricView;
import org.stapledon.api.dto.metrics.StorageMetricsView;
import org.stapledon.common.dto.ComicStorageMetrics;
import org.stapledon.common.dto.ImageCacheStats;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Schema mappings for the StorageMetrics GraphQL type.
 * Bridges ImageCacheStats (standalone query) and StorageMetricsView (from CombinedMetrics) to the schema.
 */
@Controller
public class StorageMetricsTypeResolver {

    /**
     * Map to StorageMetrics.totalBytes.
     */
    @SchemaMapping(typeName = "StorageMetrics", field = "totalBytes")
    public double totalBytes(Object source) {
        if (source instanceof ImageCacheStats stats) {
            return stats.getTotalStorageBytes();
        }
        if (source instanceof StorageMetricsView view) {
            return view.totalBytes();
        }
        return 0.0;
    }

    /**
     * Map to StorageMetrics.comicCount.
     */
    @SchemaMapping(typeName = "StorageMetrics", field = "comicCount")
    public int comicCount(Object source) {
        if (source instanceof ImageCacheStats stats) {
            return Optional.ofNullable(stats.getPerComicMetrics())
                    .map(java.util.Map::size)
                    .orElse(0);
        }
        if (source instanceof StorageMetricsView view) {
            return view.comicCount();
        }
        return 0;
    }

    /**
     * Convert to StorageMetrics.comics List.
     */
    @SchemaMapping(typeName = "StorageMetrics", field = "comics")
    public List<ComicStorageMetricView> comics(Object source) {
        if (source instanceof ImageCacheStats stats) {
            return Optional.ofNullable(stats.getPerComicMetrics())
                    .map(StorageMetricsTypeResolver::buildComicsList)
                    .orElse(Collections.emptyList());
        }
        if (source instanceof StorageMetricsView view) {
            return view.comics();
        }
        return Collections.emptyList();
    }

    /**
     * Map StorageMetrics.lastUpdated.
     */
    @SchemaMapping(typeName = "StorageMetrics", field = "lastUpdated")
    public OffsetDateTime lastUpdated(Object source) {
        if (source instanceof ImageCacheStats) {
            return null;
        }
        if (source instanceof StorageMetricsView view) {
            return view.lastUpdated();
        }
        return null;
    }

    private static List<ComicStorageMetricView> buildComicsList(java.util.Map<String, ComicStorageMetrics> perComic) {
        List<ComicStorageMetricView> result = new ArrayList<>(perComic.size());
        for (java.util.Map.Entry<String, ComicStorageMetrics> entry : perComic.entrySet()) {
            ComicStorageMetrics m = entry.getValue();
            result.add(new ComicStorageMetricView(
                    entry.getKey(),
                    (double) m.getStorageBytes(),
                    m.getImageCount(),
                    m.getStorageByYear()));
        }
        return result;
    }
}
