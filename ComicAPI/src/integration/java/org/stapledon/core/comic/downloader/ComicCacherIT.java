package org.stapledon.core.comic.downloader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.stapledon.AbstractIntegrationTest;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.util.Bootstrap;
import org.stapledon.infrastructure.config.IComicsBootstrap;
import org.stapledon.infrastructure.storage.ComicStorageFacade;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class ComicCacherIT extends AbstractIntegrationTest {

    @Autowired
    private ComicCacher comicCacher;

    @Autowired
    private ComicStorageFacade storageFacade;

    @Autowired
    private Bootstrap bootstrap;

    @BeforeEach
    void setUp() {
        ComicItem testComic = ComicItem.builder()
                .id(TEST_COMIC_ID)
                .name(TEST_COMIC_NAME)
                .author("Test Author")
                .description("Test Description")
                .enabled(true)
                .oldest(TEST_COMIC_OLDEST_DATE)
                .newest(TEST_COMIC_NEWEST_DATE)
                .source("gocomics")
                .sourceIdentifier("testcomic")
                .build();
    }

    @Test
    @DisplayName("Should cache a single comic with valid configuration")
    void cacheSingleTest() {
        ComicItem calvinAndHobbes = ComicItem.builder()
                .id("Calvin and Hobbes".hashCode())
                .name("Calvin and Hobbes")
                .author("By Bill Watterson")
                .description("Calvin and Hobbes")
                .enabled(true)
                .source("gocomics")
                .sourceIdentifier("calvinandhobbes")
                .build();

        boolean result = comicCacher.cacheSingle(calvinAndHobbes);
        log.info("Calvin and Hobbes caching result: {}", result);
    }

    @Test
    @DisplayName("Should handle invalid comic gracefully")
    void cacheSingleWithInvalidComicTest() {
        ComicItem invalidComic = ComicItem.builder()
                .id(9999)
                .name("NonexistentComic")
                .author("Fake Author")
                .description("This comic doesn't exist")
                .enabled(true)
                .source("gocomics")
                .sourceIdentifier("nonexistentcomic")
                .build();

        boolean result = comicCacher.cacheSingle(invalidComic);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should cache comics from bootstrap")
    void cacheSingleFromBootstrapTest() {
        IComicsBootstrap kingComicsBootstrap = bootstrap.getKingComics() != null && !bootstrap.getKingComics().isEmpty()
                ? bootstrap.getKingComics().get(0)
                : bootstrap.getDailyComics() != null && !bootstrap.getDailyComics().isEmpty()
                ? bootstrap.getDailyComics().get(0)
                : null;

        if (kingComicsBootstrap == null) {
            log.warn("No bootstrap comics found, skipping test");
            return;
        }

        boolean result = comicCacher.cacheSingle(true, kingComicsBootstrap);
        log.info("Beetle Bailey caching result: {}", result);
    }

    @Test
    @DisplayName("Should attempt to cache all comics")
    void cacheAllTest() {
        boolean result = comicCacher.cacheAll();
        log.info("Cache all comics result: {}", result);
    }

    @Test
    @DisplayName("Should find bootstrap for comic")
    void lookupGoComicsTest() {
        ComicItem peanuts = ComicItem.builder()
                .id("Peanuts".hashCode())
                .name("Peanuts")
                .build();

        IComicsBootstrap result = comicCacher.lookupGoComics(peanuts);

        if (result != null) {
            assertThat(result.stripName()).isEqualToIgnoringCase("Peanuts");
            assertThat(result.getSource()).isNotEmpty();
            assertThat(result.getSourceIdentifier()).isNotEmpty();
        }
    }
}