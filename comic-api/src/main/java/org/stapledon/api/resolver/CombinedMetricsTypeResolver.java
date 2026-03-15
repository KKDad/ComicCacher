package org.stapledon.api.resolver;

import static org.stapledon.common.util.DateTimeUtils.parseDateTime;

import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.metrics.dto.CombinedMetricsData.ComicCombinedMetrics;
import org.stapledon.metrics.dto.YearlyStorageMetrics;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schema mappings for the CombinedMetrics GraphQL type.
 * Bridges CombinedMetricsData to the GraphQL schema.
 */
@Controller
public class CombinedMetricsTypeResolver {

    /**
     * Return lastUpdated as OffsetDateTime for GraphQL DateTime scalar.
     */
    @SchemaMapping(typeName = "CombinedMetrics", field = "lastUpdated")
    public OffsetDateTime lastUpdated(CombinedMetricsData data) {
        return data.getLastUpdated();
    }

    /**
     * Build StorageMetrics from CombinedMetricsData for CombinedMetrics.storage.
     */
    @SchemaMapping(typeName = "CombinedMetrics", field = "storage")
    public Map<String, Object> storage(CombinedMetricsData data) {
        Map<String, Object> storage = new LinkedHashMap<>();

        long totalBytes = 0;
        if (data.getGlobalMetrics() != null) {
            totalBytes = data.getGlobalMetrics().getTotalStorageBytes();
        }
        storage.put("totalBytes", (double) totalBytes);

        Map<String, ComicCombinedMetrics> perComic = data.getPerComicMetrics();
        int comicCount = perComic != null ? perComic.size() : 0;
        storage.put("comicCount", comicCount);

        List<Map<String, Object>> comics = new ArrayList<>();
        if (perComic != null) {
            for (Map.Entry<String, ComicCombinedMetrics> entry : perComic.entrySet()) {
                ComicCombinedMetrics m = entry.getValue();
                Map<String, Object> comic = new LinkedHashMap<>();
                comic.put("comicName", entry.getKey());
                comic.put("totalBytes", (double) m.getStorageBytes());
                comic.put("imageCount", m.getImageCount());
                comic.put("_storageByYear", buildYearlyFromCombined(m));
                comics.add(comic);
            }
        }
        storage.put("comics", comics);

        if (data.getLastUpdated() != null) {
            storage.put("_lastUpdated", data.getLastUpdated());
        }

        return storage;
    }

    /**
     * Build AccessMetrics from CombinedMetricsData for CombinedMetrics.access.
     */
    @SchemaMapping(typeName = "CombinedMetrics", field = "access")
    public Map<String, Object> access(CombinedMetricsData data) {
        Map<String, Object> access = new LinkedHashMap<>();

        Map<String, ComicCombinedMetrics> perComic = data.getPerComicMetrics();
        int totalAccesses = 0;
        List<Map<String, Object>> comics = new ArrayList<>();

        if (perComic != null) {
            for (Map.Entry<String, ComicCombinedMetrics> entry : perComic.entrySet()) {
                ComicCombinedMetrics m = entry.getValue();
                totalAccesses += m.getAccessCount();

                Map<String, Object> comic = new LinkedHashMap<>();
                comic.put("comicName", entry.getKey());
                comic.put("accessCount", m.getAccessCount());
                comic.put("averageAccessTimeMs", m.getAverageAccessTime());
                comic.put("lastAccessed", parseDateTime(m.getLastAccess()));
                comics.add(comic);
            }
        }

        access.put("totalAccesses", totalAccesses);
        access.put("comics", comics);

        if (data.getLastUpdated() != null) {
            access.put("lastUpdated", data.getLastUpdated());
        }

        return access;
    }

    private Map<String, Long> buildYearlyFromCombined(ComicCombinedMetrics m) {
        Map<String, YearlyStorageMetrics> yearly = m.getYearlyStorage();
        if (yearly == null) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, YearlyStorageMetrics> entry : yearly.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getStorageBytes());
        }
        return result;
    }
}
