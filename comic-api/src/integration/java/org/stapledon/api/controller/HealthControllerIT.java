package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.stapledon.AbstractIntegrationTest;
import org.stapledon.api.dto.health.HealthStatus;

/**
 * Integration tests for the HealthController
 * Tests actual endpoint behavior with real dependencies
 */
public class HealthControllerIT extends AbstractIntegrationTest {

        private String authToken;

        @BeforeEach
        void setUp() throws Exception {
                authToken = authenticateUser();
        }

        @Test
        @DisplayName("Health endpoint should return basic health status without detailed flag")
        void getHealthStatus_WithoutDetailedFlag_ShouldReturnBasicHealthStatus() throws Exception {
                // When
                MvcResult result = mockMvc.perform(get("/api/v1/health")
                                .header("Authorization", "Bearer " + authToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andReturn();

                // Verify response status is 200 OK
                assertThat(result.getResponse().getStatus())
                                .as("Expected GET /health to return status 200")
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

                // Parse and verify health status data
                HealthStatus healthStatus = extractFromResponse(responseContent, "data", HealthStatus.class);
                assertThat(healthStatus)
                                .as("Response should contain valid health status data")
                                .isNotNull();

                // Verify essential health status fields
                assertThat(healthStatus.getStatus())
                                .as("Health status should have a status field")
                                .isNotNull();

                assertThat(healthStatus.getTimestamp())
                                .as("Health status should have a timestamp")
                                .isNotNull();

                assertThat(healthStatus.getUptime())
                                .as("Health status should have an uptime value")
                                .isGreaterThan(0);
        }

        @Test
        @DisplayName("Health endpoint should return detailed health status with detailed flag")
        void getHealthStatus_WithDetailedFlag_ShouldReturnDetailedHealthStatus() throws Exception {
                // When
                MvcResult result = mockMvc.perform(get("/api/v1/health")
                                .param("detailed", "true")
                                .header("Authorization", "Bearer " + authToken)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andReturn();

                // Verify response status is 200 OK
                assertThat(result.getResponse().getStatus())
                                .as("Expected GET /health with detailed=true to return status 200")
                                .isEqualTo(HttpStatus.OK.value());

                // Verify response contains data field
                String responseContent = result.getResponse().getContentAsString();
                assertThat(responseContent)
                                .as("Response should contain 'data' field")
                                .contains("data");

                // Parse and verify detailed health status data
                HealthStatus healthStatus = extractFromResponse(responseContent, "data", HealthStatus.class);
                assertThat(healthStatus)
                                .as("Response should contain valid health status data")
                                .isNotNull();

                // Verify detailed health status fields
                assertThat(healthStatus.getStatus())
                                .as("Health status should have a status field")
                                .isNotNull();

                assertThat(healthStatus.getTimestamp())
                                .as("Health status should have a timestamp")
                                .isNotNull();

                assertThat(healthStatus.getUptime())
                                .as("Health status should have an uptime value")
                                .isGreaterThan(0);

                // Verify detailed fields are included
                assertThat(healthStatus.getBuildInfo())
                                .as("Detailed health status should include build info")
                                .isNotNull();

                assertThat(healthStatus.getSystemResources())
                                .as("Detailed health status should include system resources")
                                .isNotNull();

                // Components may be null depending on implementation, so we don't assert on it
        }

        @Test
        @DisplayName("Health endpoint should support no authentication")
        void getHealthStatus_WithoutAuthentication_ShouldReturnHealthStatus() throws Exception {
                // When - Send request without authentication token
                MvcResult result = mockMvc.perform(get("/api/v1/health")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andReturn();

                // Verify response status is 200 OK
                assertThat(result.getResponse().getStatus())
                                .as("Expected GET /health without auth to return status 200")
                                .isEqualTo(HttpStatus.OK.value());

                // Verify response contains data field
                String responseContent = result.getResponse().getContentAsString();
                assertThat(responseContent)
                                .as("Response should contain 'data' field")
                                .contains("data");

                // Parse and verify health status data
                HealthStatus healthStatus = extractFromResponse(responseContent, "data", HealthStatus.class);
                assertThat(healthStatus)
                                .as("Response should contain valid health status data")
                                .isNotNull();

                assertThat(healthStatus.getStatus())
                                .as("Health status should have a status field")
                                .isNotNull();
        }

        @Test
        @DisplayName("Health endpoint should accept application/json content type")
        void getHealthStatus_WithApplicationJsonContentType_ShouldReturnHealthStatus() throws Exception {
                // When
                MvcResult result = mockMvc.perform(get("/api/v1/health")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andReturn();

                // Verify response status is 200 OK
                assertThat(result.getResponse().getStatus())
                                .as("Expected GET /health with application/json to return status 200")
                                .isEqualTo(HttpStatus.OK.value());

                // Verify response contains data field
                String responseContent = result.getResponse().getContentAsString();
                assertThat(responseContent)
                                .as("Response should contain 'data' field")
                                .contains("data");

                // Parse and verify health status data
                HealthStatus healthStatus = extractFromResponse(responseContent, "data", HealthStatus.class);
                assertThat(healthStatus)
                                .as("Response should contain valid health status data")
                                .isNotNull();
        }

        @Test
        @DisplayName("Health endpoint should accept default content type")
        void getHealthStatus_WithDefaultContentType_ShouldReturnHealthStatus() throws Exception {
                // When
                MvcResult result = mockMvc.perform(get("/api/v1/health"))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andReturn();

                // Verify response status is 200 OK
                assertThat(result.getResponse().getStatus())
                                .as("Expected GET /health with default content type to return status 200")
                                .isEqualTo(HttpStatus.OK.value());

                // Verify response contains data field
                String responseContent = result.getResponse().getContentAsString();
                assertThat(responseContent)
                                .as("Response should contain 'data' field")
                                .contains("data");

                // Parse and verify health status data
                HealthStatus healthStatus = extractFromResponse(responseContent, "data", HealthStatus.class);
                assertThat(healthStatus)
                                .as("Response should contain valid health status data")
                                .isNotNull();
        }
}