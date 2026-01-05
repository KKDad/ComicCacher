package org.stapledon.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.stapledon.common.config.IComicsBootstrap;
import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.dto.ComicItem;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Unit tests for Bootstrap conversion to ComicConfig
 * Tests that new fields (sourceIdentifier override, publicationDays, active) are properly copied
 */
class BootstrapTest {

    @Test
    void convertBootstrapToComicItemBasicFields() {
        // Given: A test bootstrap with basic fields
        TestBootstrap bootstrap = new TestBootstrap(
            "TestComic",
            LocalDate.of(2019, 4, 1),
            "gocomics",
            "test-comic",
            null,
            true
        );

        Bootstrap config = Bootstrap.builder()
                .dailyComics(List.of(bootstrap))
                .build();

        // When: Converting to ComicConfig
        ComicConfig comicConfig = config.getComicConfig();

        // Then: Basic fields should be copied
        assertThat(comicConfig.getComics()).hasSize(1);
        ComicItem comic = comicConfig.getComics().get(0);
        assertThat(comic.getName()).isEqualTo("TestComic");
        assertThat(comic.getSource()).isEqualTo("gocomics");
        assertThat(comic.getSourceIdentifier()).isEqualTo("test-comic");
        assertThat(comic.getOldest()).isEqualTo(LocalDate.of(2019, 4, 1));
        assertThat(comic.isEnabled()).isTrue();
    }

    @Test
    void convertBootstrapWithPublicationDays() {
        // Given: A bootstrap with Sunday-only publication
        TestBootstrap bootstrap = new TestBootstrap(
            "FoxTrot",
            LocalDate.of(2019, 4, 1),
            "gocomics",
            "foxtrot",
            List.of(DayOfWeek.SUNDAY),
            true
        );

        Bootstrap config = Bootstrap.builder()
                .dailyComics(List.of(bootstrap))
                .build();

        // When: Converting to ComicConfig
        ComicConfig comicConfig = config.getComicConfig();

        // Then: publicationDays should be copied
        ComicItem comic = comicConfig.getComics().get(0);
        assertThat(comic.getPublicationDays()).isNotNull();
        assertThat(comic.getPublicationDays()).containsExactly(DayOfWeek.SUNDAY);
    }

    @Test
    void convertBootstrapWithNullPublicationDays() {
        // Given: A bootstrap with null publicationDays (daily publication)
        TestBootstrap bootstrap = new TestBootstrap(
            "Garfield",
            LocalDate.of(2019, 4, 1),
            "gocomics",
            "garfield",
            null,
            true
        );

        Bootstrap config = Bootstrap.builder()
                .dailyComics(List.of(bootstrap))
                .build();

        // When: Converting to ComicConfig
        ComicConfig comicConfig = config.getComicConfig();

        // Then: publicationDays should be null (meaning daily)
        ComicItem comic = comicConfig.getComics().get(0);
        assertThat(comic.getPublicationDays()).isNull();
    }

    @Test
    void convertBootstrapWithActiveTrue() {
        // Given: An active comic
        TestBootstrap bootstrap = new TestBootstrap(
            "Garfield",
            LocalDate.of(2019, 4, 1),
            "gocomics",
            "garfield",
            null,
            true
        );

        Bootstrap config = Bootstrap.builder()
                .dailyComics(List.of(bootstrap))
                .build();

        // When: Converting to ComicConfig
        ComicConfig comicConfig = config.getComicConfig();

        // Then: active should be true
        ComicItem comic = comicConfig.getComics().get(0);
        assertThat(comic.getActive()).isTrue();
        assertThat(comic.isActive()).isTrue();
    }

    @Test
    void convertBootstrapWithActiveFalse() {
        // Given: An inactive comic
        TestBootstrap bootstrap = new TestBootstrap(
            "Committed",
            LocalDate.of(2019, 4, 1),
            "gocomics",
            "committed",
            null,
            false
        );

        Bootstrap config = Bootstrap.builder()
                .dailyComics(List.of(bootstrap))
                .build();

        // When: Converting to ComicConfig
        ComicConfig comicConfig = config.getComicConfig();

        // Then: active should be false
        ComicItem comic = comicConfig.getComics().get(0);
        assertThat(comic.getActive()).isFalse();
        assertThat(comic.isActive()).isFalse();
    }

    @Test
    void convertBootstrapWithNullActive() {
        // Given: A comic with null active (should default to true)
        TestBootstrap bootstrap = new TestBootstrap(
            "TestComic",
            LocalDate.of(2019, 4, 1),
            "gocomics",
            "test-comic",
            null,
            null
        );

        Bootstrap config = Bootstrap.builder()
                .dailyComics(List.of(bootstrap))
                .build();

        // When: Converting to ComicConfig
        ComicConfig comicConfig = config.getComicConfig();

        // Then: active should default to true via isActive() helper
        ComicItem comic = comicConfig.getComics().get(0);
        assertThat(comic.isActive()).isTrue();
    }

    @Test
    void convertBootstrapWithAllNewFeatures() {
        // Given: A bootstrap with all new features
        TestBootstrap bootstrap = new TestBootstrap(
            "FoxTrot",
            LocalDate.of(2019, 4, 1),
            "gocomics",
            "foxtrot",
            List.of(DayOfWeek.SUNDAY),
            true
        );

        Bootstrap config = Bootstrap.builder()
                .dailyComics(List.of(bootstrap))
                .build();

        // When: Converting to ComicConfig
        ComicConfig comicConfig = config.getComicConfig();

        // Then: All new fields should be present
        ComicItem comic = comicConfig.getComics().get(0);
        assertThat(comic.getSourceIdentifier()).isEqualTo("foxtrot");
        assertThat(comic.getPublicationDays()).containsExactly(DayOfWeek.SUNDAY);
        assertThat(comic.getActive()).isTrue();
    }

    @Test
    void convertMultipleBootstraps() {
        // Given: Multiple bootstraps with different configurations
        TestBootstrap active = new TestBootstrap(
            "Active Comic",
            LocalDate.of(2019, 4, 1),
            "gocomics",
            "active-comic",
            null,
            true
        );

        TestBootstrap inactive = new TestBootstrap(
            "Inactive Comic",
            LocalDate.of(2019, 4, 1),
            "gocomics",
            "inactive-comic",
            null,
            false
        );

        TestBootstrap sundayOnly = new TestBootstrap(
            "Sunday Only",
            LocalDate.of(2019, 4, 1),
            "gocomics",
            "sunday-only",
            List.of(DayOfWeek.SUNDAY),
            true
        );

        Bootstrap config = Bootstrap.builder()
                .dailyComics(List.of(active, inactive, sundayOnly))
                .build();

        // When: Converting to ComicConfig
        ComicConfig comicConfig = config.getComicConfig();

        // Then: All comics should be converted correctly
        assertThat(comicConfig.getComics()).hasSize(3);

        ComicItem activeComic = findComicByName(comicConfig, "Active Comic");
        assertThat(activeComic.isActive()).isTrue();
        assertThat(activeComic.getPublicationDays()).isNull();

        ComicItem inactiveComic = findComicByName(comicConfig, "Inactive Comic");
        assertThat(inactiveComic.isActive()).isFalse();

        ComicItem sundayComic = findComicByName(comicConfig, "Sunday Only");
        assertThat(sundayComic.getPublicationDays()).containsExactly(DayOfWeek.SUNDAY);
        assertThat(sundayComic.isActive()).isTrue();
    }

    // Helper method to find comic by name
    private ComicItem findComicByName(ComicConfig config, String name) {
        return config.getComics().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Comic not found: " + name));
    }

    /**
     * Test implementation of IComicsBootstrap for testing purposes
     */
    private static class TestBootstrap implements IComicsBootstrap {
        private final String name;
        private final LocalDate startDate;
        private final String source;
        private final String sourceIdentifier;
        private final List<DayOfWeek> publicationDays;
        private final Boolean active;

        public TestBootstrap(String name, LocalDate startDate, String source, String sourceIdentifier,
                           List<DayOfWeek> publicationDays, Boolean active) {
            this.name = name;
            this.startDate = startDate;
            this.source = source;
            this.sourceIdentifier = sourceIdentifier;
            this.publicationDays = publicationDays;
            this.active = active;
        }

        @Override
        public String stripName() {
            return name;
        }

        @Override
        public LocalDate startDate() {
            return startDate;
        }

        @Override
        public Object getDownloader() {
            return null; // Not needed for these tests
        }

        @Override
        public String getSource() {
            return source;
        }

        @Override
        public String getSourceIdentifier() {
            return sourceIdentifier;
        }

        @Override
        public List<DayOfWeek> getPublicationDays() {
            return publicationDays;
        }

        @Override
        public Boolean getActive() {
            return active != null ? active : true;
        }
    }
}
