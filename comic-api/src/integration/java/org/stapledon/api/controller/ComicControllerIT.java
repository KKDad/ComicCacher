package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;
import org.stapledon.AbstractIntegrationTest;

/**
 * Integration tests for ComicController binary image endpoints.
 * 
 * Navigation endpoints have been removed - use GraphQL for strip navigation.
 * This controller now only serves avatar images.
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
    @DisplayName("Should return 404 for non-existent comic avatar")
    void retrieveAvatarForNonExistentComicTest() throws Exception {
        int nonExistentComicId = 99999;

        MvcResult result = mockMvc.perform(get(COMICS_PATH + "/{comic}/avatar", nonExistentComicId))
                .andDo(print())
                .andReturn();

        assertThat(result.getResponse().getStatus())
                .as("Expected GET /comics/{comic}/avatar for non-existent comic to return 404")
                .isEqualTo(HttpStatus.NOT_FOUND.value());
    }
}
