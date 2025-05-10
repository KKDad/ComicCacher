package org.stapledon.api.service;

import org.stapledon.dto.AuthRequest;
import org.stapledon.dto.AuthResponse;
import org.stapledon.dto.UserRegistrationDto;

import java.util.Optional;

public interface AuthService {

    /**
     * Register a new user
     * 
     * @param registrationDto User registration data
     * @return Authentication response with token if successful, empty otherwise
     */
    Optional<AuthResponse> register(UserRegistrationDto registrationDto);

    /**
     * Authenticate a user
     * 
     * @param authRequest Authentication request containing username and password
     * @return Authentication response with token if successful, empty otherwise
     */
    Optional<AuthResponse> login(AuthRequest authRequest);

    /**
     * Refresh token
     * 
     * @param refreshToken Refresh token
     * @return Authentication response with new token if successful, empty otherwise
     */
    Optional<AuthResponse> refreshToken(String refreshToken);

    /**
     * Validate token
     * 
     * @param token JWT token
     * @return true if token is valid, false otherwise
     */
    boolean validateToken(String token);
}