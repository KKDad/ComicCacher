package org.stapledon.api.resolver;

import static org.stapledon.common.util.DateTimeUtils.parseDateTime;

import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.dto.AccessMetricsData.ComicAccessMetrics;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Schema mappings for the AccessMetrics GraphQL type.
 * Bridges AccessMetricsData (standalone query) and Map (from CombinedMetrics) to the schema.
 */
@Controller
public class AccessMetricsTypeResolver {

    /**
     * Compute AccessMetrics.totalAccesses by summing all comic access counts.
     */
    @SchemaMapping(typeName = "AccessMetrics", field = "totalAccesses")
    public int totalAccesses(Object source) {
        if (source instanceof AccessMetricsData data) {
            return Optional.ofNullable(data.getComicMetrics())
                    .map(m -> m.values().stream().mapToInt(ComicAccessMetrics::getAccessCount).sum())
                    .orElse(0);
        }
        return Optional.ofNullable(source)
                .filter(Map.class::isInstance)
                .map(s -> ((Map<?, ?>) s).get("totalAccesses"))
                .map(v -> ((Number) v).intValue())
                .orElse(0);
    }

    /**
     * Convert AccessMetricsData.comicMetrics Map to AccessMetrics.comics List.
     */
    @SuppressWarnings("unchecked")
    @SchemaMapping(typeName = "AccessMetrics", field = "comics")
    public List<Map<String, Object>> comics(Object source) {
        if (source instanceof AccessMetricsData data) {
            return Optional.ofNullable(data.getComicMetrics())
                    .map(AccessMetricsTypeResolver::buildComicsList)
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
     * Return lastUpdated for AccessMetrics.
     */
    @SchemaMapping(typeName = "AccessMetrics", field = "lastUpdated")
    public OffsetDateTime lastUpdated(Object source) {
        if (source instanceof AccessMetricsData data) {
            return data.getLastUpdated();
        }
        return Optional.ofNullable(source)
                .filter(Map.class::isInstance)
                .map(s -> ((Map<?, ?>) s).get("lastUpdated"))
                .filter(OffsetDateTime.class::isInstance)
                .map(OffsetDateTime.class::cast)
                .orElse(null);
    }

    private static List<Map<String, Object>> buildComicsList(Map<String, ComicAccessMetrics> metrics) {
        List<Map<String, Object>> result = new ArrayList<>(metrics.size());
        for (Map.Entry<String, ComicAccessMetrics> entry : metrics.entrySet()) {
            ComicAccessMetrics m = entry.getValue();
            Map<String, Object> comic = new LinkedHashMap<>();
            comic.put("comicName", entry.getKey());
            comic.put("accessCount", m.getAccessCount());
            comic.put("averageAccessTimeMs", m.getAverageAccessTime());
            comic.put("lastAccessed", parseDateTime(m.getLastAccess()));
            result.add(comic);
        }
        return result;
    }
}
