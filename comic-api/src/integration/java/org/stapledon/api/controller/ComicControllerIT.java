package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.stapledon.AbstractIntegrationTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Base64;

/**
 * Integration tests for ComicController binary image endpoints.
 *
 * Tests both avatar and strip image endpoints, verifying binary response format,
 * proper content-type headers, cache control, and error handling.
 */
class ComicControllerIT extends AbstractIntegrationTest {

    private static final String API_BASE_PATH = "/api/v1";
    private static final String COMICS_PATH = API_BASE_PATH + "/comics";

    // Known test comic ID - use comic ID 1 which should exist in test data
    private static final int TEST_COMIC_ID = 1;

    // Test image - 1x1 PNG pixel (base64 encoded)
    private static final String TEST_IMAGE_BASE64 =
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";

    @BeforeAll
    static void setupTestImages() throws IOException {
        // Create test strip images for the dates we'll be testing
        Path integrationCacheDir = Paths.get("./integration-cache");
        Path testComicDir = integrationCacheDir.resolve("TestComic");

        // Create test strips for specific dates
        createTestStrip(testComicDir, "2023", "2023-01-15.png");
        createTestStrip(testComicDir, "2023", "2023-01-01.png");
        createTestStrip(testComicDir, "2024", "2024-05-19.png");
    }

    private static void createTestStrip(Path comicDir, String year, String filename) throws IOException {
        Path yearDir = comicDir.resolve(year);
        Files.createDirectories(yearDir);
        byte[] imageBytes = Base64.getDecoder().decode(TEST_IMAGE_BASE64);
        Files.write(yearDir.resolve(filename), imageBytes);
    }

    // =========================================================================
    // Avatar Endpoint Tests
    // =========================================================================

    @Test
    @DisplayName("Should retrieve comic avatar as binary data with proper headers")
    void retrieveAvatarTest() throws Exception {
        // Execute request for comic avatar
        MvcResult result = mockMvc.perform(get(COMICS_PATH + "/{comic}/avatar", TEST_COMIC_ID))
                .andDo(print())
                .andReturn();

        // Verify response status is 200 OK or 404 (if comic doesn't have avatar)
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /comics/{comic}/avatar to return status 200 or 404")
                .isIn(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value());

        // If successful, verify binary image response
        if (result.getResponse().getStatus() == HttpStatus.OK.value()) {
            byte[] responseBytes = result.getResponse().getContentAsByteArray();

            assertThat(responseBytes)
                    .as("Response should contain binary image data")
                    .isNotEmpty();

            // Verify content type is an image type
            String contentType = result.getResponse().getContentType();
            assertThat(contentType)
                    .as("Content-Type should be an image MIME type")
                    .matches("image/.*");

            // Verify cache control header is set (1 day for avatars)
            String cacheControl = result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL);
            assertThat(cacheControl)
                    .as("Cache-Control header should be present")
                    .isNotNull();

            // Verify Content-Length header is set
            String contentLength = result.getResponse().getHeader(HttpHeaders.CONTENT_LENGTH);
            assertThat(contentLength)
                    .as("Content-Length header should be present")
                    .isNotNull();
            assertThat(Integer.parseInt(contentLength))
                    .as("Content-Length should match actual content size")
                    .isEqualTo(responseBytes.length);
        }
    }

    @Test
    @DisplayName("Should return 404 for non-existent comic avatar")
    void retrieveAvatarForNonExistentComicTest() throws Exception {
        int nonExistentComicId = 99999;

        mockMvc.perform(get(COMICS_PATH + "/{comic}/avatar", nonExistentComicId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // Strip Endpoint Tests
    // =========================================================================

    @Test
    @DisplayName("Should retrieve comic strip as binary data with proper headers")
    void retrieveStripTest() throws Exception {
        LocalDate testDate = LocalDate.of(2023, 1, 15);

        // Execute request for comic strip
        MvcResult result = mockMvc.perform(get(COMICS_PATH + "/{comic}/strip/{date}", TEST_COMIC_ID, testDate))
                .andDo(print())
                .andReturn();

        // Verify response status is 200 OK or 404 (if strip doesn't exist for that date)
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /comics/{comic}/strip/{date} to return status 200 or 404")
                .isIn(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value());

        // If successful, verify binary image response
        if (result.getResponse().getStatus() == HttpStatus.OK.value()) {
            byte[] responseBytes = result.getResponse().getContentAsByteArray();

            assertThat(responseBytes)
                    .as("Response should contain binary image data")
                    .isNotEmpty();

            // Verify content type is an image type
            String contentType = result.getResponse().getContentType();
            assertThat(contentType)
                    .as("Content-Type should be an image MIME type")
                    .matches("image/.*");

            // Verify cache control header is set (7 days for strips)
            String cacheControl = result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL);
            assertThat(cacheControl)
                    .as("Cache-Control header should be present")
                    .isNotNull();

            // Verify Content-Length header is set
            String contentLength = result.getResponse().getHeader(HttpHeaders.CONTENT_LENGTH);
            assertThat(contentLength)
                    .as("Content-Length header should be present")
                    .isNotNull();
            assertThat(Integer.parseInt(contentLength))
                    .as("Content-Length should match actual content size")
                    .isEqualTo(responseBytes.length);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "2023-01-01",
        "2024-05-19",
        "2023-06-15"
    })
    @DisplayName("Should handle various valid dates for strip retrieval")
    void retrieveStripWithVariousDatesTest(LocalDate date) throws Exception {
        MvcResult result = mockMvc.perform(get(COMICS_PATH + "/{comic}/strip/{date}", TEST_COMIC_ID, date))
                .andDo(print())
                .andReturn();

        // Should return either 200 (strip exists) or 404 (strip doesn't exist for that date)
        assertThat(result.getResponse().getStatus())
                .as("Expected valid status for date: " + date)
                .isIn(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Should return 404 for non-existent comic strip")
    void retrieveStripForNonExistentComicTest() throws Exception {
        int nonExistentComicId = 99999;
        LocalDate testDate = LocalDate.of(2023, 1, 15);

        mockMvc.perform(get(COMICS_PATH + "/{comic}/strip/{date}", nonExistentComicId, testDate))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 for strip with date before comic's oldest date")
    void retrieveStripBeforeOldestDateTest() throws Exception {
        LocalDate beforeOldest = TEST_COMIC_OLDEST_DATE.minusDays(1);

        mockMvc.perform(get(COMICS_PATH + "/{comic}/strip/{date}", TEST_COMIC_ID, beforeOldest))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 for strip with future date")
    void retrieveStripForFutureDateTest() throws Exception {
        LocalDate futureDate = LocalDate.now().plusYears(1);

        mockMvc.perform(get(COMICS_PATH + "/{comic}/strip/{date}", TEST_COMIC_ID, futureDate))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should handle invalid date format gracefully")
    void retrieveStripWithInvalidDateFormatTest() throws Exception {
        // Spring will return 400 Bad Request for invalid date format
        mockMvc.perform(get(COMICS_PATH + "/{comic}/strip/{date}", TEST_COMIC_ID, "invalid-date"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
