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

/**
 * Schema mappings for the AccessMetrics GraphQL type.
 * Bridges AccessMetricsData to the GraphQL schema.
 */
@Controller
public class AccessMetricsTypeResolver {

    /**
     * Compute AccessMetrics.totalAccesses by summing all comic access counts.
     */
    @SchemaMapping(typeName = "AccessMetrics", field = "totalAccesses")
    public int totalAccesses(AccessMetricsData data) {
        Map<String, ComicAccessMetrics> metrics = data.getComicMetrics();
        if (metrics == null) {
            return 0;
        }
        return metrics.values().stream().mapToInt(ComicAccessMetrics::getAccessCount).sum();
    }

    /**
     * Convert AccessMetricsData.comicMetrics Map to AccessMetrics.comics List.
     */
    @SchemaMapping(typeName = "AccessMetrics", field = "comics")
    public List<Map<String, Object>> comics(AccessMetricsData data) {
        Map<String, ComicAccessMetrics> metrics = data.getComicMetrics();
        if (metrics == null) {
            return Collections.emptyList();
        }
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

    /**
     * Return lastUpdated for AccessMetrics.
     */
    @SchemaMapping(typeName = "AccessMetrics", field = "lastUpdated")
    public OffsetDateTime lastUpdated(AccessMetricsData data) {
        return data.getLastUpdated();
    }

}
