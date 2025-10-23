package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.stapledon.AbstractIntegrationTest;
import org.stapledon.StapledonAccountGivens;
import org.stapledon.common.dto.ImageCacheStats;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.engine.management.ComicManagementFacade;
import org.stapledon.common.service.ComicStorageFacade;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import org.stapledon.common.dto.ComicItem;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MetricsControllerIT extends AbstractIntegrationTest {

    private static final String API_BASE_PATH = "/api/v1";
    private static final String METRICS_PATH = API_BASE_PATH + "/metrics";
    private String authToken;

    @Autowired
    private ComicManagementFacade comicManagementFacade;

    @Autowired
    private ComicStorageFacade comicStorageFacade;

    @BeforeEach
    void setUp() throws Exception {
        // Use direct JWT token generation instead of authentication
        StapledonAccountGivens.GivenAccountContext context = StapledonAccountGivens.GivenAccountContext.builder()
                .username("testuser")
                .build();

        authToken = context.authenticate();
        assertThat(authToken)
            .as("Token generation should succeed")
            .isNotNull();

        createTestComic(3, "New Test Comic");

        // Force refresh all metrics to populate the JSON files
        MockHttpServletRequestBuilder refreshRequest = get(METRICS_PATH + "/refresh-all")
            .header("Authorization", "Bearer " + authToken);

        mockMvc.perform(refreshRequest)
                .andExpect(status().isOk());
    }

    private void createTestComic(int comicId, String name) throws Exception {
        ComicItem newComic = ComicItem.builder()
                .id(comicId)
                .name(name)
                .enabled(true)
                .build();

        MockHttpServletRequestBuilder request = post("/api/v1/comics/" + comicId)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newComic));

        mockMvc.perform(request)
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should retrieve storage metrics successfully")
    void getStorageMetricsTest() throws Exception {
        // Execute request to get storage metrics with auth token
        MockHttpServletRequestBuilder request = get(METRICS_PATH + "/storage")
            .header("Authorization", "Bearer " + authToken);
            
        MvcResult result = mockMvc.perform(request)
                .andDo(print())
                .andReturn();

        // Verify response status is 200 OK
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /metrics/storage to return status 200")
                .isEqualTo(HttpStatus.OK.value());

        // Verify response contains data field
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent)
                .as("Response should contain 'data' field")
                .contains("data");

        // Verify response can be parsed
        JsonNode root = objectMapper.readTree(responseContent);
        assertThat(root.has("data"))
                .as("Response JSON should have a 'data' field")
                .isTrue();

        // Attempt to parse as ImageCacheStats to verify structure
        ImageCacheStats stats = extractFromResponse(responseContent, "data", ImageCacheStats.class);
        assertThat(stats)
                .as("Response should contain valid storage metrics data")
                .isNotNull();
    }

    @Test
    @DisplayName("Should retrieve access metrics successfully")
    void getAccessMetricsTest() throws Exception {
        // Execute request to get access metrics with auth token
        MockHttpServletRequestBuilder request = get(METRICS_PATH + "/access")
            .header("Authorization", "Bearer " + authToken);
            
        MvcResult result = mockMvc.perform(request)
                .andDo(print())
                .andReturn();

        // Verify response status is 200 OK
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /metrics/access to return status 200")
                .isEqualTo(HttpStatus.OK.value());

        // Verify response contains data field
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent)
                .as("Response should contain 'data' field")
                .contains("data");

        // Verify response can be parsed
        JsonNode root = objectMapper.readTree(responseContent);
        assertThat(root.has("data"))
                .as("Response JSON should have a 'data' field")
                .isTrue();

        // Attempt to parse as AccessMetricsData to verify structure
        AccessMetricsData data = extractFromResponse(responseContent, "data", AccessMetricsData.class);
        assertThat(data)
                .as("Response should contain valid access metrics data")
                .isNotNull();
    }

    @Test
    @DisplayName("Should retrieve combined metrics successfully")
    void getCombinedMetricsTest() throws Exception {
        // Refresh storage metrics to ensure the new comic is included
        mockMvc.perform(get(METRICS_PATH + "/storage/refresh")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        // Execute request to get combined metrics with auth token
        MockHttpServletRequestBuilder request = get(METRICS_PATH + "/combined")
            .header("Authorization", "Bearer " + authToken);
            
        MvcResult result = mockMvc.perform(request)
                .andDo(print())
                .andReturn();

        // Verify response status is 200 OK
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /metrics/combined to return status 200")
                .isEqualTo(HttpStatus.OK.value());

        // Verify response contains data field
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent)
                .as("Response should contain 'data' field")
                .contains("data");

        // Attempt to parse as CombinedMetricsData to verify structure
        CombinedMetricsData combinedData = extractFromResponse(responseContent, "data", CombinedMetricsData.class);
        assertThat(combinedData)
                .as("Response should contain valid combined metrics data")
                .isNotNull();

        // Verify comics map exists
        assertThat(combinedData.getComics())
                .as("Combined metrics should have a comics map")
                .isNotNull();
    }

    @AfterEach
    void tearDown() {
        comicManagementFacade.deleteComic(3);
    }

    @Test
    @DisplayName("Should refresh storage metrics successfully")
    void refreshStorageMetricsTest() throws Exception {
        // Execute request to refresh storage metrics with auth token
        MockHttpServletRequestBuilder request = get(METRICS_PATH + "/storage/refresh")
            .header("Authorization", "Bearer " + authToken);
            
        MvcResult result = mockMvc.perform(request)
                .andDo(print())
                .andReturn();

        // Verify response status is 200 OK
        assertThat(result.getResponse().getStatus())
                .as("Expected GET /metrics/storage/refresh to return status 200")
                .isEqualTo(HttpStatus.OK.value());

        // Verify response contains data field
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent)
                .as("Response should contain 'data' field")
                .contains("data");

        // Verify response can be parsed as ImageCacheStats
        ImageCacheStats stats = extractFromResponse(responseContent, "data", ImageCacheStats.class);
        assertThat(stats)
                .as("Response should contain valid refreshed storage metrics data")
                .isNotNull();
    }
}