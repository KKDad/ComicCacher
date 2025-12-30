package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.stapledon.AbstractIntegrationTest;
import org.stapledon.StapledonAccountGivens;

class UpdateControllerIT extends AbstractIntegrationTest {

    private static final String API_BASE_PATH = "/api/v1";
    private static final String UPDATE_PATH = API_BASE_PATH + "/update";
    private String authToken;

    @BeforeEach
    void setUp() {
        // Use direct JWT token generation instead of authentication
        StapledonAccountGivens.GivenAccountContext context = StapledonAccountGivens.GivenAccountContext.builder()
                .username("testuser")
                .build();

        authToken = context.authenticate();
        assertThat(authToken)
            .as("Token generation should succeed")
            .isNotNull();
    }

    @Test
    @Tag("slow") // This test might be slow as it updates all comics
    @DisplayName("Should update all comics")
    void updateAllComicsTest() throws Exception {
        // Execute request to update all comics with auth token
        MockHttpServletRequestBuilder request = get(UPDATE_PATH)
            .header("Authorization", "Bearer " + authToken);

        MvcResult result = mockMvc.perform(request)
            .andDo(print())
            .andReturn();

        // Verify response status is 200 OK
        assertThat(result.getResponse().getStatus())
            .as("Expected GET /update to return status 200")
            .isEqualTo(HttpStatus.OK.value());

        // Verify response contains data
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent)
            .as("Response should contain 'data' field")
            .contains("data");

        // Verify response contains message about updating comics
        assertThat(responseContent)
            .as("Response should contain message about updating comics")
            .contains("update");
    }

    @Test
    @DisplayName("Should update a specific comic")
    void updateSpecificComicTest() throws Exception {
        // Use test comic ID from constants
        int comicId = TEST_COMIC_ID;

        // Execute request to update specific comic with auth token
        MockHttpServletRequestBuilder request = get(UPDATE_PATH + "/{comicId}", comicId)
            .header("Authorization", "Bearer " + authToken);

        MvcResult result = mockMvc.perform(request)
            .andDo(print())
            .andReturn();

        // Verify response status is 200 OK
        assertThat(result.getResponse().getStatus())
            .as("Expected GET /update/{comicId} to return status 200")
            .isEqualTo(HttpStatus.OK.value());

        // Verify response contains data
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent)
            .as("Response should contain 'data' field")
            .contains("data");

        // Verify response contains message about updating the specific comic
        assertThat(responseContent)
            .as("Response should contain message about updating specific comic")
            .contains("update");
    }

    @Test
    @DisplayName("Should return 404 for non-existent comic")
    void updateNonExistentComicTest() throws Exception {
        // Use a comic ID that doesn't exist
        int nonExistentId = 999999;

        // Execute request to update non-existent comic with auth token
        MockHttpServletRequestBuilder request = get(UPDATE_PATH + "/{comicId}", nonExistentId)
            .header("Authorization", "Bearer " + authToken);

        MvcResult result = mockMvc.perform(request)
            .andDo(print())
            .andReturn();

        // Verify response status is 404 Not Found
        assertThat(result.getResponse().getStatus())
            .as("Expected GET /update/{comicId} with non-existent ID to return status 404")
            .isEqualTo(HttpStatus.NOT_FOUND.value());
    }
}