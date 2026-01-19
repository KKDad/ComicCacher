package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;
import org.stapledon.AbstractIntegrationTest;
import org.stapledon.common.dto.ImageDto;

import java.time.format.DateTimeFormatter;
import tools.jackson.databind.JsonNode;

/**
 * Integration tests for ComicController image endpoints.
 * CRUD operations have been moved to GraphQL ComicResolver.
 */
class ComicControllerIT extends AbstractIntegrationTest {

    private static final String API_BASE_PATH = "/api/v1";
    private static final String COMICS_PATH = API_BASE_PATH + "/comics";

    // Known test comic ID - use comic ID 1 which should exist in test data
    private static final int TEST_COMIC_ID = 1;

    @Test
    @DisplayName("Should retrieve comic avatar")
    void retrieveAvatarTest() throws Exception {
        // Execute request for comic avatar
        MvcResult result = mockMvc.perform(get(COMICS_PATH + "/{comic}/avatar", TEST_COMIC_ID))
                .andDo(print())
                .andReturn();

        // Verify response status is 200 OK or 404 (if comic doesn't have avatar)
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /comics/{comic}/avatar to return status 200 or 404")
                .isIn(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value());

        // If successful, verify response contains image data
        if (result.getResponse().getStatus() == HttpStatus.OK.value()) {
            String responseContent = result.getResponse().getContentAsString();
            assertThat(responseContent)
                    .as("Response should contain 'image' data")
                    .contains("image");
        }
    }

    @Test
    @DisplayName("Should retrieve first comic image")
    void retrieveFirstComicImageTest() throws Exception {
        // Execute request for first comic image
        MvcResult result = mockMvc.perform(get(COMICS_PATH + "/{comic}/strips/first", TEST_COMIC_ID))
                .andDo(print())
                .andReturn();

        // Verify response status is either 200 OK or 404 Not Found (if no images are
        // available)
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /comics/{comic}/strips/first to return status 200 or 404")
                .isIn(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value());

        // If successful, verify response format and structure
        if (result.getResponse().getStatus() == HttpStatus.OK.value()) {
            String responseContent = result.getResponse().getContentAsString();
            assertThat(responseContent)
                    .as("Response should be valid JSON")
                    .isNotEmpty();

            // Verify response has ComicNavigationResult structure
            assertThat(responseContent)
                    .as("Response should contain 'found' field")
                    .contains("\"found\":");

            // Extract image if available
            ImageDto image = extractImageDto(responseContent);

            if (image != null) {
                assertThat(image.getImageDate())
                        .as("Image should have a date when found=true")
                        .isNotNull();
                assertThat(image.getMimeType())
                        .as("Image should have a mime type")
                        .isNotNull();
            } else {
                assertThat(responseContent)
                        .as("When image is null, response should have found=false")
                        .contains("\"found\":false");
            }
        }
    }

    @Test
    @DisplayName("Should retrieve next and previous comic images")
    void retrieveNextAndPreviousImageTest() throws Exception {
        // Get first comic strip
        MvcResult firstComicResult = mockMvc.perform(get(COMICS_PATH + "/{comic}/strips/first", TEST_COMIC_ID))
                .andReturn();

        // Skip test if first image not available
        if (firstComicResult.getResponse().getStatus() != HttpStatus.OK.value()) {
            return;
        }

        // Extract first image
        ImageDto firstImage = extractImageDto(firstComicResult.getResponse().getContentAsString());

        // Skip test if no images are available (found=false)
        if (firstImage == null || firstImage.getImageDate() == null) {
            return;
        }

        String dateStr = firstImage.getImageDate().format(DateTimeFormatter.ISO_DATE);

        // Try to get next comic after the first
        MvcResult nextResult = mockMvc.perform(get(COMICS_PATH + "/{comic}/next/{date}", TEST_COMIC_ID, dateStr))
                .andDo(print())
                .andReturn();

        // Verify response status is either 200 OK or 404 Not Found
        assertThat(nextResult.getResponse().getStatus())
                .as("Expected GET /comics/{comic}/next/{date} to return status 200 or 404")
                .isIn(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value());

        // Get last comic strip
        MvcResult lastComicResult = mockMvc.perform(get(COMICS_PATH + "/{comic}/strips/last", TEST_COMIC_ID))
                .andReturn();

        // Skip remaining test if last image not available
        if (lastComicResult.getResponse().getStatus() != HttpStatus.OK.value()) {
            return;
        }

        // Extract last image
        ImageDto lastImage = extractImageDto(lastComicResult.getResponse().getContentAsString());

        // Skip test if no images are available (found=false)
        if (lastImage == null || lastImage.getImageDate() == null) {
            return;
        }

        String lastDateStr = lastImage.getImageDate().format(DateTimeFormatter.ISO_DATE);

        // Try to get previous comic before the last
        MvcResult previousResult = mockMvc
                .perform(get(COMICS_PATH + "/{comic}/previous/{date}", TEST_COMIC_ID, lastDateStr))
                .andDo(print())
                .andReturn();

        // Verify response status is either 200 OK or 404 Not Found
        assertThat(previousResult.getResponse().getStatus())
                .as("Expected GET /comics/{comic}/previous/{date} to return status 200 or 404")
                .isIn(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Should retrieve last comic image")
    void retrieveLastComicImageTest() throws Exception {
        // Execute request for last comic image
        MvcResult result = mockMvc.perform(get(COMICS_PATH + "/{comic}/strips/last", TEST_COMIC_ID))
                .andDo(print())
                .andReturn();

        // Verify response status is either 200 OK or 404 Not Found (if no images are
        // available)
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /comics/{comic}/strips/last to return status 200 or 404")
                .isIn(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value());

        // If successful, verify response format and structure
        if (result.getResponse().getStatus() == HttpStatus.OK.value()) {
            String responseContent = result.getResponse().getContentAsString();
            assertThat(responseContent)
                    .as("Response should be valid JSON")
                    .isNotEmpty();

            // Verify response has ComicNavigationResult structure
            assertThat(responseContent)
                    .as("Response should contain 'found' field")
                    .contains("\"found\":");

            // Extract image if available
            ImageDto image = extractImageDto(responseContent);

            if (image != null) {
                assertThat(image.getImageDate())
                        .as("Image should have a date when found=true")
                        .isNotNull();
                assertThat(image.getMimeType())
                        .as("Image should have a mime type")
                        .isNotNull();
            } else {
                assertThat(responseContent)
                        .as("When image is null, response should have found=false")
                        .contains("\"found\":false");
            }
        }
    }

    @Test
    @DisplayName("Should handle invalid date format properly")
    void retrieveComicWithInvalidDateFormatTest() throws Exception {
        String invalidDate = "not-a-date";

        // Execute request with invalid date format
        MvcResult result = mockMvc.perform(get(COMICS_PATH + "/{comic}/next/{date}", TEST_COMIC_ID, invalidDate))
                .andDo(print())
                .andReturn();

        // Verify response status is 400 Bad Request
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /comics/{comic}/next/{date} with invalid date to return status 400")
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    // Helper method

    /**
     * Extract image dto from API response
     */
    private ImageDto extractImageDto(String jsonResponse) {
        try {
            if (jsonResponse == null || jsonResponse.isEmpty()) {
                return null;
            }

            JsonNode root = objectMapper.readTree(jsonResponse);

            // Check if response has ComicNavigationResult structure
            if (root.has("found")) {
                boolean found = root.path("found").asBoolean();
                if (found && root.has("image")) {
                    JsonNode imageNode = root.path("image");
                    return objectMapper.readValue(imageNode.toString(), ImageDto.class);
                }
                return null;
            }

            // Legacy format or direct ImageDto
            if (root.has("data")) {
                return objectMapper.readValue(root.path("data").toString(), ImageDto.class);
            } else {
                return objectMapper.readValue(jsonResponse, ImageDto.class);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
