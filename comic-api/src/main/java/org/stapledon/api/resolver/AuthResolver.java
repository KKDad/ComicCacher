package org.stapledon.api.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.stapledon.api.dto.auth.AuthRequest;
import org.stapledon.api.dto.auth.AuthResponse;
import org.stapledon.api.dto.user.UserRegistrationDto;
import org.stapledon.core.auth.model.AuthenticationException;
import org.stapledon.core.auth.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL resolver for authentication operations. Handles user registration, login, token refresh, and validation.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthResolver {

    private final AuthService authService;

    /**
     * Register a new user account.
     *
     * @param input Registration input containing username, password, email, and display name
     * @return Authentication payload with JWT tokens
     */
    @MutationMapping
    public AuthResponse register(@Argument("input") RegisterInput input) {
        log.info("GraphQL: User registration request for: {}", input.username());

        UserRegistrationDto registrationDto = UserRegistrationDto.builder().username(input.username()).password(input.password()).email(input.email()).displayName(input.displayName())
                .build();

        return authService.register(registrationDto).orElseThrow(() -> new AuthenticationException("User registration failed"));
    }

    /**
     * Authenticate with username and password.
     *
     * @param input Login input containing username and password
     * @return Authentication payload with JWT tokens
     */
    @MutationMapping
    public AuthResponse login(@Argument("input") LoginInput input) {
        log.info("GraphQL: Login request for user: {}", input.username());

        AuthRequest authRequest = new AuthRequest(input.username(), input.password());

        return authService.login(authRequest).orElseThrow(() -> new AuthenticationException("Authentication failed"));
    }

    /**
     * Refresh an expired JWT token using a refresh token.
     *
     * @param refreshToken The refresh token
     * @return Authentication payload with new JWT tokens
     */
    @MutationMapping
    public AuthResponse refreshToken(@Argument String refreshToken) {
        log.info("GraphQL: Token refresh request");

        return authService.refreshToken(refreshToken).orElseThrow(() -> new AuthenticationException("Invalid refresh token"));
    }

    /**
     * Validate the current JWT token from the security context.
     *
     * @return true if the token is valid, false otherwise
     */
    @QueryMapping
    public boolean validateToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("GraphQL: No valid authentication found");
            return false;
        }

        // If we have an authenticated principal, the token is valid
        boolean isValid = authentication.getPrincipal() != null && !"anonymousUser".equals(authentication.getPrincipal());

        log.debug("GraphQL: Token validation result: {}", isValid);
        return isValid;
    }

    /**
     * Request a password reset email.
     * Always returns true to prevent email enumeration.
     */
    @MutationMapping
    public boolean forgotPassword(@Argument String email) {
        log.info("GraphQL: Password reset requested for email: {}", email);
        authService.forgotPassword(email);
        return true;
    }

    /**
     * Reset password using a token from the password reset email.
     */
    @MutationMapping
    public AuthResponse resetPassword(@Argument String token, @Argument String newPassword) {
        log.info("GraphQL: Password reset with token");
        return authService.resetPassword(token, newPassword)
                .orElseThrow(() -> new AuthenticationException("Password reset failed"));
    }

    /**
     * Input record for user registration.
     */
    public record RegisterInput(String username, String password, String email, String displayName) {
    }

    /**
     * Input record for user login.
     */
    public record LoginInput(String username, String password) {
    }
}
