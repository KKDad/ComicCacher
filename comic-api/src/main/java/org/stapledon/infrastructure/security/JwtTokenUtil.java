package org.stapledon.infrastructure.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.stapledon.api.dto.auth.JwtTokenDto;
import org.stapledon.api.dto.user.User;
import org.stapledon.infrastructure.config.properties.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@Component
@RequiredArgsConstructor
public class JwtTokenUtil {

    private final JwtProperties jwtProperties;

    /**
     * Generate a JWT token for a user
     *
     * @param user User to generate token for
     * @return JWT token
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles());

        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtProperties.getExpiration());

        return Jwts.builder()
                .claims().add(claims).and()
                .subject(user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generate a refresh token for a user
     *
     * @param user User to generate refresh token for
     * @return Refresh token
     */
    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtProperties.getRefreshExpiration());

        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Create JWT token DTO with token details
     *
     * @param user User to create token for
     * @return JWT token DTO
     */
    public JwtTokenDto createToken(User user) {
        String token = generateToken(user);
        String refreshToken = generateRefreshToken(user);

        return JwtTokenDto.builder()
                .token(token)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusMillis(jwtProperties.getExpiration()))
                .build();
    }

    /**
     * Validate token against user details
     *
     * @param token       JWT token
     * @param userDetails User details
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Extract username from token
     *
     * @param token JWT token
     * @return Username
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from token
     *
     * @param token JWT token
     * @return Expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract roles from token
     *
     * @param token JWT token
     * @return List of roles (empty list if no roles found)
     */
    @SuppressWarnings("unchecked")
    public java.util.List<String> extractRoles(String token) {
        final Claims claims = extractAllClaims(token);
        Object roles = claims.get("roles");
        return roles != null ? (java.util.List<String>) roles : java.util.Collections.emptyList();
    }

    /**
     * Extract claim from token
     *
     * @param token          JWT token
     * @param claimsResolver Claims resolver
     * @return Claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private static final String PURPOSE_CLAIM = "purpose";
    private static final String PASSWORD_RESET_PURPOSE = "password_reset";
    private static final long PASSWORD_RESET_EXPIRATION_MS = 15 * 60 * 1000L; // 15 minutes

    /**
     * Generate a short-lived password reset token.
     */
    public String generatePasswordResetToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(PURPOSE_CLAIM, PASSWORD_RESET_PURPOSE);

        Instant now = Instant.now();
        Instant expiration = now.plusMillis(PASSWORD_RESET_EXPIRATION_MS);

        return Jwts.builder()
                .claims().add(claims).and()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validate a password reset token and extract the username.
     * Returns empty if the token is invalid, expired, or not a password reset token.
     */
    public java.util.Optional<String> validatePasswordResetToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String purpose = claims.get(PURPOSE_CLAIM, String.class);
            if (!PASSWORD_RESET_PURPOSE.equals(purpose)) {
                log.warn("Token is not a password reset token");
                return java.util.Optional.empty();
            }
            if (isTokenExpired(token)) {
                log.warn("Password reset token has expired");
                return java.util.Optional.empty();
            }
            return java.util.Optional.ofNullable(claims.getSubject());
        } catch (Exception e) {
            log.error("Invalid password reset token: {}", e.getMessage());
            return java.util.Optional.empty();
        }
    }

    /**
     * Check if token is expired
     *
     * @param token JWT token
     * @return true if expired, false otherwise
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extract all claims from token
     *
     * @param token JWT token
     * @return Claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get signing key from secret
     *
     * @return Signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}