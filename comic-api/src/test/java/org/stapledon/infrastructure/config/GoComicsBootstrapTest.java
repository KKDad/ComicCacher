package org.stapledon.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.stapledon.common.config.IComicsBootstrap;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Unit tests for GoComicsBootstrap with new features:
 * - sourceIdentifier override
 * - publicationDays schedule
 * - active status flag
 */
class GoComicsBootstrapTest {

    @Test
    void sourceIdentifierAutoGeneration() {
        // Given: A comic with no explicit sourceIdentifier
        GoComicsBootstrap bootstrap = GoComicsBootstrap.builder()
                .name("TheDuplex")
                .startDate(LocalDate.of(2019, 4, 1))
                .build();

        // When: Getting the source identifier
        String identifier = bootstrap.getSourceIdentifier();

        // Then: It should be auto-generated (lowercase, no spaces)
        assertThat(identifier).isEqualTo("theduplex");
    }

    @Test
    void sourceIdentifierExplicitOverride() {
        // Given: A comic with explicit sourceIdentifier
        GoComicsBootstrap bootstrap = GoComicsBootstrap.builder()
                .name("TheDuplex")
                .sourceIdentifier("duplex")
                .startDate(LocalDate.of(2019, 4, 1))
                .build();

        // When: Getting the source identifier
        String identifier = bootstrap.getSourceIdentifier();

        // Then: It should use the explicit value
        assertThat(identifier).isEqualTo("duplex");
    }

    @Test
    void sourceIdentifierWithSpaces() {
        // Given: A comic name with spaces and no explicit identifier
        GoComicsBootstrap bootstrap = GoComicsBootstrap.builder()
                .name("Mother Goose & Grimm")
                .startDate(LocalDate.of(2019, 4, 1))
                .build();

        // When: Getting the source identifier
        String identifier = bootstrap.getSourceIdentifier();

        // Then: Spaces should be removed and lowercase
        assertThat(identifier).isEqualTo("mothergoose&grimm");
    }

    @Test
    void sourceIdentifierOverrideWithSpaces() {
        // Given: A comic with explicit sourceIdentifier containing hyphens
        GoComicsBootstrap bootstrap = GoComicsBootstrap.builder()
                .name("Mother Goose & Grimm")
                .sourceIdentifier("mother-goose-and-grimm")
                .startDate(LocalDate.of(2019, 4, 1))
                .build();

        // When: Getting the source identifier
        String identifier = bootstrap.getSourceIdentifier();

        // Then: It should use the explicit value
        assertThat(identifier).isEqualTo("mother-goose-and-grimm");
    }

    @Test
    void publicationDaysNull() {
        // Given: A comic with no publicationDays specified
        GoComicsBootstrap bootstrap = GoComicsBootstrap.builder()
                .name("Garfield")
                .startDate(LocalDate.of(2019, 4, 1))
                .build();

        // When: Getting publication days
        List<DayOfWeek> days = bootstrap.getPublicationDays();

        // Then: Should return null (meaning daily publication)
        assertThat(days).isNull();
    }

    @Test
    void publicationDaysSundayOnly() {
        // Given: A comic that publishes only on Sundays
        GoComicsBootstrap bootstrap = GoComicsBootstrap.builder()
                .name("FoxTrot")
                .publicationDays(List.of(DayOfWeek.SUNDAY))
                .startDate(LocalDate.of(2019, 4, 1))
                .build();

        // When: Getting publication days
        List<DayOfWeek> days = bootstrap.getPublicationDays();

        // Then: Should return Sunday only
        assertThat(days).containsExactly(DayOfWeek.SUNDAY);
    }

    @Test
    void publicationDaysWeekdaysOnly() {
        // Given: A comic that publishes Monday-Friday
        GoComicsBootstrap bootstrap = GoComicsBootstrap.builder()
                .name("Dilbert")
                .publicationDays(List.of(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
                ))
                .startDate(LocalDate.of(2019, 4, 1))
                .build();

        // When: Getting publication days
        List<DayOfWeek> days = bootstrap.getPublicationDays();

        // Then: Should return all weekdays
        assertThat(days).containsExactlyInAnyOrder(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        );
    }

    @Test
    void activeDefaultTrue() {
        // Given: A comic with no active flag specified
        GoComicsBootstrap bootstrap = GoComicsBootstrap.builder()
                .name("Garfield")
                .startDate(LocalDate.of(2019, 4, 1))
                .build();

        // When: Getting active status
        Boolean active = bootstrap.getActive();

        // Then: Should default to true
        assertThat(active).isTrue();
    }

    @Test
    void activeExplicitlyTrue() {
        // Given: A comic explicitly marked as active
        GoComicsBootstrap bootstrap = GoComicsBootstrap.builder()
                .name("Garfield")
                .active(true)
                .startDate(LocalDate.of(2019, 4, 1))
                .build();

        // When: Getting active status
        Boolean active = bootstrap.getActive();

        // Then: Should be true
        assertThat(active).isTrue();
    }

    @Test
    void activeExplicitlyFalse() {
        // Given: A discontinued comic marked as inactive
        GoComicsBootstrap bootstrap = GoComicsBootstrap.builder()
                .name("Committed")
                .active(false)
                .startDate(LocalDate.of(2019, 4, 1))
                .build();

        // When: Getting active status
        Boolean active = bootstrap.getActive();

        // Then: Should be false
        assertThat(active).isFalse();
    }

    @Test
    void interfaceCompatibility() {
        // Given: A GoComicsBootstrap instance
        GoComicsBootstrap bootstrap = GoComicsBootstrap.builder()
                .name("TestComic")
                .sourceIdentifier("test-comic")
                .publicationDays(List.of(DayOfWeek.SUNDAY))
                .active(false)
                .startDate(LocalDate.of(2019, 4, 1))
                .build();

        // When: Treating it as IComicsBootstrap interface
        IComicsBootstrap iBootstrap = bootstrap;

        // Then: All interface methods should work correctly
        assertThat(iBootstrap.stripName()).isEqualTo("TestComic");
        assertThat(iBootstrap.getSource()).isEqualTo("gocomics");
        assertThat(iBootstrap.getSourceIdentifier()).isEqualTo("test-comic");
        assertThat(iBootstrap.getPublicationDays()).containsExactly(DayOfWeek.SUNDAY);
        assertThat(iBootstrap.getActive()).isFalse();
        assertThat(iBootstrap.startDate()).isEqualTo(LocalDate.of(2019, 4, 1));
    }

    @Test
    void combinedFeatures() {
        // Given: A comic using all new features
        GoComicsBootstrap bootstrap = GoComicsBootstrap.builder()
                .name("FoxTrot")
                .sourceIdentifier("foxtrot")  // Explicit override
                .publicationDays(List.of(DayOfWeek.SUNDAY))  // Sunday-only
                .active(true)  // Still active
                .startDate(LocalDate.of(2019, 4, 1))
                .build();

        // When/Then: All features should work together
        assertThat(bootstrap.getSourceIdentifier()).isEqualTo("foxtrot");
        assertThat(bootstrap.getPublicationDays()).containsExactly(DayOfWeek.SUNDAY);
        assertThat(bootstrap.getActive()).isTrue();
    }
}
