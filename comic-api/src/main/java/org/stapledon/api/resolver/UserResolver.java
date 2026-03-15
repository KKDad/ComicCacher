package org.stapledon.api.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.stapledon.api.dto.user.User;
import org.stapledon.core.auth.model.AuthenticationException;
import org.stapledon.core.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL resolver for User operations.
 * Implements queries and mutations defined in comics-schema.graphql.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UserResolver {

    private final UserService userService;

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Get the current authenticated user's profile.
     * Maps to the "me" query in the schema.
     */
    @QueryMapping
    public User me(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }

        log.info("Getting profile for user: {}", userDetails.getUsername());
        return userService.getUser(userDetails.getUsername())
                .orElse(null);
    }

    // =========================================================================
    // Mutations
    // =========================================================================

    /**
     * Update the current user's profile.
     */
    @MutationMapping
    public User updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Argument UpdateProfileInput input) {

        if (userDetails == null) {
            throw new AuthenticationException("Authentication required");
        }

        log.info("Updating profile for user: {}", userDetails.getUsername());

        // Get current user
        User currentUser = userService.getUser(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        // Update fields if provided
        User updatedUser = User.builder()
                .username(currentUser.getUsername())
                .email(input.email() != null ? input.email() : currentUser.getEmail())
                .displayName(input.displayName() != null ? input.displayName() : currentUser.getDisplayName())
                .created(currentUser.getCreated())
                .lastLogin(currentUser.getLastLogin())
                .roles(currentUser.getRoles())
                .build();

        return userService.updateUser(updatedUser)
                .orElseThrow(() -> new AuthenticationException("Failed to update profile"));
    }

    /**
     * Update the current user's password.
     */
    @MutationMapping
    public boolean updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Argument String newPassword) {

        if (userDetails == null) {
            throw new AuthenticationException("Authentication required");
        }

        if (newPassword == null || newPassword.isEmpty()) {
            throw new AuthenticationException("New password is required");
        }

        log.info("Updating password for user: {}", userDetails.getUsername());

        return userService.updatePassword(userDetails.getUsername(), newPassword)
                .isPresent();
    }

    /**
     * Delete a user account (admin only).
     */
    @MutationMapping
    public boolean deleteAccount(@Argument String username) {
        log.info("Deleting user account: {}", username);
        return userService.deleteUser(username);
    }

    // =========================================================================
    // Record Types for GraphQL Input
    // =========================================================================

    public record UpdateProfileInput(String email, String displayName) {
    }
}
