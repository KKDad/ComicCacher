package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.metrics.collector.AccessMetricsCollector;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Tests for ComicResolver query and schema mapping methods.
 */
@ExtendWith(MockitoExtension.class)
class ComicResolverTest {

    @Mock
    private ManagementFacade managementFacade;

    @Mock
    private AccessMetricsCollector accessMetricsCollector;

    private ComicResolver resolver;

    private ComicItem testComic;
    private final LocalDate testDate = LocalDate.of(2026, 3, 15);

    @BeforeEach
    void setUp() {
        resolver = new ComicResolver(managementFacade, accessMetricsCollector, "http://localhost:8087");
        testComic = ComicItem.builder()
                .id(1)
                .name("Test Comic")
                .newest(testDate)
                .oldest(testDate.minusDays(30))
                .enabled(true)
                .build();
    }

    // =========================================================================
    // randomStrip
    // =========================================================================

    @Nested
    class RandomStripTests {

        @Test
        void returnsStripFromSpecificComic() {
            when(managementFacade.getRandomDate(1)).thenReturn(Optional.of(testDate));
            when(managementFacade.getComicStripWithNavigation(1, testDate))
                    .thenReturn(ComicNavigationResult.found(
                            ImageDto.builder().mimeType("image/png").imageDate(testDate).build(),
                            testDate.minusDays(1), testDate.plusDays(1)));

            ComicResolver.ComicStrip result = resolver.randomStrip(1);

            assertThat(result).isNotNull();
            assertThat(result.available()).isTrue();
            assertThat(result.date()).isEqualTo(testDate);
            assertThat(result.imageUrl()).contains("/api/v1/comics/1/strip/" + testDate);
        }

        @Test
        void returnsNullWhenComicHasNoDates() {
            when(managementFacade.getRandomDate(999)).thenReturn(Optional.empty());

            ComicResolver.ComicStrip result = resolver.randomStrip(999);

            assertThat(result).isNull();
        }

        @Test
        void picksRandomComicWhenNoComicIdProvided() {
            when(managementFacade.getAllComics()).thenReturn(List.of(testComic));
            when(managementFacade.getRandomDate(1)).thenReturn(Optional.of(testDate));
            when(managementFacade.getComicStripWithNavigation(1, testDate))
                    .thenReturn(ComicNavigationResult.found(
                            ImageDto.builder().mimeType("image/png").imageDate(testDate).build(),
                            testDate.minusDays(1), null));

            ComicResolver.ComicStrip result = resolver.randomStrip(null);

            assertThat(result).isNotNull();
            assertThat(result.available()).isTrue();
        }

        @Test
        void returnsNullWhenNoComicsExist() {
            when(managementFacade.getAllComics()).thenReturn(List.of());

            ComicResolver.ComicStrip result = resolver.randomStrip(null);

            assertThat(result).isNull();
        }
    }

    // =========================================================================
    // randomStrip — not-found fallback
    // =========================================================================

    @Nested
    class RandomStripNotFoundTests {

        @Test
        void fallsBackToRequestedDateWhenCurrentDateIsNull() {
            LocalDate requestedDate = testDate.plusDays(5);
            when(managementFacade.getRandomDate(1)).thenReturn(Optional.of(requestedDate));
            when(managementFacade.getComicStripWithNavigation(1, requestedDate))
                    .thenReturn(ComicNavigationResult.notFound("NOT_AVAILABLE", requestedDate,
                            testDate.plusDays(4), testDate.plusDays(6)));

            ComicResolver.ComicStrip result = resolver.randomStrip(1);

            assertThat(result).isNotNull();
            assertThat(result.available()).isFalse();
            assertThat(result.date()).isEqualTo(requestedDate);
            assertThat(result.previous()).isNotNull();
            assertThat(result.next()).isNotNull();
        }
    }

    // =========================================================================
    // stripWindow
    // =========================================================================

    @Nested
    class StripWindowTests {

        @Test
        void returnsCorrectWindowInChronologicalOrder() {
            LocalDate day1 = testDate.minusDays(1);
            LocalDate day2 = testDate;
            LocalDate day3 = testDate.plusDays(1);

            ImageDto img = ImageDto.builder().mimeType("image/png").build();
            when(managementFacade.getStripWindow(1, testDate, 1, 1))
                    .thenReturn(List.of(
                            ComicNavigationResult.found(img, null, day2),
                            ComicNavigationResult.found(img, day1, day3),
                            ComicNavigationResult.found(img, day2, null)));

            List<ComicResolver.ComicStrip> result = resolver.stripWindow(testComic, testDate, 1, 1);

            assertThat(result).hasSize(3);
            assertThat(result).allMatch(ComicResolver.ComicStrip::available);
        }

        @ParameterizedTest
        @CsvSource({"0, 0", "10, 10", "20, 20", "25, 20", "50, 20"})
        void capsBeforeAndAfterValues(int requested, int expected) {
            when(managementFacade.getStripWindow(1, testDate, expected, expected))
                    .thenReturn(List.of());

            List<ComicResolver.ComicStrip> result = resolver.stripWindow(testComic, testDate, requested, requested);

            assertThat(result).isEmpty();
        }

        @Test
        void returnsCenterOnlyWhenBeforeAndAfterAreZero() {
            ImageDto img = ImageDto.builder().mimeType("image/png").build();
            when(managementFacade.getStripWindow(1, testDate, 0, 0))
                    .thenReturn(List.of(ComicNavigationResult.found(img, testDate.minusDays(1), testDate.plusDays(1))));

            List<ComicResolver.ComicStrip> result = resolver.stripWindow(testComic, testDate, 0, 0);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().available()).isTrue();
        }

        @Test
        void returnFewerStripsAtBoundary() {
            ImageDto img = ImageDto.builder().mimeType("image/png").build();
            // Only center + 1 after (no before available)
            when(managementFacade.getStripWindow(1, testDate, 5, 5))
                    .thenReturn(List.of(
                            ComicNavigationResult.found(img, null, testDate.plusDays(1)),
                            ComicNavigationResult.found(img, testDate, null)));

            List<ComicResolver.ComicStrip> result = resolver.stripWindow(testComic, testDate, 5, 5);

            assertThat(result).hasSize(2);
        }
    }

    // =========================================================================
    // strips (batch date fetch)
    // =========================================================================

    @Nested
    class StripsTests {

        @Test
        void returnsStripsForRequestedDates() {
            LocalDate date1 = testDate;
            LocalDate date2 = testDate.plusDays(1);
            ImageDto img = ImageDto.builder().mimeType("image/png").build();

            when(managementFacade.getComicStripWithNavigation(1, date1))
                    .thenReturn(ComicNavigationResult.found(img, null, date2));
            when(managementFacade.getComicStripWithNavigation(1, date2))
                    .thenReturn(ComicNavigationResult.found(img, date1, null));

            List<ComicResolver.ComicStrip> result = resolver.strips(testComic, List.of(date1, date2));

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(ComicResolver.ComicStrip::available);
        }

        @Test
        void returnsEmptyForEmptyDateList() {
            List<ComicResolver.ComicStrip> result = resolver.strips(testComic, List.of());

            assertThat(result).isEmpty();
        }

        @Test
        void handlesDuplicateDates() {
            ImageDto img = ImageDto.builder().mimeType("image/png").build();
            when(managementFacade.getComicStripWithNavigation(1, testDate))
                    .thenReturn(ComicNavigationResult.found(img, null, null));

            List<ComicResolver.ComicStrip> result = resolver.strips(testComic, List.of(testDate, testDate));

            assertThat(result).hasSize(2);
        }

        @ParameterizedTest
        @CsvSource({"25, 25", "30, 30", "31, 30"})
        void capsListSizeCorrectly(int requested, int expected) {
            List<LocalDate> dates = IntStream.range(0, requested)
                    .mapToObj(testDate::plusDays)
                    .toList();

            when(managementFacade.getComicStripWithNavigation(anyInt(), org.mockito.ArgumentMatchers.any(LocalDate.class)))
                    .thenReturn(ComicNavigationResult.found(
                            ImageDto.builder().mimeType("image/png").build(), null, null));

            List<ComicResolver.ComicStrip> result = resolver.strips(testComic, dates);

            assertThat(result).hasSize(expected);
        }

        @Test
        void handlesUnavailableDates() {
            when(managementFacade.getComicStripWithNavigation(1, testDate))
                    .thenReturn(ComicNavigationResult.notFound("NOT_AVAILABLE", testDate, null, null));

            List<ComicResolver.ComicStrip> result = resolver.strips(testComic, List.of(testDate));

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().available()).isFalse();
        }
    }
}
