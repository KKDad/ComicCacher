package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.stapledon.api.dto.auth.AuthResponse;
import org.stapledon.api.dto.user.User;
import org.stapledon.api.dto.user.UserRegistrationDto;
import org.stapledon.infrastructure.config.properties.JwtProperties;
import org.stapledon.infrastructure.security.JwtTokenUtil;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pure integration test that tests JWT token generation directly
 * without Spring context dependencies
 */
public class PureAuthIntegrationTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .build();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void jwtTokenGeneration() {
        // Create JWT properties
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-for-integration-testing-12345678901234567890");
        jwtProperties.setExpiration(300000); // 5 minutes
        jwtProperties.setRefreshExpiration(3600000); // 1 hour

        // Create JWT token util
        JwtTokenUtil jwtTokenUtil = new JwtTokenUtil(jwtProperties);

        // Create a test user
        User user = User.builder()
                .username("jwtuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .email("jwt@example.com")
                .displayName("JWT Test User")
                .created(LocalDateTime.now())
                .roles(Collections.singletonList("USER"))
                .userToken(UUID.randomUUID())
                .build();

        // Generate token
        String token = jwtTokenUtil.generateToken(user);

        // Verify
        assertThat(token).as("Token should not be null").isNotNull();
        assertThat(token.length() > 20).as("Token should be of reasonable length").isTrue();

        // Extract username from token
        String username = jwtTokenUtil.extractUsername(token);
        assertThat(username).as("Username should be extracted correctly").isEqualTo("jwtuser");

        // Create UserDetails object for validation
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .roles("USER")
                .build();

        // Validate token
        assertThat(jwtTokenUtil.validateToken(token, userDetails)).as("Token should be valid").isTrue();
    }

    @Test
    void userRegistrationStructure() {
        // This test verifies the structure of DTOs
        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username("testuser")
                .password("password123")
                .email("test@example.com")
                .displayName("Test User")
                .build();

        // Verify DTO properties
        assertThat(registrationDto.getUsername()).isEqualTo("testuser");
        assertThat(registrationDto.getPassword()).isEqualTo("password123");
        assertThat(registrationDto.getEmail()).isEqualTo("test@example.com");
        assertThat(registrationDto.getDisplayName()).isEqualTo("Test User");

        // Test JSON serialization
        try {
            String json = objectMapper.writeValueAsString(registrationDto);
            UserRegistrationDto deserialized = objectMapper.readValue(json, UserRegistrationDto.class);

            assertThat(deserialized.getUsername()).isEqualTo(registrationDto.getUsername());
            assertThat(deserialized.getPassword()).isEqualTo(registrationDto.getPassword());
            assertThat(deserialized.getEmail()).isEqualTo(registrationDto.getEmail());
            assertThat(deserialized.getDisplayName()).isEqualTo(registrationDto.getDisplayName());
        } catch (Exception e) {
            fail("", "JSON serialization should not throw an exception: " + e.getMessage());
        }
    }

    @Test
    void authResponseStructure() {
        // Create auth response
        AuthResponse authResponse = AuthResponse.builder()
                .username("testuser")
                .displayName("Test User")
                .token("jwt.token.here")
                .refreshToken("refresh.token.here")
                .build();

        // Verify properties
        assertThat(authResponse.getUsername()).isEqualTo("testuser");
        assertThat(authResponse.getDisplayName()).isEqualTo("Test User");
        assertThat(authResponse.getToken()).isEqualTo("jwt.token.here");
        assertThat(authResponse.getRefreshToken()).isEqualTo("refresh.token.here");

        // Test JSON serialization
        try {
            String json = objectMapper.writeValueAsString(authResponse);
            AuthResponse deserialized = objectMapper.readValue(json, AuthResponse.class);

            assertThat(deserialized.getUsername()).isEqualTo(authResponse.getUsername());
            assertThat(deserialized.getDisplayName()).isEqualTo(authResponse.getDisplayName());
            assertThat(deserialized.getToken()).isEqualTo(authResponse.getToken());
            assertThat(deserialized.getRefreshToken()).isEqualTo(authResponse.getRefreshToken());
        } catch (Exception e) {
            fail("", "JSON serialization should not throw an exception: " + e.getMessage());
        }
    }
}
