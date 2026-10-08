package org.stapledon.api.resolver;

import static org.stapledon.common.util.DateTimeUtils.parseDateTime;

import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.api.dto.metrics.AccessMetricsView;
import org.stapledon.api.dto.metrics.ComicAccessMetricView;
import org.stapledon.api.dto.metrics.ComicStorageMetricView;
import org.stapledon.api.dto.metrics.StorageMetricsView;
import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.metrics.dto.CombinedMetricsData.ComicCombinedMetrics;
import org.stapledon.metrics.dto.YearlyStorageMetrics;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Schema mappings for the CombinedMetrics GraphQL type.
 * Bridges CombinedMetricsData to the GraphQL schema using typed view records. Metrics are keyed by directory name; each entry
 * gets the id, source and display name of the configured comic with that directory, so clients can join the two lists by id.
 */
@Controller
@RequiredArgsConstructor
public class CombinedMetricsTypeResolver {

    private final ManagementFacade comicManagementFacade;

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
            Map<String, ComicItem> byDirectory = comicsByDirectory();
            for (Map.Entry<String, ComicCombinedMetrics> entry : perComic.entrySet()) {
                ComicCombinedMetrics m = entry.getValue();
                ComicItem comic = byDirectory.get(directoryKey(entry.getKey()));
                comics.add(new ComicStorageMetricView(
                        comic != null ? comic.getId() : null,
                        comic != null ? comic.getSource() : null,
                        displayName(entry.getKey(), m, comic),
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
            Map<String, ComicItem> byDirectory = comicsByDirectory();
            for (Map.Entry<String, ComicCombinedMetrics> entry : perComic.entrySet()) {
                ComicCombinedMetrics m = entry.getValue();
                totalAccesses += m.getAccessCount();

                ComicItem comic = byDirectory.get(directoryKey(entry.getKey()));
                comics.add(new ComicAccessMetricView(
                        comic != null ? comic.getId() : null,
                        comic != null ? comic.getSource() : null,
                        displayName(entry.getKey(), m, comic),
                        m.getAccessCount(),
                        m.getAverageAccessTime(),
                        parseDateTime(m.getLastAccess())));
            }
        }

        return new AccessMetricsView(totalAccesses, comics, data.getLastUpdated());
    }

    private Map<String, ComicItem> comicsByDirectory() {
        return comicManagementFacade.getAllComics().stream()
                .collect(Collectors.toMap(c -> ComicIdentifier.from(c).getDirectoryName(), Function.identity(), (a, _) -> a));
    }

    /**
     * Older snapshots in metrics-history may still key access entries by display name, so look them up without spaces too.
     */
    private static String directoryKey(String key) {
        return key.replace(" ", "");
    }

    private static String displayName(String key, ComicCombinedMetrics m, ComicItem comic) {
        if (comic != null && comic.getName() != null) {
            return comic.getName();
        }
        return m.getComicName() != null && !m.getComicName().isBlank() ? m.getComicName() : key;
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
