package org.stapledon.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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

/**
 * Pure integration test that tests JWT token generation directly
 * without Spring context dependencies
 */
public class PureAuthIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
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
        assertNotNull(token, "Token should not be null");
        assertTrue(token.length() > 20, "Token should be of reasonable length");
        
        // Extract username from token
        String username = jwtTokenUtil.extractUsername(token);
        assertEquals("jwtuser", username, "Username should be extracted correctly");
        
        // Create UserDetails object for validation
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .roles("USER")
                .build();

        // Validate token
        assertTrue(jwtTokenUtil.validateToken(token, userDetails), "Token should be valid");
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
        assertEquals("testuser", registrationDto.getUsername());
        assertEquals("password123", registrationDto.getPassword());
        assertEquals("test@example.com", registrationDto.getEmail());
        assertEquals("Test User", registrationDto.getDisplayName());
        
        // Test JSON serialization
        try {
            String json = objectMapper.writeValueAsString(registrationDto);
            UserRegistrationDto deserialized = objectMapper.readValue(json, UserRegistrationDto.class);
            
            assertEquals(registrationDto.getUsername(), deserialized.getUsername());
            assertEquals(registrationDto.getPassword(), deserialized.getPassword());
            assertEquals(registrationDto.getEmail(), deserialized.getEmail());
            assertEquals(registrationDto.getDisplayName(), deserialized.getDisplayName());
        } catch (Exception e) {
            fail("JSON serialization should not throw an exception: " + e.getMessage());
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
        assertEquals("testuser", authResponse.getUsername());
        assertEquals("Test User", authResponse.getDisplayName());
        assertEquals("jwt.token.here", authResponse.getToken());
        assertEquals("refresh.token.here", authResponse.getRefreshToken());
        
        // Test JSON serialization
        try {
            String json = objectMapper.writeValueAsString(authResponse);
            AuthResponse deserialized = objectMapper.readValue(json, AuthResponse.class);
            
            assertEquals(authResponse.getUsername(), deserialized.getUsername());
            assertEquals(authResponse.getDisplayName(), deserialized.getDisplayName());
            assertEquals(authResponse.getToken(), deserialized.getToken());
            assertEquals(authResponse.getRefreshToken(), deserialized.getRefreshToken());
        } catch (Exception e) {
            fail("JSON serialization should not throw an exception: " + e.getMessage());
        }
    }
}