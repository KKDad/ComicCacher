package org.stapledon.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.stapledon.config.properties.CacheProperties;
import org.stapledon.config.properties.JwtProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for integration tests
 * Creates required directories and beans for testing
 */
@TestConfiguration
@EnableWebSecurity
@EnableAutoConfiguration
@Profile("test") // Only activate in test profile, not integration
public class IntegrationTestConfiguration {

    /**
     * Configure security for tests
     * Permits all requests to simplify testing
     * NOTE: Disabled for integration tests to avoid filter chain conflicts
     */
    @Bean
    @Primary
    @Profile("test") // Explicitly mark as test-only
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
    
    /**
     * Password encoder for integration tests
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * JWT properties for integration tests
     */
    @Bean
    @Primary
    public JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("integration-test-secret-key-very-long-and-secure-for-testing");
        properties.setExpiration(300000); // 5 minutes
        properties.setRefreshExpiration(600000); // 10 minutes
        return properties;
    }
    
    /**
     * Cache properties for integration tests
     * Creates test directories if they don't exist
     */
    @Bean
    @Primary
    public CacheProperties cacheProperties() throws IOException {
        // Create test cache directory
        Path testCacheDir = Paths.get("./test-cache");
        if (!Files.exists(testCacheDir)) {
            Files.createDirectories(testCacheDir);
        }
        
        // Initialize cache properties
        CacheProperties properties = new CacheProperties();
        properties.setLocation(testCacheDir.toAbsolutePath().toString());
        properties.setConfig("test-comics.json");
        properties.setUsersConfig("test-users.json");
        properties.setPreferencesConfig("test-preferences.json");
        
        // Create empty config files if they don't exist
        createEmptyJsonFile(testCacheDir.resolve(properties.getConfig()));
        createEmptyJsonFile(testCacheDir.resolve(properties.getUsersConfig()));
        createEmptyJsonFile(testCacheDir.resolve(properties.getPreferencesConfig()));
        
        return properties;
    }
    
    /**
     * Helper to create empty JSON files for testing
     */
    private void createEmptyJsonFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            Files.write(filePath, "{}".getBytes());
        }
    }
}