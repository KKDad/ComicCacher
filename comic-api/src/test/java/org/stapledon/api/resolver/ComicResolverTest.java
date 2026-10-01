package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.stapledon.api.dto.ErrorCode;
import org.stapledon.api.dto.payload.MutationPayloads.CreateComicPayload;
import org.stapledon.api.dto.payload.MutationPayloads.UpdateComicPayload;
import org.stapledon.api.resolver.ComicResolver.CreateComicInput;
import org.stapledon.api.resolver.ComicResolver.UpdateComicInput;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.dto.StartSource;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.engine.source.ComicValidator;
import org.stapledon.engine.source.ComicValidator.Problem;
import org.stapledon.metrics.collector.AccessMetricsCollector;

import java.time.DayOfWeek;
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

    @Mock
    private ComicValidator validator;

    private ComicResolver resolver;

    private ComicItem testComic;
    private final LocalDate testDate = LocalDate.of(2026, 3, 15);

    @BeforeEach
    void setUp() {
        resolver = new ComicResolver(managementFacade, accessMetricsCollector, new ComicVisibility(), validator, "http://localhost:8087");
        testComic = ComicItem.builder()
                .id(1)
                .name("Test Comic")
                .newest(testDate)
                .oldest(testDate.minusDays(30))
                .enabled(true)
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void signInAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("someone", null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private static CreateComicInput createInput(String name) {
        return new CreateComicInput(name, "Author", "About", null, null, "gocomics", "slug", List.of(DayOfWeek.SUNDAY), null, null, null);
    }

    private static UpdateComicInput emptyUpdate() {
        return new UpdateComicInput(null, null, null, null, null, null, null, null, null, null, null);
    }

    // =========================================================================
    // randomStrip
    // =========================================================================

    @Nested
    class RandomStripTests {

        @Test
        void returnsStripFromSpecificComic() {
            when(managementFacade.getComic(1)).thenReturn(Optional.of(testComic));
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
            when(managementFacade.getComic(1)).thenReturn(Optional.of(testComic));
            when(managementFacade.getRandomDate(1)).thenReturn(Optional.empty());

            ComicResolver.ComicStrip result = resolver.randomStrip(1);

            assertThat(result).isNull();
        }

        @Test
        void returnsNullForUnknownComic() {
            ComicResolver.ComicStrip result = resolver.randomStrip(999);

            assertThat(result).isNull();
        }

        @Test
        void returnsNullForHiddenComicWhenNotAdmin() {
            signInAs("USER");
            when(managementFacade.getComic(1)).thenReturn(Optional.of(testComic.toBuilder().enabled(false).build()));

            assertThat(resolver.randomStrip(1)).isNull();
            verify(managementFacade, never()).getRandomDate(anyInt());
        }

        @Test
        void neverPicksHiddenComicAtRandom() {
            signInAs("USER");
            when(managementFacade.getAllComics()).thenReturn(List.of(testComic.toBuilder().enabled(false).build()));

            assertThat(resolver.randomStrip(null)).isNull();
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
            when(managementFacade.getComic(1)).thenReturn(Optional.of(testComic));
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

    // =========================================================================
    // Visibility
    // =========================================================================

    @Nested
    class VisibilityTests {

        private final ComicItem hidden = ComicItem.builder().id(2).name("Hidden Comic").enabled(false).active(true).build();
        private final ComicItem inactive = ComicItem.builder().id(3).name("Inactive Comic").enabled(true).active(false).build();

        @Test
        void comicsLeavesOutHiddenComicsForReaders() {
            signInAs("USER");
            when(managementFacade.getAllComics()).thenReturn(List.of(testComic, hidden));

            ComicResolver.ComicConnection result = resolver.comics(null, null, null, true, 20, null);

            assertThat(result.edges()).extracting(edge -> edge.node().getId()).containsExactly(1);
        }

        @Test
        void comicsIncludesHiddenComicsForAdminsWhoAsk() {
            signInAs("ADMIN");
            when(managementFacade.getAllComics()).thenReturn(List.of(testComic, hidden));

            assertThat(resolver.comics(null, null, null, true, 20, null).totalCount()).isEqualTo(2);
            assertThat(resolver.comics(null, null, null, false, 20, null).totalCount()).isEqualTo(1);
        }

        @Test
        void comicsFiltersOnActive() {
            when(managementFacade.getAllComics()).thenReturn(List.of(testComic, inactive));

            assertThat(resolver.comics(null, false, null, null, 20, null).edges())
                    .extracting(edge -> edge.node().getId()).containsExactly(3);
            assertThat(resolver.comics(null, true, null, null, 20, null).edges())
                    .extracting(edge -> edge.node().getId()).containsExactly(1);
        }

        @Test
        void comicReturnsNullForHiddenComicUnlessAdmin() {
            when(managementFacade.getComic(2)).thenReturn(Optional.of(hidden));

            signInAs("OPERATOR");
            assertThat(resolver.comic(2)).isNull();

            signInAs("ADMIN");
            assertThat(resolver.comic(2)).isEqualTo(hidden);
        }

        @Test
        void searchLeavesOutHiddenComics() {
            signInAs("USER");
            when(managementFacade.getAllComics()).thenReturn(List.of(testComic, hidden));

            assertThat(resolver.search("comic", 20).comics()).containsExactly(testComic);
        }
    }

    // =========================================================================
    // createComic / updateComic
    // =========================================================================

    @Nested
    class MutationTests {

        @Test
        void createComicPassesEveryInputField() {
            when(validator.validateNew(any())).thenReturn(List.of());
            when(managementFacade.createComic(any())).thenAnswer(inv -> Optional.of(((ComicItem) inv.getArgument(0)).toBuilder().id(7).build()));
            CreateComicInput input = new CreateComicInput("Freefall", "Mark Stanley", "Space", false, false, "freefall", "freefall",
                    List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), 1, 4000, null);

            CreateComicPayload payload = resolver.createComic(input);

            ArgumentCaptor<ComicItem> saved = ArgumentCaptor.forClass(ComicItem.class);
            verify(managementFacade).createComic(saved.capture());
            ComicItem comic = saved.getValue();
            assertThat(comic.getId()).isZero();
            assertThat(comic.getName()).isEqualTo("Freefall");
            assertThat(comic.getAuthor()).isEqualTo("Mark Stanley");
            assertThat(comic.getDescription()).isEqualTo("Space");
            assertThat(comic.isEnabled()).isFalse();
            assertThat(comic.isActive()).isFalse();
            assertThat(comic.getSource()).isEqualTo("freefall");
            assertThat(comic.getSourceIdentifier()).isEqualTo("freefall");
            assertThat(comic.getPublicationDays()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
            assertThat(comic.getFirstStripNumber()).isEqualTo(1);
            assertThat(comic.getLastStripNumber()).isEqualTo(4000);
            assertThat(comic.getStartSource()).isEqualTo(StartSource.MANUAL);
            assertThat(payload.comic().getId()).isEqualTo(7);
            assertThat(payload.errors()).isEmpty();
        }

        @Test
        void createComicDefaultsToEnabledActiveAndNoStart() {
            when(validator.validateNew(any())).thenReturn(List.of());
            when(managementFacade.createComic(any())).thenAnswer(inv -> Optional.of(inv.getArgument(0)));

            ComicItem comic = resolver.createComic(createInput("New Comic")).comic();

            assertThat(comic.isEnabled()).isTrue();
            assertThat(comic.isActive()).isTrue();
            assertThat(comic.getStartSource()).isNull();
        }

        @Test
        void createComicReportsValidationProblemsWithoutSaving() {
            when(validator.validateNew(any())).thenReturn(List.of(new Problem("sourceIdentifier", "bad slug")));

            CreateComicPayload payload = resolver.createComic(createInput("New Comic"));

            assertThat(payload.comic()).isNull();
            assertThat(payload.errors()).singleElement().satisfies(error -> {
                assertThat(error.field()).isEqualTo("input.sourceIdentifier");
                assertThat(error.code()).isEqualTo(ErrorCode.VALIDATION_ERROR);
            });
            verify(managementFacade, never()).createComic(any());
        }

        @Test
        void updateComicAppliesOnlyGivenFields() {
            when(managementFacade.getComic(1)).thenReturn(Optional.of(testComic.toBuilder().author("Kept").build()));
            when(validator.validateUpdate(any(), any())).thenReturn(List.of());
            when(managementFacade.updateComic(eq(1), any())).thenAnswer(inv -> Optional.of(inv.getArgument(1)));
            UpdateComicInput input = new UpdateComicInput(null, null, null, false, false, null, null, List.of(DayOfWeek.SUNDAY), null, null,
                    LocalDate.of(1985, 11, 18));

            UpdateComicPayload payload = resolver.updateComic(1, input);

            ComicItem comic = payload.comic();
            assertThat(comic.getAuthor()).isEqualTo("Kept");
            assertThat(comic.getName()).isEqualTo("Test Comic");
            assertThat(comic.isEnabled()).isFalse();
            assertThat(comic.isActive()).isFalse();
            assertThat(comic.getPublicationDays()).containsExactly(DayOfWeek.SUNDAY);
            assertThat(comic.getSourceStartDate()).isEqualTo(LocalDate.of(1985, 11, 18));
            assertThat(comic.getStartSource()).isEqualTo(StartSource.MANUAL);
        }

        @Test
        void updateComicWithoutStartKeepsItsOrigin() {
            when(managementFacade.getComic(1)).thenReturn(Optional.of(testComic.toBuilder().startSource(StartSource.DETECTED).build()));
            when(validator.validateUpdate(any(), any())).thenReturn(List.of());
            when(managementFacade.updateComic(eq(1), any())).thenAnswer(inv -> Optional.of(inv.getArgument(1)));

            ComicItem comic = resolver.updateComic(1, emptyUpdate()).comic();

            assertThat(comic.getStartSource()).isEqualTo(StartSource.DETECTED);
        }

        @Test
        void updateComicReportsValidationProblemsWithoutSaving() {
            when(managementFacade.getComic(1)).thenReturn(Optional.of(testComic));
            when(validator.validateUpdate(any(), any())).thenReturn(List.of(new Problem("name", "taken")));

            UpdateComicPayload payload = resolver.updateComic(1, emptyUpdate());

            assertThat(payload.comic()).isNull();
            assertThat(payload.errors()).extracting(error -> error.field()).containsExactly("input.name");
            verify(managementFacade, never()).updateComic(anyInt(), any());
        }
    }
}
