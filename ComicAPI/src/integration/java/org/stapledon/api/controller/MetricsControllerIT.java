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

class MetricsControllerIT extends AbstractIntegrationTest {

    private static final String API_BASE_PATH = "/api/v1";
    private static final String METRICS_PATH = API_BASE_PATH + "/metrics";
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