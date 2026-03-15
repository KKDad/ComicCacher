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
import java.util.Optional;

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
    public double totalBytes(Object source) {
        if (source instanceof ImageCacheStats stats) {
            return stats.getTotalStorageBytes();
        }
        return Optional.ofNullable(source)
                .filter(Map.class::isInstance)
                .map(s -> ((Map<?, ?>) s).get("totalBytes"))
                .map(v -> ((Number) v).doubleValue())
                .orElse(0.0);
    }

    /**
     * Map ImageCacheStats.perComicMetrics.size() to StorageMetrics.comicCount.
     */
    @SchemaMapping(typeName = "StorageMetrics", field = "comicCount")
    public int comicCount(Object source) {
        if (source instanceof ImageCacheStats stats) {
            return Optional.ofNullable(stats.getPerComicMetrics())
                    .map(Map::size)
                    .orElse(0);
        }
        return Optional.ofNullable(source)
                .filter(Map.class::isInstance)
                .map(s -> ((Map<?, ?>) s).get("comicCount"))
                .map(v -> ((Number) v).intValue())
                .orElse(0);
    }

    /**
     * Convert ImageCacheStats.perComicMetrics Map to StorageMetrics.comics List.
     */
    @SuppressWarnings("unchecked")
    @SchemaMapping(typeName = "StorageMetrics", field = "comics")
    public List<Map<String, Object>> comics(Object source) {
        if (source instanceof ImageCacheStats stats) {
            return Optional.ofNullable(stats.getPerComicMetrics())
                    .map(StorageMetricsTypeResolver::buildComicsList)
                    .orElse(Collections.emptyList());
        }
        return Optional.ofNullable(source)
                .filter(Map.class::isInstance)
                .map(s -> ((Map<?, ?>) s).get("comics"))
                .filter(List.class::isInstance)
                .map(c -> (List<Map<String, Object>>) c)
                .orElse(Collections.emptyList());
    }

    /**
     * Map StorageMetrics.lastUpdated — not available in ImageCacheStats.
     */
    @SchemaMapping(typeName = "StorageMetrics", field = "lastUpdated")
    public OffsetDateTime lastUpdated(Object source) {
        if (source instanceof ImageCacheStats) {
            return null;
        }
        return Optional.ofNullable(source)
                .filter(Map.class::isInstance)
                .map(s -> ((Map<?, ?>) s).get("_lastUpdated"))
                .filter(OffsetDateTime.class::isInstance)
                .map(OffsetDateTime.class::cast)
                .orElse(null);
    }

    private static List<Map<String, Object>> buildComicsList(Map<String, ComicStorageMetrics> perComic) {
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
}
