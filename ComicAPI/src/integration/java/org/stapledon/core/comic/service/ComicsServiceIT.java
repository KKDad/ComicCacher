package org.stapledon.core.comic.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.stapledon.AbstractIntegrationTest;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.util.Direction;
import org.stapledon.core.comic.management.ComicManagementFacade;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

/**
 * Integration tests for ComicsService implementation.
 * Tests the service's interactions with the ComicManagementFacade and other dependencies
 * in a real application context.
 */
@Slf4j
class ComicsServiceIT extends AbstractIntegrationTest {

    @Autowired
    private ComicsService comicsService;

    @Autowired
    private ComicManagementFacade comicManagementFacade;

    private ComicItem testComic;

    @BeforeEach
    void setUp() {
        testComic = comicManagementFacade.getComic(TEST_COMIC_ID)
                .orElseGet(() -> {
                    ComicItem newComic = ComicItem.builder()
                            .id(TEST_COMIC_ID)
                            .name(TEST_COMIC_NAME)
                            .author("Test Author")
                            .description("Test Description")
                            .enabled(true)
                            .oldest(TEST_COMIC_OLDEST_DATE)
                            .newest(TEST_COMIC_NEWEST_DATE)
                            .avatarAvailable(false)
                            .source("gocomics")
                            .sourceIdentifier("testcomic")
                            .build();
                    comicManagementFacade.createComic(newComic);
                    return newComic;
                });
    }

    @Test
    @DisplayName("Should retrieve all comics")
    void retrieveAllTest() {
        List<ComicItem> comics = comicsService.retrieveAll();
        assertThat(comics).isNotEmpty();
        assertThat(comics.stream().anyMatch(comic -> comic.getId() == TEST_COMIC_ID)).isTrue();
    }

    @Test
    @DisplayName("Should retrieve specific comic by ID")
    void retrieveComicTest() {
        Optional<ComicItem> comicOptional = comicsService.retrieveComic(TEST_COMIC_ID);
        assertThat(comicOptional).isPresent();
        assertThat(comicOptional.get().getId()).isEqualTo(TEST_COMIC_ID);
    }

    @Test
    @DisplayName("Should return empty optional for non-existent comic")
    void retrieveNonExistentComicTest() {
        assertThat(comicsService.retrieveComic(999999)).isEmpty();
    }

    @Test
    @DisplayName("Should create new comic")
    void createComicTest() {
        int newComicId = TEST_COMIC_ID + 1000;
        ComicItem newComic = ComicItem.builder()
                .id(newComicId)
                .name("New Test Comic")
                .author("New Test Author")
                .description("New Test Description")
                .enabled(true)
                .oldest(LocalDate.now().minusYears(1))
                .newest(LocalDate.now())
                .avatarAvailable(false)
                .source("gocomics")
                .sourceIdentifier("newtestcomic")
                .build();

        try {
            Optional<ComicItem> createdComic = comicsService.createComic(newComicId, newComic);
            assertThat(createdComic).isPresent();
            assertThat(createdComic.get().getId()).isEqualTo(newComicId);
            assertThat(comicsService.retrieveComic(newComicId)).isPresent();
        } finally {
            comicsService.deleteComic(newComicId);
        }
    }

    @Test
    @DisplayName("Should update existing comic")
    void updateComicTest() {
        String updatedDescription = "Updated Test Description " + System.currentTimeMillis();
        ComicItem updatedComic = ComicItem.builder()
                .id(testComic.getId())
                .name(testComic.getName())
                .author(testComic.getAuthor())
                .description(updatedDescription)
                .enabled(testComic.isEnabled())
                .oldest(testComic.getOldest())
                .newest(testComic.getNewest())
                .avatarAvailable(testComic.isAvatarAvailable())
                .source(testComic.getSource())
                .sourceIdentifier(testComic.getSourceIdentifier())
                .build();

        Optional<ComicItem> updatedComicOptional = comicsService.updateComic(TEST_COMIC_ID, updatedComic);
        assertThat(updatedComicOptional).isPresent();
        assertThat(updatedComicOptional.get().getDescription()).isEqualTo(updatedDescription);
        assertThat(comicsService.retrieveComic(TEST_COMIC_ID).get().getDescription()).isEqualTo(updatedDescription);
    }

    @Test
    @DisplayName("Should delete comic")
    void deleteComicTest() {
        int tempComicId = TEST_COMIC_ID + 2000;
        ComicItem tempComic = ComicItem.builder()
                .id(tempComicId)
                .name("Temporary Test Comic")
                .author("Temp Test Author")
                .description("Temp Test Description")
                .enabled(true)
                .oldest(LocalDate.now().minusYears(1))
                .newest(LocalDate.now())
                .avatarAvailable(false)
                .source("gocomics")
                .sourceIdentifier("temptestcomic")
                .build();

        comicsService.createComic(tempComicId, tempComic);
        assertThat(comicsService.retrieveComic(tempComicId)).isPresent();
        assertThat(comicsService.deleteComic(tempComicId)).isTrue();
        assertThat(comicsService.retrieveComic(tempComicId)).isEmpty();
    }
}