package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.stapledon.api.dto.metrics.ComicStorageMetricView;
import org.stapledon.api.dto.metrics.StorageMetricsView;
import org.stapledon.common.dto.ComicStorageMetrics;
import org.stapledon.common.dto.ImageCacheStats;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Tests for StorageMetricsTypeResolver.
 */
class StorageMetricsTypeResolverTest {

    private StorageMetricsTypeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new StorageMetricsTypeResolver();
    }

    // =========================================================================
    // totalBytes
    // =========================================================================

    record TotalBytesCase(String label, Object source, double expected) {
    }

    static Stream<TotalBytesCase> totalBytesCases() {
        return Stream.of(
                new TotalBytesCase("ImageCacheStats",
                        ImageCacheStats.builder().totalStorageBytes(5000L).build(),
                        5000.0),
                new TotalBytesCase("StorageMetricsView",
                        new StorageMetricsView(123.4, 1, List.of(), null),
                        123.4),
                new TotalBytesCase("Unknown type",
                        "unknown",
                        0.0)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("totalBytesCases")
    void totalBytes(TotalBytesCase tc) {
        assertThat(resolver.totalBytes(tc.source)).isEqualTo(tc.expected);
    }

    // =========================================================================
    // comicCount
    // =========================================================================

    record ComicCountCase(String label, Object source, int expected) {
    }

    static Stream<ComicCountCase> comicCountCases() {
        var metrics = new LinkedHashMap<String, ComicStorageMetrics>();
        metrics.put("Garfield", ComicStorageMetrics.builder().build());
        metrics.put("Dilbert", ComicStorageMetrics.builder().build());

        return Stream.of(
                new ComicCountCase("ImageCacheStats with perComicMetrics",
                        ImageCacheStats.builder().perComicMetrics(metrics).build(),
                        2),
                new ComicCountCase("ImageCacheStats with null perComicMetrics",
                        ImageCacheStats.builder().build(),
                        0),
                new ComicCountCase("StorageMetricsView",
                        new StorageMetricsView(0, 7, List.of(), null),
                        7),
                new ComicCountCase("Unknown type",
                        42,
                        0)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("comicCountCases")
    void comicCount(ComicCountCase tc) {
        assertThat(resolver.comicCount(tc.source)).isEqualTo(tc.expected);
    }

    // =========================================================================
    // comics
    // =========================================================================

    record ComicsCase(String label, Object source, int expectedSize) {
    }

    static Stream<ComicsCase> comicsCases() {
        var metrics = new LinkedHashMap<String, ComicStorageMetrics>();
        metrics.put("Garfield", ComicStorageMetrics.builder()
                .storageBytes(1000L).imageCount(10)
                .storageByYear(Map.of("2023", 500L)).build());

        var preBuiltList = List.of(new ComicStorageMetricView("Dilbert", 500.0, 5, Map.of()));

        return Stream.of(
                new ComicsCase("ImageCacheStats with perComicMetrics",
                        ImageCacheStats.builder().perComicMetrics(metrics).build(),
                        1),
                new ComicsCase("ImageCacheStats with null perComicMetrics",
                        ImageCacheStats.builder().build(),
                        0),
                new ComicsCase("StorageMetricsView with comics",
                        new StorageMetricsView(0, 1, preBuiltList, null),
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
                new LastUpdatedCase("ImageCacheStats returns null",
                        ImageCacheStats.builder().build(),
                        null),
                new LastUpdatedCase("StorageMetricsView with lastUpdated",
                        new StorageMetricsView(0, 0, List.of(), now),
                        now),
                new LastUpdatedCase("StorageMetricsView without lastUpdated",
                        new StorageMetricsView(0, 0, List.of(), null),
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
