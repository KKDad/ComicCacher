package org.stapledon.api.resolver;

import static org.stapledon.common.util.DateTimeUtils.parseDateTime;

import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.api.dto.metrics.AccessMetricsView;
import org.stapledon.api.dto.metrics.ComicAccessMetricView;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.dto.AccessMetricsData.ComicAccessMetrics;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Schema mappings for the AccessMetrics GraphQL type.
 * Bridges AccessMetricsData (standalone query) and AccessMetricsView (from CombinedMetrics) to the schema.
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
        if (source instanceof AccessMetricsView view) {
            return view.totalAccesses();
        }
        return 0;
    }

    /**
     * Convert to AccessMetrics.comics List.
     */
    @SchemaMapping(typeName = "AccessMetrics", field = "comics")
    public List<ComicAccessMetricView> comics(Object source) {
        if (source instanceof AccessMetricsData data) {
            return Optional.ofNullable(data.getComicMetrics())
                    .map(AccessMetricsTypeResolver::buildComicsList)
                    .orElse(Collections.emptyList());
        }
        if (source instanceof AccessMetricsView view) {
            return view.comics();
        }
        return Collections.emptyList();
    }

    /**
     * Return lastUpdated for AccessMetrics.
     */
    @SchemaMapping(typeName = "AccessMetrics", field = "lastUpdated")
    public OffsetDateTime lastUpdated(Object source) {
        if (source instanceof AccessMetricsData data) {
            return data.getLastUpdated();
        }
        if (source instanceof AccessMetricsView view) {
            return view.lastUpdated();
        }
        return null;
    }

    private static List<ComicAccessMetricView> buildComicsList(Map<String, ComicAccessMetrics> metrics) {
        List<ComicAccessMetricView> result = new ArrayList<>(metrics.size());
        for (Map.Entry<String, ComicAccessMetrics> entry : metrics.entrySet()) {
            ComicAccessMetrics m = entry.getValue();
            result.add(new ComicAccessMetricView(
                    entry.getKey(),
                    m.getAccessCount(),
                    m.getAverageAccessTime(),
                    parseDateTime(m.getLastAccess())));
        }
        return result;
    }
}
