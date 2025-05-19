package org.stapledon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.stapledon.infrastructure.config.IntegrationTestConfig;
import org.stapledon.infrastructure.config.IntegrationTestInitializer;
import org.stapledon.infrastructure.config.TestIntegrationConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.stapledon.api.dto.auth.AuthRequest;
import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.api.dto.comic.ImageDto;
import org.stapledon.api.dto.preference.UserPreference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Base class for all integration tests
 * Uses the main application security configuration with permissive settings for testing
 * Provides common test utilities and helper methods
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
@Import(TestIntegrationConfiguration.class)
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
public abstract class AbstractIntegrationTest {

    protected static final String API_BASE_PATH = "/api/v1";
    protected static final String AUTH_PATH = API_BASE_PATH + "/auth";
    protected static final String COMICS_PATH = API_BASE_PATH + "/comics";
    protected static final String USERS_PATH = API_BASE_PATH + "/users";
    protected static final String PREFERENCES_PATH = API_BASE_PATH + "/preferences";
    protected static final String METRICS_PATH = API_BASE_PATH + "/metrics";
    protected static final String UPDATE_PATH = API_BASE_PATH + "/update";

    protected static final String INTEGRATION_CACHE_PATH = "/Users/agilbert/kkdad/ComicCacher/ComicAPI/integration-cache";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected StapledonAccountGivens given;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Generate a unique username for test isolation
     * @param prefix Prefix to use for username
     * @return A unique username
     */
    protected String generateUniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Log debug information about the integration cache directory
     * Useful for troubleshooting test issues
     */
    protected void logCacheDirectoryInfo() {
        log.info("Cache directory exists: {}", new java.io.File(INTEGRATION_CACHE_PATH).exists());
        log.info("Cache directory can write: {}", new java.io.File(INTEGRATION_CACHE_PATH).canWrite());
        log.info("Users file exists: {}", new java.io.File(INTEGRATION_CACHE_PATH + "/integration-users.json").exists());

        try {
            String usersFileContent = Files.readString(Paths.get(INTEGRATION_CACHE_PATH, "integration-users.json"));
            log.info("Users file content: {}", usersFileContent);
        } catch (IOException e) {
            log.error("Error reading users file: {}", e.getMessage());
        }
    }

    /**
     * Helper method to create a test user and return the account context
     *
     * @param username Username for the test user
     * @return Account context for the created user
     */
    protected StapledonAccountGivens.GivenAccountContext createTestUser(String username) {
        return given.givenUser(
                StapledonAccountGivens.AccountInfoParameters.builder()
                        .username(username)
                        .password("test_password")
                        .email(username + "@example.com")
                        .build());
    }

    /**
     * Create a test user with the given username and attempt to authenticate
     * Handles errors gracefully
     *
     * @param username Username for the test user
     * @return JWT token if authentication successful, null otherwise
     */
    protected String authenticateUser(String username) {
        try {
            // Create a test user with specific username for easier debugging
            StapledonAccountGivens.GivenAccountContext context = createTestUser(username);

            log.info("Created test user: {}", context.getUsername());

            return authenticateAndGetToken(context.getUsername(), context.getPassword());
        } catch (Exception e) {
            log.error("Error authenticating user: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Authenticate with the given credentials and extract JWT token from response
     *
     * @param username Username to authenticate with
     * @param password Password to authenticate with
     * @return JWT token if authentication successful, null otherwise
     * @throws Exception If an error occurs during authentication
     */
    protected String authenticateAndGetToken(String username, String password) throws Exception {
        try {
            AuthRequest authRequest = AuthRequest.builder()
                    .username(username)
                    .password(password)
                    .build();

            log.debug("Authenticating with: {} / {}", username, password);

            MvcResult result = mockMvc.perform(post(AUTH_PATH + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(authRequest)))
                .andDo(print())
                .andReturn();

            // If authentication failed, return null
            if (result.getResponse().getStatus() != 200) {
                log.warn("Authentication failed with status: {}", result.getResponse().getStatus());
                return null;
            }

            String responseContent = result.getResponse().getContentAsString();

            return extractFromResponse(responseContent, "data.token", String.class);
        } catch (Exception e) {
            log.error("Error during authentication: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Add authorization header to request
     *
     * @param requestBuilder Request builder to add header to
     * @param token JWT token to add
     * @return Request builder with authorization header
     */
    protected MockHttpServletRequestBuilder addAuthHeader(MockHttpServletRequestBuilder requestBuilder, String token) {
        return requestBuilder.header("Authorization", "Bearer " + token);
    }

    /**
     * Get first comic ID from comics API
     *
     * @return Comic ID if found, -1 otherwise
     */
    protected int getFirstComicId() {
        try {
            MvcResult allComicsResult = mockMvc.perform(get(COMICS_PATH))
                .andReturn();

            if (allComicsResult.getResponse().getStatus() != 200) {
                log.warn("Could not get comics list");
                return -1;
            }

            List<ComicItem> comics = extractComicList(allComicsResult.getResponse().getContentAsString());

            if (comics.isEmpty()) {
                log.warn("No comics available");
                return -1;
            }

            return comics.get(0).getId();
        } catch (Exception e) {
            log.error("Error getting first comic ID: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * Extract a value from a JSON response by path
     *
     * @param <T> Type to extract
     * @param responseContent JSON string to extract from
     * @param path JSON path to extract (e.g. "data.token")
     * @param clazz Class of value to extract
     * @return Extracted value or null if not found
     */
    @SuppressWarnings("unchecked")
    protected <T> T extractFromResponse(String responseContent, String path, Class<T> clazz) {
        try {
            JsonNode root = objectMapper.readTree(responseContent);
            String[] pathParts = path.split("\\.");

            JsonNode current = root;
            for (String part : pathParts) {
                current = current.path(part);
            }

            if (current.isNull() || current.isMissingNode()) {
                return null;
            }

            if (clazz == String.class) {
                return (T) current.asText();
            } else if (clazz == Integer.class) {
                return (T) Integer.valueOf(current.asInt());
            } else if (clazz == Boolean.class) {
                return (T) Boolean.valueOf(current.asBoolean());
            } else {
                return objectMapper.treeToValue(current, clazz);
            }
        } catch (Exception e) {
            log.error("Error extracting from response: {} - {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * Extract user preference from API response
     *
     * @param jsonResponse API response JSON string
     * @return Extracted user preference
     * @throws JsonProcessingException If JSON parsing fails
     * @throws IOException If reading fails
     */
    protected UserPreference extractUserPreference(String jsonResponse) throws JsonProcessingException, IOException {
        return objectMapper.readValue(
            objectMapper.readTree(jsonResponse).path("data").toString(),
            UserPreference.class
        );
    }

    /**
     * Extract list of comics from API response
     *
     * @param jsonResponse API response JSON string
     * @return Extracted list of comics
     * @throws JsonProcessingException If JSON parsing fails
     * @throws IOException If reading fails
     */
    protected List<ComicItem> extractComicList(String jsonResponse) throws JsonProcessingException, IOException {
        try {
            return objectMapper.readValue(
                objectMapper.readTree(jsonResponse).path("data").toString(),
                new TypeReference<List<ComicItem>>() {}
            );
        } catch (Exception e) {
            log.error("Error extracting comic list: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Extract a single comic from API response
     *
     * @param jsonResponse API response JSON string
     * @return Extracted comic
     * @throws JsonProcessingException If JSON parsing fails
     * @throws IOException If reading fails
     */
    protected ComicItem extractSingleComic(String jsonResponse) throws JsonProcessingException, IOException {
        return objectMapper.readValue(
            objectMapper.readTree(jsonResponse).path("data").toString(),
            ComicItem.class
        );
    }

    /**
     * Extract image dto from API response
     *
     * @param jsonResponse API response JSON string
     * @return Extracted image dto
     * @throws JsonProcessingException If JSON parsing fails
     * @throws IOException If reading fails
     */
    protected ImageDto extractImageDto(String jsonResponse) throws JsonProcessingException, IOException {
        try {
            if (jsonResponse == null || jsonResponse.isEmpty()) {
                log.warn("Empty JSON response when extracting image DTO");
                return null;
            }
            
            if (jsonResponse.contains("data")) {
                return objectMapper.readValue(
                    objectMapper.readTree(jsonResponse).path("data").toString(),
                    ImageDto.class
                );
            } else {
                return objectMapper.readValue(jsonResponse, ImageDto.class);
            }
        } catch (Exception e) {
            log.error("Error extracting image DTO: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verify API response status with descriptive assertion message
     *
     * @param result API result to verify
     * @param expectedStatus Expected HTTP status code
     * @param context Context description for assertion message
     */
    protected void verifyStatus(MvcResult result, int expectedStatus, String context) {
        assertThat(result.getResponse().getStatus())
            .as("Expected %s to return status %d but got %d",
                context, expectedStatus, result.getResponse().getStatus())
            .isEqualTo(expectedStatus);
    }

    /**
     * Verify API response status accepting multiple possible statuses
     *
     * @param result API result to verify
     * @param expectedStatuses Expected HTTP status codes
     * @param context Context description for assertion message
     */
    protected void verifyStatusIn(MvcResult result, int[] expectedStatuses, String context) {
        // Convert primitive int array to Integer array for isIn method
        Integer[] boxedStatuses = new Integer[expectedStatuses.length];
        for (int i = 0; i < expectedStatuses.length; i++) {
            boxedStatuses[i] = expectedStatuses[i];
        }
        
        assertThat(result.getResponse().getStatus())
            .as("Expected %s to return one of statuses %s but got %d", 
                context, java.util.Arrays.toString(expectedStatuses), result.getResponse().getStatus())
            .isIn((Object[]) boxedStatuses);
    }

    /**
     * Verify response body contains expected data
     *
     * @param responseContent Response content to check
     * @param expectedContent Expected substring
     * @param context Context description for assertion message
     */
    protected void verifyResponseContains(String responseContent, String expectedContent, String context) {
        assertThat(responseContent)
            .as("Expected %s response to contain '%s'", context, expectedContent)
            .contains(expectedContent);
    }
}