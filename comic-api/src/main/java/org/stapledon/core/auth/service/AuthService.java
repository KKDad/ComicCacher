package org.stapledon.core.auth.service;

import org.stapledon.api.dto.auth.AuthRequest;
import org.stapledon.api.dto.auth.AuthResponse;
import org.stapledon.api.dto.user.UserRegistrationDto;

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

    /**
     * Invalidate all tokens previously issued to a user.
     * Sets the user's tokensInvalidatedBefore timestamp so that subsequent token
     * validation (including refresh) rejects any token issued before the call.
     *
     * @param username Username whose tokens should be invalidated
     */
    void logout(String username);

    /**
     * Initiate a password reset by sending a reset email.
     * Always succeeds silently to prevent email enumeration.
     */
    void forgotPassword(String email);

    /**
     * Reset a user's password using a password-reset token.
     */
    Optional<AuthResponse> resetPassword(String token, String newPassword);
}