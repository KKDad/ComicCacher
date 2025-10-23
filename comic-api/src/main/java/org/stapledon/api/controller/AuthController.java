package org.stapledon.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stapledon.api.dto.auth.AuthRequest;
import org.stapledon.api.dto.auth.AuthResponse;
import org.stapledon.api.dto.user.UserRegistrationDto;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.model.ResponseBuilder;
import org.stapledon.core.auth.model.AuthenticationException;
import org.stapledon.core.auth.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user
     * 
     * @param registrationDto User registration data
     * @return Authentication response with token
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody UserRegistrationDto registrationDto) {
        log.info("User registration request for: {}", registrationDto.getUsername());
        
        return authService.register(registrationDto)
                .map(ResponseBuilder::created)
                .orElseThrow(() -> new AuthenticationException("User registration failed"));
    }

    /**
     * Authenticate a user
     * 
     * @param authRequest Authentication request containing username and password
     * @return Authentication response with token
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody AuthRequest authRequest) {
        log.info("Login request for user: {}", authRequest.getUsername());
        
        return authService.login(authRequest)
                .map(ResponseBuilder::ok)
                .orElseThrow(() -> new AuthenticationException("Authentication failed"));
    }

    /**
     * Refresh token
     * 
     * @param refreshToken Refresh token
     * @return Authentication response with new token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody String refreshToken) {
        log.info("Token refresh request");
        
        // Remove quotes if present (fixes issue with direct string body)
        String token = refreshToken;
        if (token.startsWith("\"") && token.endsWith("\"")) {
            token = token.substring(1, token.length() - 1);
        }
        
        return authService.refreshToken(token)
                .map(ResponseBuilder::ok)
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));
    }

    /**
     * Validate token
     * 
     * @param token JWT token
     * @return true if token is valid
     */
    @PostMapping("/validate-token")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestHeader(value = "Authorization", required = false) String bearerToken) {
        log.info("Token validation request");
        
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            boolean valid = authService.validateToken(token);
            return ResponseBuilder.ok(valid);
        }
        
        return ResponseBuilder.ok(false);
    }
}