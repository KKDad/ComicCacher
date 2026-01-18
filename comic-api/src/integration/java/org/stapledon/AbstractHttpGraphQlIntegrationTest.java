package org.stapledon;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import org.stapledon.infrastructure.security.JwtTokenUtil;
import org.stapledon.metrics.collector.StorageMetricsCollector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.stream.Stream;
import tools.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for all integration tests
 * Uses the main application security configuration with permissive settings for
 * testing
 * Provides common test utilities and helper methods
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@ActiveProfiles("integration")
@Getter
public abstract class AbstractHttpGraphQlIntegrationTest {

    // Integration test comics
    protected static final int TEST_COMIC_ID = 1;
    protected static final String TEST_COMIC_NAME = "Test Comic";
    protected static final LocalDate TEST_COMIC_OLDEST_DATE = LocalDate.of(2023, 1, 1);
    protected static final LocalDate TEST_COMIC_NEWEST_DATE = LocalDate.of(2024, 5, 19);

    // Integration test users
    protected static final String TEST_USER = "testuser";
    protected static final String TEST_ADMIN_USER = "adminuser";

    @Autowired
    protected HttpGraphQlTester graphQlTester;

    @Autowired
    protected StapledonAccountGivens givens;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected StorageMetricsCollector storageMetricsCollector;

    @Autowired
    protected JwtTokenUtil jwtTokenUtil;

    @BeforeAll
    static void setup() {
        try {
            Path integrationCacheDir = Paths.get("./integration-cache");
            if (Files.exists(integrationCacheDir)) {
                try (Stream<Path> walk = Files.walk(integrationCacheDir)) {
                    walk.sorted(java.util.Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
            }

            log.info("Creating integration cache directory: {}", integrationCacheDir);
            Files.createDirectories(integrationCacheDir);

            // Create comics.json
            String comicsJson = """
                    {
                      "items": {
                        "1": {
                          "id": 1,
                          "name": "Test Comic",
                          "author": "Test Author",
                          "oldest": "2023-01-01",
                          "newest": "2024-05-19",
                          "enabled": true,
                          "description": "A comic for integration testing",
                          "avatarAvailable": true,
                          "source": "gocomics",
                          "sourceIdentifier": "testcomic"
                        },
                        "2": {
                          "id": 2,
                          "name": "Another Test Comic",
                          "author": "Another Test Author",
                          "oldest": "2023-02-15",
                          "newest": "2024-05-18",
                          "enabled": true,
                          "description": "Another comic for integration testing",
                          "avatarAvailable": true,
                          "source": "comicskingdom",
                          "sourceIdentifier": "anothertestcomic"
                        }
                      }
                    }""";
            try {
                Files.writeString(integrationCacheDir.resolve("comics.json"), comicsJson);
            } catch (IOException e) {
                log.error("Failed to write comics.json: {}", e.getMessage(), e);
            }

            // Create empty users.json and preferences.json
            createEmptyJsonFile(integrationCacheDir.resolve("users.json"));
            createEmptyJsonFile(integrationCacheDir.resolve("preferences.json"));
            createEmptyJsonFile(integrationCacheDir.resolve("bootstrap.json"));

            // Create test comic directory and avatar
            Path testComicDir = integrationCacheDir.resolve("TestComic");
            try {
                Files.createDirectories(testComicDir);
                Files.write(testComicDir.resolve("avatar.png"), java.util.Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=")); // dummy
                // avatar
                Files.createDirectories(testComicDir.resolve("2023"));
                Files.createDirectories(testComicDir.resolve("2024"));
            } catch (IOException e) {
                log.error("Failed to create test comic directory or avatar: {}", e.getMessage(), e);
            }
        } catch (IOException e) {
            log.error("Failed to create integration test directories: {}", e.getMessage(), e);
        }
    }

    private static void createEmptyJsonFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            log.info("Creating empty JSON file: {}", path);
            Files.write(path, "{}".getBytes());
        }
    }

    /**
     * Create a test user with the given username and attempt to authenticate
     * Handles errors gracefully
     */
    protected String authenticateUser() {
        try {
            StapledonAccountGivens.GivenAccountContext context = givens.givenUser();
            String jwtToken = context.authenticate();
            log.info("Using test user: {}", context.getUsername());
            graphQlTester = getGraphQlTester()
                .mutate()
                .headers(h -> h.setBearerAuth(jwtToken))
                .build();

            return jwtToken;
        } catch (Exception e) {
            log.error("Error authenticating user: {}", e.getMessage());
            return null;
        }
    }
}
