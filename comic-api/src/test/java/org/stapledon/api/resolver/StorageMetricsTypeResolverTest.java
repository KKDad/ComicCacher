package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.stapledon.common.dto.ComicStorageMetrics;
import org.stapledon.common.dto.ImageCacheStats;

import java.time.OffsetDateTime;
import java.util.Collections;
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
                new TotalBytesCase("Map with totalBytes",
                        Map.of("totalBytes", 123.4),
                        123.4),
                new TotalBytesCase("Map without totalBytes",
                        Map.of("other", "value"),
                        0.0),
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
                new ComicCountCase("Map with comicCount",
                        Map.of("comicCount", 7),
                        7),
                new ComicCountCase("Map without comicCount",
                        Map.of("other", "value"),
                        0),
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

        var preBuiltList = List.of(Map.<String, Object>of("comicName", "Dilbert"));

        return Stream.of(
                new ComicsCase("ImageCacheStats with perComicMetrics",
                        ImageCacheStats.builder().perComicMetrics(metrics).build(),
                        1),
                new ComicsCase("ImageCacheStats with null perComicMetrics",
                        ImageCacheStats.builder().build(),
                        0),
                new ComicsCase("Map with comics list",
                        Map.of("comics", preBuiltList),
                        1),
                new ComicsCase("Map without comics",
                        Map.of("other", "value"),
                        0),
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

    void comics_fromImageCacheStats_containsExpectedFields() {
        var metrics = new LinkedHashMap<String, ComicStorageMetrics>();
        metrics.put("Garfield", ComicStorageMetrics.builder()
                .storageBytes(2000L).imageCount(20)
                .storageByYear(Map.of("2023", 1500L)).build());

        var result = resolver.comics(ImageCacheStats.builder().perComicMetrics(metrics).build());
        assertThat(result).hasSize(1);
        var comic = result.get(0);
        assertThat(comic.get("comicName")).isEqualTo("Garfield");
        assertThat(comic.get("totalBytes")).isEqualTo(2000.0);
        assertThat(comic.get("imageCount")).isEqualTo(20);
        assertThat(comic.get("_storageByYear")).isEqualTo(Map.of("2023", 1500L));
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
                new LastUpdatedCase("Map with _lastUpdated",
                        Map.of("_lastUpdated", now),
                        now),
                new LastUpdatedCase("Map without _lastUpdated",
                        Map.of("other", "value"),
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
