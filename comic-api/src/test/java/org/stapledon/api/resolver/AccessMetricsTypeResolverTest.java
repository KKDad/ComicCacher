package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.stapledon.api.dto.metrics.AccessMetricsView;
import org.stapledon.api.dto.metrics.ComicAccessMetricView;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.dto.AccessMetricsData.ComicAccessMetrics;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

/**
 * Tests for AccessMetricsTypeResolver.
 */
class AccessMetricsTypeResolverTest {

    private AccessMetricsTypeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AccessMetricsTypeResolver();
    }

    // =========================================================================
    // totalAccesses
    // =========================================================================

    record TotalAccessesCase(String label, Object source, int expected) {
    }

    static Stream<TotalAccessesCase> totalAccessesCases() {
        var metrics = new LinkedHashMap<String, ComicAccessMetrics>();
        metrics.put("Garfield", ComicAccessMetrics.builder().accessCount(10).build());
        metrics.put("Dilbert", ComicAccessMetrics.builder().accessCount(25).build());

        return Stream.of(
                new TotalAccessesCase("AccessMetricsData sums access counts",
                        AccessMetricsData.builder().comicMetrics(metrics).build(),
                        35),
                new TotalAccessesCase("AccessMetricsData with null comicMetrics",
                        AccessMetricsData.builder().comicMetrics(null).build(),
                        0),
                new TotalAccessesCase("AccessMetricsView",
                        new AccessMetricsView(42, List.of(), null),
                        42),
                new TotalAccessesCase("Unknown type",
                        "unknown",
                        0)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("totalAccessesCases")
    void totalAccesses(TotalAccessesCase tc) {
        assertThat(resolver.totalAccesses(tc.source)).isEqualTo(tc.expected);
    }

    // =========================================================================
    // comics
    // =========================================================================

    record ComicsCase(String label, Object source, int expectedSize) {
    }

    static Stream<ComicsCase> comicsCases() {
        var metrics = new LinkedHashMap<String, ComicAccessMetrics>();
        metrics.put("Garfield", ComicAccessMetrics.builder()
                .accessCount(5).lastAccess("2024-01-15T10:30:00Z")
                .totalAccessTimeMs(500L).build());
        metrics.put("Dilbert", ComicAccessMetrics.builder()
                .accessCount(3).lastAccess("2024-01-14T09:00:00Z")
                .totalAccessTimeMs(300L).build());

        var preBuiltList = List.of(new ComicAccessMetricView("Calvin", 1, 10.0, null));

        return Stream.of(
                new ComicsCase("AccessMetricsData with comicMetrics",
                        AccessMetricsData.builder().comicMetrics(metrics).build(),
                        2),
                new ComicsCase("AccessMetricsData with null comicMetrics",
                        AccessMetricsData.builder().comicMetrics(null).build(),
                        0),
                new ComicsCase("AccessMetricsView with comics",
                        new AccessMetricsView(1, preBuiltList, null),
                        1),
                new ComicsCase("Unknown type",
                        "unknown",
                        0)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("comicsCases")
    void comics(ComicsCase tc) {
        var result = resolver.comics(tc.source);
        assertThat(result).hasSize(tc.expectedSize);
    }

    // =========================================================================
    // lastUpdated
    // =========================================================================

    record LastUpdatedCase(String label, Object source, OffsetDateTime expected) {
    }

    static Stream<LastUpdatedCase> lastUpdatedCases() {
        var now = OffsetDateTime.now();

        return Stream.of(
                new LastUpdatedCase("AccessMetricsData returns lastUpdated",
                        AccessMetricsData.builder().lastUpdated(now).build(),
                        now),
                new LastUpdatedCase("AccessMetricsData with null lastUpdated",
                        AccessMetricsData.builder().build(),
                        null),
                new LastUpdatedCase("AccessMetricsView with lastUpdated",
                        new AccessMetricsView(0, List.of(), now),
                        now),
                new LastUpdatedCase("AccessMetricsView without lastUpdated",
                        new AccessMetricsView(0, List.of(), null),
                        null),
                new LastUpdatedCase("Unknown type",
                        "unknown",
                        null)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lastUpdatedCases")
    void lastUpdated(LastUpdatedCase tc) {
        assertThat(resolver.lastUpdated(tc.source)).isEqualTo(tc.expected);
    }
}
