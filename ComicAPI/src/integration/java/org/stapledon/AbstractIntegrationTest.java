package org.stapledon;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.stapledon.api.dto.auth.AuthRequest;

import java.time.LocalDate;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for all integration tests
 * Uses the main application security configuration with permissive settings for testing
 * Provides common test utilities and helper methods
 */
@Slf4j
@SpringBootTest(classes = ComicApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Getter
public abstract class AbstractIntegrationTest {

    // Create integration cache directory and initial JSON files if they don't exist
    static {
        try {
            java.nio.file.Path integrationCacheDir = java.nio.file.Paths.get("./integration-cache");
            if (!java.nio.file.Files.exists(integrationCacheDir)) {
                log.info("Creating integration cache directory: {}", integrationCacheDir);
                java.nio.file.Files.createDirectories(integrationCacheDir);
                
                // Create empty JSON files if not exists
                createEmptyJsonFile(integrationCacheDir.resolve("comics.json"));
                createEmptyJsonFile(integrationCacheDir.resolve("users.json"));
                createEmptyJsonFile(integrationCacheDir.resolve("preferences.json"));
                createEmptyJsonFile(integrationCacheDir.resolve("bootstrap.json"));
                
                // Create test comic directory
                java.nio.file.Path testComicDir = integrationCacheDir.resolve("TestComic");
                java.nio.file.Files.createDirectories(testComicDir);
                java.nio.file.Files.createDirectories(testComicDir.resolve("2023"));
                java.nio.file.Files.createDirectories(testComicDir.resolve("2024"));
            }
        } catch (java.io.IOException e) {
            log.error("Failed to create integration test directories: {}", e.getMessage(), e);
        }
    }
    
    private static void createEmptyJsonFile(java.nio.file.Path path) throws java.io.IOException {
        if (!java.nio.file.Files.exists(path)) {
            log.info("Creating empty JSON file: {}", path);
            java.nio.file.Files.write(path, "{}".getBytes());
        }
    }

    // Integration test comics
    protected static final int TEST_COMIC_ID = 1;
    protected static final String TEST_COMIC_NAME = "Test Comic";
    protected static final LocalDate TEST_COMIC_OLDEST_DATE = LocalDate.of(2023, 1, 1);
    protected static final LocalDate TEST_COMIC_NEWEST_DATE = LocalDate.of(2024, 5, 19);
    
    // Integration test users
    protected static final String TEST_USER = "testuser";
    protected static final String TEST_ADMIN_USER = "adminuser";
    
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected StapledonAccountGivens givens;

    @Autowired
    protected ObjectMapper objectMapper;
    
    /**
     * Create a test user with the given username and attempt to authenticate
     * Handles errors gracefully
     *
     */
    protected String authenticateUser() {
        try {
            StapledonAccountGivens.GivenAccountContext context =givens.givenUser();
            log.info("Using test user: {}", context.getUsername());

            return authenticateAndGetToken(context.getUsername(), context.getPassword());
        } catch (Exception e) {
            log.error("Error authenticating user: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Authenticate with the given credentials and extract JWT token from response
     *
     */
    protected String authenticateAndGetToken(String username, String password) throws Exception {
        try {
            AuthRequest authRequest = AuthRequest.builder()
                    .username(username)
                    .password(password)
                    .build();

            log.debug("Authenticating with: {} / {}", username, password);

            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
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
     * Extract a value from a JSON response by path
     *
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
}