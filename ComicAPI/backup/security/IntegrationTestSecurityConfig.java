package org.stapledon.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.stapledon.config.properties.CacheProperties;
import org.stapledon.config.properties.JwtProperties;
import org.stapledon.security.JwtAuthenticationEntryPoint;
import org.stapledon.security.JwtTokenFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Unified security configuration for integration tests
 * Combines elements from multiple security configs to avoid conflicts
 */
@TestConfiguration
@EnableWebSecurity
@Profile("integration")
public class IntegrationTestSecurityConfig {

    /**
     * Configure security for integration tests
     * Disables security features to simplify testing
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public SecurityFilterChain integrationSecurityFilterChain(HttpSecurity http) throws Exception {
        // Use a security matcher to ensure this only applies to specific paths
        // This helps avoid conflicts with other security filter chains
        http.securityMatcher("/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().permitAll()
            );
        
        return http.build();
    }
    
    /**
     * Password encoder for integration tests
     */
    @Bean
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
     * JWT authentication entry point for integration tests
     */
    @Bean
    @Primary
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }
    
    /**
     * Authentication manager for integration tests
     */
    @Bean
    @Primary
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfiguration) throws Exception {
        return authConfiguration.getAuthenticationManager();
    }
    
    /**
     * User details service with test users for integration tests
     */
    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        // Create test users for integration tests
        UserDetails testUser = User.builder()
                .username("testuser")
                .password(passwordEncoder().encode("testpassword"))
                .roles("USER")
                .build();
        
        UserDetails testAdmin = User.builder()
                .username("testadmin")
                .password(passwordEncoder().encode("testpassword"))
                .roles("USER", "ADMIN")
                .build();
        
        return new InMemoryUserDetailsManager(testUser, testAdmin);
    }
    
    /**
     * Cache properties for integration tests
     * Creates test directories if they don't exist
     */
    @Bean
    @Primary
    public CacheProperties cacheProperties() throws IOException {
        // Create test cache directory
        Path testCacheDir = Paths.get("./integration-cache");
        if (!Files.exists(testCacheDir)) {
            Files.createDirectories(testCacheDir);
        }
        
        // Initialize cache properties
        CacheProperties properties = new CacheProperties();
        properties.setLocation(testCacheDir.toAbsolutePath().toString());
        properties.setConfig("integration-comics.json");
        properties.setUsersConfig("integration-users.json");
        properties.setPreferencesConfig("integration-preferences.json");
        
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