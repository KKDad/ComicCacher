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
import org.stapledon.api.dto.comic.ComicStorageMetrics;
import org.stapledon.api.dto.comic.ImageCacheStats;
import org.stapledon.core.comic.management.ComicManagementFacade;
import org.stapledon.infrastructure.storage.ComicStorageFacade; // Add this
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import org.stapledon.api.dto.comic.ComicItem;
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
        // Ensure the newly created comic has at least one image cached for metrics to be generated
        try (IDailyComic dailyComic = comicManagementFacade.getComic(3).map(comic -> {
            GoComics gc = new GoComics(null);
            gc.setComic(comic.getName());
            gc.setDate(LocalDate.now().minusDays(1)); // Use a relative date
            gc.setCacheRoot(comicStorageFacade.getCacheRoot().getAbsolutePath());
            return gc;
        }).orElseThrow(() -> new RuntimeException("Test comic not found"))) {
            dailyComic.ensureCache();
        } catch (Exception e) {
            log.error("Failed to cache image for new test comic: {}", e.getMessage(), e);
            throw e;
        }
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

        // Attempt to parse as ComicStorageMetrics to verify structure
        ComicStorageMetrics metrics = extractFromResponse(responseContent, "data", ComicStorageMetrics.class);
        assertThat(metrics)
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

        // Attempt to parse as ImageCacheStats to verify structure
        ImageCacheStats stats = extractFromResponse(responseContent, "data", ImageCacheStats.class);
        assertThat(stats)
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

        // Verify response contains comic metrics
        JsonNode dataNode = objectMapper.readTree(responseContent).path("data");
        assertThat(dataNode.isObject())
                .as("Data node should be a JSON object")
                .isTrue();

        // The response has comics as keys, ensure at least one comic exists in the response
        assertThat(dataNode.size())
                .as("Combined metrics should include at least one comic")
                .isGreaterThan(0);
        
        // Check that at least one comic entry has the required fields
        boolean hasRequiredFields = false;
        for (JsonNode comicMetrics : dataNode) {
            if (comicMetrics.has("comicName") && 
                comicMetrics.has("storageBytes") && 
                comicMetrics.has("accessCount")) {
                hasRequiredFields = true;
                break;
            }
        }
        
        assertThat(hasRequiredFields)
                .as("At least one comic should have the required metrics fields")
                .isTrue();
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

        // Verify response can be parsed as ComicStorageMetrics
        ComicStorageMetrics metrics = extractFromResponse(responseContent, "data", ComicStorageMetrics.class);
        assertThat(metrics)
                .as("Response should contain valid refreshed storage metrics data")
                .isNotNull();

        // Verify total size is available
        assertThat(metrics.getStorageBytes())
                .as("Refreshed metrics should include total size")
                .isNotNull();
    }
}