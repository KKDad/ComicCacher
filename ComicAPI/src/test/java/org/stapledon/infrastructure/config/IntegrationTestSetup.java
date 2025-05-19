package org.stapledon.infrastructure.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.stapledon.infrastructure.config.properties.CacheProperties;
import org.stapledon.infrastructure.config.properties.JwtProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.annotation.PostConstruct;

/**
 * Configuration for setting up integration tests.
 * Creates necessary directories and files for testing.
 */
@TestConfiguration
public class IntegrationTestSetup {

    /**
     * Create test directories and files for integration tests
     */
    @PostConstruct
    public void initTestEnvironment() throws IOException {
        // Create test cache directory
        Path integrationCacheDir = Paths.get("./integration-cache");
        if (!Files.exists(integrationCacheDir)) {
            Files.createDirectories(integrationCacheDir);
        }
        
        // Create empty config files if they don't exist
        createEmptyJsonFile(integrationCacheDir.resolve("integration-comics.json"));
        createEmptyJsonFile(integrationCacheDir.resolve("integration-users.json"));
        createEmptyJsonFile(integrationCacheDir.resolve("integration-preferences.json"));
    }
    
    /**
     * Helper to create empty JSON files for testing
     */
    private void createEmptyJsonFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            Files.write(filePath, "{}".getBytes());
        }
    }
    
    /**
     * JWT properties for integration tests.
     * This bean should be read from application-integration.properties.
     */
    @Bean
    @Primary
    public JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("integration-test-secret-key-very-long-and-secure-for-testing-purposes-123456789");
        properties.setExpiration(300000); // 5 minutes
        properties.setRefreshExpiration(600000); // 10 minutes
        return properties;
    }
    
    /**
     * Cache properties for integration tests.
     * This bean should be read from application-integration.properties.
     */
    @Bean
    @Primary
    public CacheProperties cacheProperties() {
        Path integrationCacheDir = Paths.get("./integration-cache");
        
        CacheProperties properties = new CacheProperties();
        properties.setLocation(integrationCacheDir.toAbsolutePath().toString());
        properties.setConfig("integration-comics.json");
        properties.setUsersConfig("integration-users.json");
        properties.setPreferencesConfig("integration-preferences.json");
        
        return properties;
    }
}