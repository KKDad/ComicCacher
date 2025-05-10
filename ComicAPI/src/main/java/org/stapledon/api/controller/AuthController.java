package org.stapledon.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.model.ResponseBuilder;
import org.stapledon.api.service.AuthService;
import org.stapledon.dto.AuthRequest;
import org.stapledon.dto.AuthResponse;
import org.stapledon.dto.UserRegistrationDto;
import org.stapledon.exceptions.AuthenticationException;

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
        
        return authService.refreshToken(refreshToken)
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
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestHeader("Authorization") String bearerToken) {
        log.info("Token validation request");
        
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            boolean valid = authService.validateToken(token);
            return ResponseBuilder.ok(valid);
        }
        
        return ResponseBuilder.ok(false);
    }
}