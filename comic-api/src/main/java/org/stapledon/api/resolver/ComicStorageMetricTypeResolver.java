package org.stapledon.api.resolver;

import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.api.dto.metrics.ComicStorageMetricView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schema mappings for the ComicStorageMetric GraphQL type.
 * Converts internal storageByYear Map to the yearlyBreakdown list.
 */
@Controller
public class ComicStorageMetricTypeResolver {

    /**
     * Convert storageByYear Map to yearlyBreakdown List.
     */
    @SchemaMapping(typeName = "ComicStorageMetric", field = "yearlyBreakdown")
    public List<Map<String, Object>> yearlyBreakdown(ComicStorageMetricView comic) {
        Map<String, Long> storageByYear = comic.storageByYear();
        if (storageByYear == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>(storageByYear.size());
        for (Map.Entry<String, Long> entry : storageByYear.entrySet()) {
            Map<String, Object> yearly = new LinkedHashMap<>();
            try {
                yearly.put("year", Integer.parseInt(entry.getKey()));
            } catch (NumberFormatException e) {
                yearly.put("year", 0);
            }
            yearly.put("bytes", (double) entry.getValue());
            yearly.put("imageCount", 0);
            result.add(yearly);
        }
        return result;
    }
}
