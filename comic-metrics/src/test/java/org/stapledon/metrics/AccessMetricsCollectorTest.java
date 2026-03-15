package org.stapledon.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.stapledon.metrics.collector.AccessMetricsCollector;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.repository.AccessMetricsRepository;

import java.util.List;
import java.util.stream.Stream;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for AccessMetricsCollector.
 */
@ExtendWith(MockitoExtension.class)
class AccessMetricsCollectorTest {

    @Mock
    private AccessMetricsRepository accessMetricsRepository;

    private AccessMetricsCollector collector;

    @BeforeEach
    void setUp() {
        when(accessMetricsRepository.get()).thenReturn(AccessMetricsData.builder().build());
        collector = new AccessMetricsCollector(accessMetricsRepository, 5);
        collector.init();
    }

    // =========================================================================
    // trackAccess accumulation
    // =========================================================================

    @ParameterizedTest(name = "After {0} hits and {1} misses, accessCount={2}, hitRatio={3}")
    @CsvSource({
            "1, 0, 1, 1.0",
            "0, 1, 1, 0.0",
            "3, 2, 5, 0.6",
            "5, 0, 5, 1.0",
            "0, 3, 3, 0.0"
    })
    void trackAccess_accumulates(int hits, int misses, int expectedCount, double expectedHitRatio) {
        for (int i = 0; i < hits; i++) {
            collector.trackAccess("TestComic", true, 100L);
        }
        for (int i = 0; i < misses; i++) {
            collector.trackAccess("TestComic", false, 200L);
        }

        assertThat(collector.getAccessCounts().get("TestComic")).isEqualTo(expectedCount);
        assertThat(collector.getHitRatios().get("TestComic")).isCloseTo(expectedHitRatio, org.assertj.core.data.Offset.offset(0.001));
    }

    // =========================================================================
    // Multi-comic isolation
    // =========================================================================

    record MultiComicCase(String label, List<String> comics, List<Integer> expectedCounts) {
    }

    static Stream<MultiComicCase> multiComicCases() {
        return Stream.of(
                new MultiComicCase("Two comics tracked separately",
                        List.of("Garfield", "Garfield", "Dilbert"),
                        List.of(2, 1)),
                new MultiComicCase("Three distinct comics",
                        List.of("A", "B", "C"),
                        List.of(1, 1, 1))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("multiComicCases")
    void trackAccess_isolatesComics(MultiComicCase tc) {
        for (String comic : tc.comics) {
            collector.trackAccess(comic, true, 50L);
        }

        var counts = collector.getAccessCounts();
        var distinctComics = tc.comics.stream().distinct().toList();
        for (int i = 0; i < distinctComics.size(); i++) {
            assertThat(counts.get(distinctComics.get(i))).isEqualTo(tc.expectedCounts.get(i));
        }
    }

    // =========================================================================
    // Average access time
    // =========================================================================

    @Test
    void trackAccess_calculatesAverageAccessTime() {
        collector.trackAccess("TestComic", true, 100L);
        collector.trackAccess("TestComic", true, 300L);

        assertThat(collector.getAverageAccessTimes().get("TestComic")).isEqualTo(200.0);
    }

    // =========================================================================
    // Last access time
    // =========================================================================

    @Test
    void trackAccess_recordsLastAccessTime() {
        collector.trackAccess("TestComic", true, 50L);

        assertThat(collector.getLastAccessTimes().get("TestComic")).isNotNull();
    }

    // =========================================================================
    // Persistence threshold
    // =========================================================================

    @Test
    void trackAccess_persistsAtThreshold() {
        // threshold is 5
        for (int i = 0; i < 4; i++) {
            collector.trackAccess("TestComic", true, 10L);
        }
        verify(accessMetricsRepository, never()).save(any());

        collector.trackAccess("TestComic", true, 10L);
        verify(accessMetricsRepository, times(1)).save(any());
    }

    @Test
    void trackAccess_persistsAgainAfterThresholdResets() {
        // Hit threshold twice (5 + 5 = 10 calls)
        for (int i = 0; i < 10; i++) {
            collector.trackAccess("TestComic", true, 10L);
        }
        verify(accessMetricsRepository, times(2)).save(any());
    }

    // =========================================================================
    // Constructor validation
    // =========================================================================

    @Test
    void constructor_rejectsNullRepository() {
        assertThatThrownBy(() -> new AccessMetricsCollector(null, 50))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("accessMetricsRepository");
    }
}
