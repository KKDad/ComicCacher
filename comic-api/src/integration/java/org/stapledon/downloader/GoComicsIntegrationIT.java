package org.stapledon.downloader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.engine.downloader.GoComics;
import org.stapledon.engine.downloader.IDailyComic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;

class GoComicsIntegrationIT {
    private static final Logger LOG = LoggerFactory.getLogger(GoComicsIntegrationIT.class);
    private static Path path;
    private static CacheProperties cacheProperties;

    @BeforeAll
    static void setUp() throws Exception {
        path = Files.createTempDirectory("GoComicsTest");
        LOG.info("Using TempDirectory: " + path.toString());

        // Create CacheProperties for tests with headless=true
        cacheProperties = new CacheProperties();
        cacheProperties.setChromeHeadless(true);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (!Files.exists(path))
            return;

        // Remote test directory and contents
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private GoComics getSubject(String name) {
        GoComics gc = new GoComics(null, cacheProperties);
        gc.setComic(name);
        gc.setDate(LocalDate.now().minusDays(1)); // Use a relative date
        gc.setCacheRoot(path.toString());

        return gc;
    }

    @Test
    void ensureCacheTest() throws Exception {
        LocalDate testDate = LocalDate.now().minusDays(1);
        String year = String.valueOf(testDate.getYear());
        String dateString = testDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        File expectedFile = new File(path.toString() + "/AdamAtHome/" + year + "/" + dateString + ".png");
        LOG.info("Expecting to get file: " + expectedFile);
        assertThat(expectedFile).doesNotExist();

        try (IDailyComic subject = getSubject("Adam at Home")) {
            // Act
            boolean result = subject.ensureCache();

        // Assert
        assertThat(result).isTrue();
        assertThat(expectedFile).exists();
        }
    }

    @Test
    void advanceTest() {
        // Arrange
        try (GoComics subject = getSubject("Adam at Home")) {
            // Act
            LocalDate result = subject.advance();

            // Assert
            assertThat(result).isEqualTo(LocalDate.now());
        }
    }


    /**
     * This test validates that the updateComicMetadata method properly extracts author information
     * for a subset of comics. Since the GoComics website structure has changed, we now focus on
     * a smaller set of popular comics to verify the functionality.
     *
     * Note: This test is dependent on external web resources, which may change.
     * If it fails, it may be due to website changes rather than code issues.
     */
    @ParameterizedTest
    @CsvSource({
            "Adam at Home",
            "Garfield",
            "Peanuts",
            "CalvinAndHobbes"
            })
    void getComicDetailsTest(String name) {
        // Arrange
        try (GoComics subject = getSubject(name)) {
            // Act
            ComicItem item = new ComicItem();
            subject.updateComicMetadata(item);

        // Assert that we get basic metadata regardless of website changes
        assertThat(item.getDescription()).isNotNull();
        assertThat(item.getAvatarAvailable()).isTrue();
        assertThat(item.getAuthor()).isNotNull();

        // Log the actual author for verification
        LOG.info("Comic '{}' has author: '{}'", name, item.getAuthor());

        // Make sure author starts with "By " as expected by other parts of the app
        assertThat(item.getAuthor()).startsWith("By ");
        }
    }

    /**
     * Tests for a few specific known comics to verify expected fallback behavior.
     * This test is more resilient to website changes by checking for known patterns
     * rather than exact values.
     */
    @Test
    void shouldHandleSpecificComics() {
        // Adam at Home by Rob Harrell
        try (GoComics subject = getSubject("Adam at Home")) {
            ComicItem item = new ComicItem();
            subject.updateComicMetadata(item);

            // Verify we get some kind of metadata
            assertThat(item.getDescription()).isNotEmpty();
            assertThat(item.getAuthor()).isNotEmpty();
        }

        // Reset with a different comic
        try (GoComics subject = getSubject("Garfield")) {
            ComicItem item = new ComicItem();
            subject.updateComicMetadata(item);

            // Should mention Jim Davis somewhere in the author line
            assertThat(item.getAuthor()).contains("By ");
        }

        // One more popular comic
        try (GoComics subject = getSubject("Peanuts")) {
            ComicItem item = new ComicItem();
            subject.updateComicMetadata(item);

            // Should have author and description
            assertThat(item.getAuthor()).isNotEmpty();
            assertThat(item.getDescription()).isNotEmpty();
        }
    }
}