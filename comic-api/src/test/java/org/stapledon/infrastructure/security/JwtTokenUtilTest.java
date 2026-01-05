package org.stapledon.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.stapledon.api.dto.auth.JwtTokenDto;
import org.stapledon.api.dto.user.User;
import org.stapledon.infrastructure.config.properties.JwtProperties;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

class JwtTokenUtilTest {

    private UserDetails userDetails;

    private JwtTokenUtil jwtTokenUtil;

    @BeforeEach
    void setUp() {
        // Setup mock properties
        JwtProperties jwtProperties = mock(JwtProperties.class);
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
        assertThat(token).isNotNull();
        assertThat(jwtTokenUtil.extractUsername(token)).isEqualTo("testuser");

        List<String> roles = jwtTokenUtil.extractRoles(token);
        assertThat(roles).isNotNull();
        assertThat(roles.size()).isEqualTo(1);
        assertThat(roles.get(0)).isEqualTo("USER");
    }

    @Test
    void generateRefreshTokenShouldCreateValidToken() {
        // Given
        User user = createTestUser("testuser");

        // When
        String refreshToken = jwtTokenUtil.generateRefreshToken(user);

        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(jwtTokenUtil.extractUsername(refreshToken)).isEqualTo("testuser");
    }

    @Test
    void createTokenShouldReturnTokenDtoWithCorrectValues() {
        // Given
        User user = createTestUser("testuser");

        // When
        JwtTokenDto tokenDto = jwtTokenUtil.createToken(user);

        // Then
        assertThat(tokenDto).isNotNull();
        assertThat(tokenDto.getUsername()).isEqualTo("testuser");
        assertThat(tokenDto.getToken()).isNotNull();
        assertThat(tokenDto.getIssuedAt()).isNotNull();
        assertThat(tokenDto.getExpiresAt()).isNotNull();
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
        assertThat(isValid).isTrue();
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
        assertThat(isValid).isFalse();
    }

    @Test
    void extractExpirationShouldReturnExpirationDate() {
        // Given
        User user = createTestUser("testuser");
        String token = jwtTokenUtil.generateToken(user);

        // When
        Date expiration = jwtTokenUtil.extractExpiration(token);

        // Then
        assertThat(expiration).isNotNull();
        // Expiration should be in the future
        assertThat(expiration.after(new Date())).isTrue();
    }

    private User createTestUser(String username) {
        return User.builder()
                .username(username)
                .passwordHash("hashedPassword")
                .email(username + "@example.com")
                .displayName("Test " + username)
                .created(LocalDateTime.now())
                .roles(List.of("USER"))
                .userToken(UUID.randomUUID())
                .build();
    }
}
