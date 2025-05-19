package org.stapledon.infrastructure.config;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Initializer for integration tests that sets up test properties and
 * creates necessary files.
 */
public class IntegrationTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String BASE_PATH = "/Users/agilbert/kkdad/ComicCacher/ComicAPI/integration-cache";
    
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        // Create base directory if it doesn't exist
        createDirectories();
        
        // Set properties for tests
        TestPropertyValues values = TestPropertyValues.of(
                "cache.location=" + BASE_PATH,
                "cache.config=" + BASE_PATH + "/integration-comics.json",
                "cache.users.config=" + BASE_PATH + "/integration-users.json",
                "cache.preferences.config=" + BASE_PATH + "/integration-preferences.json",
                "daily.runner.enabled=false",
                "startup.reconciler.enabled=false",
                "jwt.secret=integration-test-secret-key-123456789012345678901234567890",
                "jwt.expiration=300000",
                "jwt.refresh-expiration=600000"
        );
        
        values.applyTo(context);
    }
    
    private void createDirectories() {
        try {
            // Create base directory
            Path basePath = Paths.get(BASE_PATH);
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
            }
            
            // Create subdirectories for comics
            Files.createDirectories(Paths.get(BASE_PATH, "TestComic"));
            Files.createDirectories(Paths.get(BASE_PATH, "TestComic", "2025"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test directories", e);
        }
    }
}