package org.stapledon.api.resolver;

import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.common.dto.ComicStorageMetrics;
import org.stapledon.common.dto.ImageCacheStats;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schema mappings for the StorageMetrics GraphQL type.
 * Bridges ImageCacheStats (standalone query) and Map (from CombinedMetrics) to the schema.
 */
@Controller
public class StorageMetricsTypeResolver {

    /**
     * Map ImageCacheStats.totalStorageBytes to StorageMetrics.totalBytes.
     */
    @SchemaMapping(typeName = "StorageMetrics", field = "totalBytes")
    public double totalBytes(ImageCacheStats stats) {
        return stats.getTotalStorageBytes();
    }

    /**
     * Map ImageCacheStats.perComicMetrics.size() to StorageMetrics.comicCount.
     */
    @SchemaMapping(typeName = "StorageMetrics", field = "comicCount")
    public int comicCount(ImageCacheStats stats) {
        Map<String, ComicStorageMetrics> perComic = stats.getPerComicMetrics();
        return perComic != null ? perComic.size() : 0;
    }

    /**
     * Convert ImageCacheStats.perComicMetrics Map to StorageMetrics.comics List.
     */
    @SchemaMapping(typeName = "StorageMetrics", field = "comics")
    public List<Map<String, Object>> comics(ImageCacheStats stats) {
        Map<String, ComicStorageMetrics> perComic = stats.getPerComicMetrics();
        if (perComic == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>(perComic.size());
        for (Map.Entry<String, ComicStorageMetrics> entry : perComic.entrySet()) {
            ComicStorageMetrics m = entry.getValue();
            Map<String, Object> comic = new LinkedHashMap<>();
            comic.put("comicName", entry.getKey());
            comic.put("totalBytes", (double) m.getStorageBytes());
            comic.put("imageCount", m.getImageCount());
            comic.put("_storageByYear", m.getStorageByYear());
            result.add(comic);
        }
        return result;
    }

    /**
     * Map StorageMetrics.lastUpdated — not available in ImageCacheStats.
     */
    @SchemaMapping(typeName = "StorageMetrics", field = "lastUpdated")
    public OffsetDateTime lastUpdated(Object source) {
        if (source instanceof ImageCacheStats) {
            return null;
        }
        if (source instanceof Map) {
            Object val = ((Map<?, ?>) source).get("_lastUpdated");
            if (val instanceof OffsetDateTime) {
                return (OffsetDateTime) val;
            }
        }
        return null;
    }
}
