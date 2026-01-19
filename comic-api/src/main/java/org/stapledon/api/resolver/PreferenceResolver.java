package org.stapledon.api.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.stapledon.api.dto.preference.UserPreference;
import org.stapledon.core.auth.model.AuthenticationException;
import org.stapledon.core.preference.service.PreferenceService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL resolver for User Preference operations.
 * Implements queries and mutations for favorites and display settings.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PreferenceResolver {

    private final PreferenceService preferenceService;

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Get the current authenticated user's preferences.
     * Maps to the "preferences" query in the schema.
     */
    @QueryMapping
    public UserPreference preferences(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationException("Authentication required");
        }

        log.info("Getting preferences for user: {}", userDetails.getUsername());
        return preferenceService.getPreference(userDetails.getUsername())
                .orElse(null);
    }

    // =========================================================================
    // Schema Mappings - Type Conversions
    // =========================================================================

    /**
     * Convert Map<Integer, LocalDate> to List<LastReadEntry> for GraphQL.
     */
    @SchemaMapping(typeName = "UserPreference", field = "lastReadDates")
    public List<LastReadEntry> lastReadDates(UserPreference preference) {
        if (preference.getLastReadDates() == null) {
            return List.of();
        }
        return preference.getLastReadDates().entrySet().stream()
                .map(e -> new LastReadEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Mutations
    // =========================================================================

    /**
     * Add a comic to the user's favorites.
     */
    @MutationMapping
    public UserPreference addFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @Argument int comicId) {

        if (userDetails == null) {
            throw new AuthenticationException("Authentication required");
        }

        log.info("Adding comic {} to favorites for user: {}", comicId, userDetails.getUsername());

        return preferenceService.addFavorite(userDetails.getUsername(), comicId)
                .orElseThrow(() -> new RuntimeException("Failed to add favorite"));
    }

    /**
     * Remove a comic from the user's favorites.
     */
    @MutationMapping
    public UserPreference removeFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @Argument int comicId) {

        if (userDetails == null) {
            throw new AuthenticationException("Authentication required");
        }

        log.info("Removing comic {} from favorites for user: {}", comicId, userDetails.getUsername());

        return preferenceService.removeFavorite(userDetails.getUsername(), comicId)
                .orElseThrow(() -> new RuntimeException("Failed to remove favorite"));
    }

    /**
     * Update the last read date for a comic.
     */
    @MutationMapping
    public UserPreference updateLastRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @Argument int comicId,
            @Argument LocalDate date) {

        if (userDetails == null) {
            throw new AuthenticationException("Authentication required");
        }

        log.info("Updating last read date for comic {} to {} for user: {}", comicId, date, userDetails.getUsername());

        return preferenceService.updateLastRead(userDetails.getUsername(), comicId, date)
                .orElseThrow(() -> new RuntimeException("Failed to update last read date"));
    }

    /**
     * Update display settings (theme, layout, etc.).
     */
    @MutationMapping
    @SuppressWarnings("unchecked")
    public UserPreference updateDisplaySettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @Argument Map<String, Object> settings) {

        if (userDetails == null) {
            throw new AuthenticationException("Authentication required");
        }

        log.info("Updating display settings for user: {}", userDetails.getUsername());

        return preferenceService.updateDisplaySettings(userDetails.getUsername(), new HashMap<>(settings))
                .orElseThrow(() -> new RuntimeException("Failed to update display settings"));
    }

    // =========================================================================
    // Record Types for GraphQL
    // =========================================================================

    public record LastReadEntry(int comicId, LocalDate date) {
    }
}
