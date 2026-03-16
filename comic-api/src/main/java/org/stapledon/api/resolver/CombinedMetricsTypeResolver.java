package org.stapledon.api.resolver;

import static org.stapledon.common.util.DateTimeUtils.parseDateTime;

import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.api.dto.metrics.AccessMetricsView;
import org.stapledon.api.dto.metrics.ComicAccessMetricView;
import org.stapledon.api.dto.metrics.ComicStorageMetricView;
import org.stapledon.api.dto.metrics.StorageMetricsView;
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
 * Bridges CombinedMetricsData to the GraphQL schema using typed view records.
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
     * Build StorageMetricsView from CombinedMetricsData for CombinedMetrics.storage.
     */
    @SchemaMapping(typeName = "CombinedMetrics", field = "storage")
    public StorageMetricsView storage(CombinedMetricsData data) {
        long totalBytes = 0;
        if (data.getGlobalMetrics() != null) {
            totalBytes = data.getGlobalMetrics().getTotalStorageBytes();
        }

        Map<String, ComicCombinedMetrics> perComic = data.getPerComicMetrics();
        int comicCount = perComic != null ? perComic.size() : 0;

        List<ComicStorageMetricView> comics = new ArrayList<>();
        if (perComic != null) {
            for (Map.Entry<String, ComicCombinedMetrics> entry : perComic.entrySet()) {
                ComicCombinedMetrics m = entry.getValue();
                comics.add(new ComicStorageMetricView(
                        entry.getKey(),
                        (double) m.getStorageBytes(),
                        m.getImageCount(),
                        buildYearlyFromCombined(m)));
            }
        }

        return new StorageMetricsView((double) totalBytes, comicCount, comics, data.getLastUpdated());
    }

    /**
     * Build AccessMetricsView from CombinedMetricsData for CombinedMetrics.access.
     */
    @SchemaMapping(typeName = "CombinedMetrics", field = "access")
    public AccessMetricsView access(CombinedMetricsData data) {
        Map<String, ComicCombinedMetrics> perComic = data.getPerComicMetrics();
        int totalAccesses = 0;
        List<ComicAccessMetricView> comics = new ArrayList<>();

        if (perComic != null) {
            for (Map.Entry<String, ComicCombinedMetrics> entry : perComic.entrySet()) {
                ComicCombinedMetrics m = entry.getValue();
                totalAccesses += m.getAccessCount();

                comics.add(new ComicAccessMetricView(
                        entry.getKey(),
                        m.getAccessCount(),
                        m.getAverageAccessTime(),
                        parseDateTime(m.getLastAccess())));
            }
        }

        return new AccessMetricsView(totalAccesses, comics, data.getLastUpdated());
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
