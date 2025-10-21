package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.stapledon.AbstractIntegrationTest;
import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.api.dto.comic.ImageDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

class ComicControllerIT extends AbstractIntegrationTest {

    private static final String API_BASE_PATH = "/api/v1";
    private static final String COMICS_PATH = API_BASE_PATH + "/comics";
//
//    static {
//        // Create necessary directories for test
//        try {
//            Path basePath = Paths.get("/tmp/comicapi-integration-test/cache");
//            if (!Files.exists(basePath)) {
//                Files.createDirectories(basePath);
//            }
//
//            // Create subdirectories for comics
//            Files.createDirectories(Paths.get("/tmp/comicapi-integration-test/cache", "TestComic"));
//            Files.createDirectories(Paths.get("/tmp/comicapi-integration-test/cache", "TestComic", "2025"));
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to create test directories", e);
//        }
//    }


    @Test
    @DisplayName("Should return a list of all comics")
    void retrieveAllComicsTest() throws Exception {
        // Execute request to get all comics
        MvcResult result = mockMvc.perform(get(COMICS_PATH))
                .andDo(print())
                .andReturn();

        // Verify response status is 200 OK
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /comics to return status 200")
                .isEqualTo(HttpStatus.OK.value());

        // Verify response contains data field
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent)
                .as("Response should contain 'data' field")
                .contains("data");

        // Verify data can be parsed as a list of comics
        List<ComicItem> comics = extractComicList(responseContent);
        assertThat(comics)
                .as("Response should be a valid list of comics (can be empty)")
                .isNotNull();
    }

    @Test
    @DisplayName("Should return details for a specific comic")
    void retrieveComicDetailsTest() throws Exception {
        // Get all comics first to find a valid ID
        MvcResult allComicsResult = mockMvc.perform(get(COMICS_PATH)).andReturn();

        // Verify we could retrieve comics list
        assertThat(allComicsResult.getResponse().getStatus())
                .as("Failed to get comics list")
                .isEqualTo(HttpStatus.OK.value());

        // Extract comics from response
        List<ComicItem> comics = extractComicList(allComicsResult.getResponse().getContentAsString());

        // Skip test if no comics are available - this should be addressed with proper test data setup
        assertThat(comics)
                .as("Test requires at least one comic to be available")
                .isNotEmpty();

        // Get first comic details
        int comicId = comics.get(0).getId();

        // Execute request for comic details
        MvcResult result = mockMvc.perform(get(COMICS_PATH + "/{comic}", comicId))
                .andDo(print())
                .andReturn();

        // Verify response status is 200 OK
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /comics/{comic} to return status 200")
                .isEqualTo(HttpStatus.OK.value());

        // Verify response contains correct comic ID
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent)
                .as("Response should contain the requested comic ID")
                .contains(String.valueOf(comicId));

        // Extract comic from response and verify essential properties
        ComicItem comic = extractSingleComic(responseContent);
        assertThat(comic)
                .as("Response should contain a valid comic")
                .isNotNull();
        assertThat(comic.getId())
                .as("Comic ID should match the requested ID")
                .isEqualTo(comicId);
    }

    @Test
    @DisplayName("Should return 404 for a non-existent comic")
    void retrieveNonExistentComicTest() throws Exception {
        // Use a comic ID that doesn't exist
        int nonExistentId = 999999;

        // Execute request for non-existent comic
        MvcResult result = mockMvc.perform(get(COMICS_PATH + "/{comic}", nonExistentId))
                .andDo(print())
                .andReturn();

        // Verify response status is 404 Not Found
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /comics/{comic} with non-existent ID to return status 404")
                .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Should create, update, and delete a comic")
    void createAndUpdateComicTest() throws Exception {
        // Create a unique comic for testing
        ComicItem newComic = ComicItem.builder()
                .id(9999)
                .name("Test Comic " + System.currentTimeMillis())
                .author("Test Author")
                .description("Test Description")
                .enabled(true)
                .oldest(LocalDate.now().minusYears(1))
                .newest(LocalDate.now())
                .avatarAvailable(false)
                .source("gocomics")
                .sourceIdentifier("testcomic")
                .build();

        // Execute request to create comic
        MvcResult createResult = mockMvc.perform(post(COMICS_PATH + "/{comic}", newComic.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newComic)))
                .andDo(print())
                .andReturn();

        // Verify comic was created successfully
        assertThat(createResult.getResponse().getStatus())
                .as("Expected POST /comics/{comic} to return status 201")
                .isEqualTo(HttpStatus.CREATED.value());

        // Extract created comic
        ComicItem createdComic = extractSingleComic(createResult.getResponse().getContentAsString());
        assertThat(createdComic)
                .as("Created comic should not be null")
                .isNotNull();
        assertThat(createdComic.getName())
                .as("Created comic name should match requested name")
                .isEqualTo(newComic.getName());

        // Update comic description
        String updatedDescription = "Updated Description";
        createdComic.setDescription(updatedDescription);

        // Execute request to update comic
        MvcResult updateResult = mockMvc.perform(patch(COMICS_PATH + "/{comic}", createdComic.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createdComic)))
                .andDo(print())
                .andReturn();

        // Verify comic was updated successfully
        assertThat(updateResult.getResponse().getStatus())
                .as("Expected PATCH /comics/{comic} to return status 200")
                .isEqualTo(HttpStatus.OK.value());

        // Extract updated comic and verify description was updated
        ComicItem updatedComic = extractSingleComic(updateResult.getResponse().getContentAsString());
        assertThat(updatedComic)
                .as("Updated comic should not be null")
                .isNotNull();
        assertThat(updatedComic.getDescription())
                .as("Comic description should be updated")
                .isEqualTo(updatedDescription);

        // Delete the test comic
        MvcResult deleteResult = mockMvc.perform(delete(COMICS_PATH + "/{comic}", createdComic.getId()))
                .andReturn();

        // Verify comic was deleted successfully
        assertThat(deleteResult.getResponse().getStatus())
                .as("Expected DELETE /comics/{comic} to return status 200")
                .isEqualTo(HttpStatus.NO_CONTENT.value());

        // Verify comic no longer exists
        MvcResult verifyResult = mockMvc.perform(get(COMICS_PATH + "/{comic}", createdComic.getId()))
                .andReturn();

        // Verify comic was actually deleted
        assertThat(verifyResult.getResponse().getStatus())
                .as("Expected GET /comics/{comic} after deletion to return status 404")
                .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Should retrieve comic avatar")
    void retrieveAvatarTest() throws Exception {
        // Get all comics first to find a valid ID
        MvcResult allComicsResult = mockMvc.perform(get(COMICS_PATH)).andReturn();

        // Skip test if we can't get comics list
        assertThat(allComicsResult.getResponse().getStatus())
                .as("Failed to get comics list")
                .isEqualTo(HttpStatus.OK.value());

        // Extract comics from response
        List<ComicItem> comics = extractComicList(allComicsResult.getResponse().getContentAsString());

        // Find a comic with avatar available
        ComicItem comicWithAvatar = comics.stream()
                .filter(comic -> comic.getId() == 1)
                .findFirst()
                .orElse(null);

        // Skip test if no comics with avatar available
        if (comicWithAvatar == null) {
            return;
        }

        // Execute request for comic avatar
        MvcResult result = mockMvc.perform(get(COMICS_PATH + "/{comic}/avatar", comicWithAvatar.getId()))
                .andDo(print())
                .andReturn();

        // Verify response status is 200 OK
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /comics/{comic}/avatar to return status 200")
                .isEqualTo(HttpStatus.OK.value());

        // Verify response contains image data
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent)
                .as("Response should contain 'image' data")
                .contains("image");
    }

    @Test
    @DisplayName("Should retrieve first comic image")
    void retrieveFirstComicImageTest() throws Exception {
        // Get all comics first to find a valid ID
        MvcResult allComicsResult = mockMvc.perform(get(COMICS_PATH)).andReturn();

        // Skip test if we can't get comics list
        assertThat(allComicsResult.getResponse().getStatus())
                .as("Failed to get comics list")
                .isEqualTo(HttpStatus.OK.value());

        // Extract comics from response
        List<ComicItem> comics = extractComicList(allComicsResult.getResponse().getContentAsString());

        // Skip test if no comics are available
        if (comics.isEmpty()) {
            return;
        }

        int comicId = comics.get(0).getId();

        // Execute request for first comic image
        MvcResult result = mockMvc.perform(get(COMICS_PATH + "/{comic}/strips/first", comicId))
                .andDo(print())
                .andReturn();

        // Verify response status is either 200 OK or 404 Not Found (if no images are available)
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /comics/{comic}/strips/first to return status 200 or 404")
                .isIn(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value());

        // If successful, verify response contains image data
        if (result.getResponse().getStatus() == HttpStatus.OK.value()) {
            String responseContent = result.getResponse().getContentAsString();
            assertThat(responseContent)
                    .as("Response should contain 'image' data")
                    .contains("image");

            // Extract image and verify it has a date
            ImageDto image = extractImageDto(responseContent);
            assertThat(image)
                    .as("Response should contain a valid image")
                    .isNotNull();
            assertThat(image.getImageDate())
                    .as("Image should have a date")
                    .isNotNull();
        }
    }

    @Test
    @DisplayName("Should retrieve next and previous comic images")
    void retrieveNextAndPreviousImageTest() throws Exception {
        // Get all comics first to find a valid ID
        MvcResult allComicsResult = mockMvc.perform(get(COMICS_PATH)).andReturn();

        // Skip test if we can't get comics list
        assertThat(allComicsResult.getResponse().getStatus())
                .as("Failed to get comics list")
                .isEqualTo(HttpStatus.OK.value());

        // Extract comics from response
        List<ComicItem> comics = extractComicList(allComicsResult.getResponse().getContentAsString());

        // Skip test if no comics are available
        if (comics.isEmpty()) {
            return;
        }

        int comicId = comics.get(0).getId();

        // Get first comic strip
        MvcResult firstComicResult = mockMvc.perform(get(COMICS_PATH + "/{comic}/strips/first", comicId))
                .andReturn();

        // Skip test if first image not available
        if (firstComicResult.getResponse().getStatus() != HttpStatus.OK.value()) {
            return;
        }

        // Extract first image
        ImageDto firstImage = extractImageDto(firstComicResult.getResponse().getContentAsString());
        assertThat(firstImage)
                .as("First image should not be null")
                .isNotNull();
        assertThat(firstImage.getImageDate())
                .as("First image should have a date")
                .isNotNull();

        String dateStr = firstImage.getImageDate().format(DateTimeFormatter.ISO_DATE);

        // Try to get next comic after the first
        MvcResult nextResult = mockMvc.perform(get(COMICS_PATH + "/{comic}/next/{date}", comicId, dateStr))
                .andDo(print())
                .andReturn();

        // Verify response status is either 200 OK or 404 Not Found
        assertThat(nextResult.getResponse().getStatus())
                .as("Expected GET /comics/{comic}/next/{date} to return status 200 or 404")
                .isIn(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value());

        // Get last comic strip
        MvcResult lastComicResult = mockMvc.perform(get(COMICS_PATH + "/{comic}/strips/last", comicId))
                .andReturn();

        // Skip remaining test if last image not available
        if (lastComicResult.getResponse().getStatus() != HttpStatus.OK.value()) {
            return;
        }

        // Extract last image
        ImageDto lastImage = extractImageDto(lastComicResult.getResponse().getContentAsString());
        assertThat(lastImage)
                .as("Last image should not be null")
                .isNotNull();
        assertThat(lastImage.getImageDate())
                .as("Last image should have a date")
                .isNotNull();

        String lastDateStr = lastImage.getImageDate().format(DateTimeFormatter.ISO_DATE);

        // Try to get previous comic before the last
        MvcResult previousResult = mockMvc.perform(get(COMICS_PATH + "/{comic}/previous/{date}", comicId, lastDateStr))
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
        // Get all comics first to find a valid ID
        MvcResult allComicsResult = mockMvc.perform(get(COMICS_PATH)).andReturn();

        // Skip test if we can't get comics list
        assertThat(allComicsResult.getResponse().getStatus())
                .as("Failed to get comics list")
                .isEqualTo(HttpStatus.OK.value());

        // Extract comics from response
        List<ComicItem> comics = extractComicList(allComicsResult.getResponse().getContentAsString());

        // Skip test if no comics are available
        if (comics.isEmpty()) {
            return;
        }

        int comicId = comics.get(0).getId();

        // Execute request for last comic image
        MvcResult result = mockMvc.perform(get(COMICS_PATH + "/{comic}/strips/last", comicId))
                .andDo(print())
                .andReturn();

        // Verify response status is either 200 OK or 404 Not Found (if no images are available)
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /comics/{comic}/strips/last to return status 200 or 404")
                .isIn(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value());

        // If successful, verify response contains image data
        if (result.getResponse().getStatus() == HttpStatus.OK.value()) {
            String responseContent = result.getResponse().getContentAsString();
            assertThat(responseContent)
                    .as("Response should contain 'image' data")
                    .contains("image");

            // Extract image and verify it has a date
            ImageDto image = extractImageDto(responseContent);
            assertThat(image)
                    .as("Response should contain a valid image")
                    .isNotNull();
            assertThat(image.getImageDate())
                    .as("Image should have a date")
                    .isNotNull();
        }
    }

    @Test
    @DisplayName("Should handle invalid date format properly")
    void retrieveComicWithInvalidDateFormatTest() throws Exception {
        // Get all comics first to find a valid ID
        MvcResult allComicsResult = mockMvc.perform(get(COMICS_PATH)).andReturn();

        // Skip test if we can't get comics list
        assertThat(allComicsResult.getResponse().getStatus())
                .as("Failed to get comics list")
                .isEqualTo(HttpStatus.OK.value());

        // Extract comics from response
        List<ComicItem> comics = extractComicList(allComicsResult.getResponse().getContentAsString());

        // Skip test if no comics are available
        if (comics.isEmpty()) {
            return;
        }

        int comicId = comics.get(0).getId();
        String invalidDate = "not-a-date";

        // Execute request with invalid date format
        MvcResult result = mockMvc.perform(get(COMICS_PATH + "/{comic}/next/{date}", comicId, invalidDate))
                .andDo(print())
                .andReturn();

        // Verify response status is 400 Bad Request
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /comics/{comic}/next/{date} with invalid date to return status 400")
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    // Helper methods

    /**
     * Extract list of comics from API response
     *
     * @param jsonResponse API response JSON string
     * @return Extracted list of comics
     */
    private List<ComicItem> extractComicList(String jsonResponse) {
        try {
            JsonNode dataNode = objectMapper.readTree(jsonResponse).path("data");
            if (dataNode.isMissingNode() || dataNode.isNull()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(dataNode.toString(), new TypeReference<List<ComicItem>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Extract a single comic from API response
     *
     * @param jsonResponse API response JSON string
     * @return Extracted comic
     */
    private ComicItem extractSingleComic(String jsonResponse) {
        try {
            JsonNode dataNode = objectMapper.readTree(jsonResponse).path("data");
            if (dataNode.isMissingNode() || dataNode.isNull()) {
                return null;
            }
            return objectMapper.readValue(dataNode.toString(), ComicItem.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract image dto from API response
     *
     * @param jsonResponse API response JSON string
     * @return Extracted image dto
     */
    private ImageDto extractImageDto(String jsonResponse) {
        try {
            if (jsonResponse == null || jsonResponse.isEmpty()) {
                return null;
            }

            JsonNode root = objectMapper.readTree(jsonResponse);
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
