package org.stapledon.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.stapledon.config.properties.JwtProperties;
import org.stapledon.dto.JwtTokenDto;
import org.stapledon.dto.User;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.xml.bind.DatatypeConverter;

@Slf4j
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
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(SignatureAlgorithm.HS256, getSigningKey())
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
                .setSubject(user.getUsername())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(SignatureAlgorithm.HS256, getSigningKey())
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
     * @param token JWT token
     * @param userDetails User details
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
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
     * @return List of roles
     */
    @SuppressWarnings("unchecked")
    public java.util.List<String> extractRoles(String token) {
        final Claims claims = extractAllClaims(token);
        return (java.util.List<String>) claims.get("roles");
    }

    /**
     * Extract claim from token
     *
     * @param token JWT token
     * @param claimsResolver Claims resolver
     * @return Claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
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
                .setSigningKey(getSigningKey())
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Get signing key from secret
     *
     * @return Signing key
     */
    private byte[] getSigningKey() {
        return jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
    }
}