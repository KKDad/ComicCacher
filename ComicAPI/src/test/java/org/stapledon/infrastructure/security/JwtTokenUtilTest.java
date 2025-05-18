package org.stapledon.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.core.userdetails.UserDetails;
import org.stapledon.infrastructure.config.properties.JwtProperties;
import org.stapledon.api.dto.auth.JwtTokenDto;
import org.stapledon.api.dto.user.User;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtTokenUtilTest {

    private JwtProperties jwtProperties;
    private UserDetails userDetails;

    private JwtTokenUtil jwtTokenUtil;

    @BeforeEach
    void setUp() {
        // Setup mock properties
        jwtProperties = mock(JwtProperties.class);
        when(jwtProperties.getSecret()).thenReturn("testSecretKeyWithAtLeast32Characters0123456789");
        when(jwtProperties.getExpiration()).thenReturn(900000L); // 15 minutes
        when(jwtProperties.getRefreshExpiration()).thenReturn(86400000L); // 24 hours

        jwtTokenUtil = new JwtTokenUtil(jwtProperties);
    }

    @Test
    void generateTokenShouldCreateValidToken() {
        // Given
        User user = createTestUser("testuser");

        // When
        String token = jwtTokenUtil.generateToken(user);

        // Then
        assertNotNull(token);
        assertEquals("testuser", jwtTokenUtil.extractUsername(token));
        
        List<String> roles = jwtTokenUtil.extractRoles(token);
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals("USER", roles.get(0));
    }

    @Test
    void generateRefreshTokenShouldCreateValidToken() {
        // Given
        User user = createTestUser("testuser");

        // When
        String refreshToken = jwtTokenUtil.generateRefreshToken(user);

        // Then
        assertNotNull(refreshToken);
        assertEquals("testuser", jwtTokenUtil.extractUsername(refreshToken));
    }

    @Test
    void createTokenShouldReturnTokenDtoWithCorrectValues() {
        // Given
        User user = createTestUser("testuser");

        // When
        JwtTokenDto tokenDto = jwtTokenUtil.createToken(user);

        // Then
        assertNotNull(tokenDto);
        assertEquals("testuser", tokenDto.getUsername());
        assertNotNull(tokenDto.getToken());
        assertNotNull(tokenDto.getIssuedAt());
        assertNotNull(tokenDto.getExpiresAt());
    }

    @Test
    void validateTokenShouldReturnTrueForValidToken() {
        // Given
        User user = createTestUser("testuser");
        String token = jwtTokenUtil.generateToken(user);

        userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("testuser");

        // When
        boolean isValid = jwtTokenUtil.validateToken(token, userDetails);

        // Then
        assertTrue(isValid);
    }

    @Test
    void validateTokenShouldReturnFalseForTokenWithDifferentUsername() {
        // Given
        User user = createTestUser("testuser");
        String token = jwtTokenUtil.generateToken(user);

        userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("differentuser");

        // When
        boolean isValid = jwtTokenUtil.validateToken(token, userDetails);

        // Then
        assertFalse(isValid);
    }

    @Test
    void extractExpirationShouldReturnExpirationDate() {
        // Given
        User user = createTestUser("testuser");
        String token = jwtTokenUtil.generateToken(user);

        // When
        Date expiration = jwtTokenUtil.extractExpiration(token);

        // Then
        assertNotNull(expiration);
        // Expiration should be in the future
        assertTrue(expiration.after(new Date()));
    }

    private User createTestUser(String username) {
        return User.builder()
                .username(username)
                .passwordHash("hashedPassword")
                .email(username + "@example.com")
                .displayName("Test " + username)
                .created(LocalDateTime.now())
                .roles(Arrays.asList("USER"))
                .userToken(UUID.randomUUID())
                .build();
    }
}