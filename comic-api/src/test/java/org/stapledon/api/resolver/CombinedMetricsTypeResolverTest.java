package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.stapledon.api.dto.metrics.ComicAccessMetricView;
import org.stapledon.api.dto.metrics.ComicStorageMetricView;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.metrics.dto.CombinedMetricsData.ComicCombinedMetrics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for CombinedMetricsTypeResolver.
 */
class CombinedMetricsTypeResolverTest {

    private CombinedMetricsTypeResolver resolver;
    private CombinedMetricsData data;

    @BeforeEach
    void setUp() {
        ManagementFacade facade = mock(ManagementFacade.class);
        when(facade.getAllComics()).thenReturn(List.of(
                ComicItem.builder().id(7).name("Mother Goose & Grimm").build(),
                ComicItem.builder().id(9).name("Garfield").build()));
        resolver = new CombinedMetricsTypeResolver(facade);

        Map<String, ComicCombinedMetrics> perComic = new LinkedHashMap<>();
        perComic.put("MotherGoose&Grimm", ComicCombinedMetrics.builder().comicName("Mother Goose & Grimm").imageCount(149).accessCount(6).build());
        perComic.put("Garfield", ComicCombinedMetrics.builder().comicName("Garfield").imageCount(3).build());
        // No configured comic: kept with a null id
        perComic.put("Retired Strip", ComicCombinedMetrics.builder().comicName("Retired Strip").accessCount(1).build());
        data = CombinedMetricsData.builder().perComicMetrics(perComic).build();
    }

    @Test
    void storageEntriesCarryTheComicIdAndDisplayName() {
        List<ComicStorageMetricView> comics = resolver.storage(data).comics();

        assertThat(comics).extracting(ComicStorageMetricView::comicId, ComicStorageMetricView::comicName, ComicStorageMetricView::imageCount)
                .containsExactly(
                        tuple(7, "Mother Goose & Grimm", 149),
                        tuple(9, "Garfield", 3),
                        tuple(null, "Retired Strip", 0));
    }

    @Test
    void accessEntriesCarryTheComicIdAndDisplayName() {
        List<ComicAccessMetricView> comics = resolver.access(data).comics();

        assertThat(comics).extracting(ComicAccessMetricView::comicId, ComicAccessMetricView::comicName, ComicAccessMetricView::accessCount)
                .containsExactly(
                        tuple(7, "Mother Goose & Grimm", 6),
                        tuple(9, "Garfield", 0),
                        tuple(null, "Retired Strip", 1));
    }
}
