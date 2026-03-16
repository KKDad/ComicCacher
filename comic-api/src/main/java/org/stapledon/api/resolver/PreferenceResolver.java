package org.stapledon.api.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.stapledon.api.dto.payload.MutationPayloads.FavoritePayload;
import org.stapledon.api.dto.payload.MutationPayloads.UpdateDisplaySettingsPayload;
import org.stapledon.api.dto.payload.MutationPayloads.UpdateLastReadPayload;
import org.stapledon.api.dto.preference.UserPreference;
import org.stapledon.core.preference.service.PreferenceService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @PreAuthorize("isAuthenticated()")
    public UserPreference preferences(@AuthenticationPrincipal UserDetails userDetails) {
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
                .toList();
    }

    // =========================================================================
    // Mutations
    // =========================================================================

    /**
     * Add a comic to the user's favorites.
     */
    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public FavoritePayload addFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @Argument int comicId) {

        log.info("Adding comic {} to favorites for user: {}", comicId, userDetails.getUsername());

        UserPreference pref = preferenceService.addFavorite(userDetails.getUsername(), comicId)
                .orElseThrow(() -> new RuntimeException("Failed to add favorite"));
        return new FavoritePayload(pref, List.of());
    }

    /**
     * Remove a comic from the user's favorites.
     */
    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public FavoritePayload removeFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @Argument int comicId) {

        log.info("Removing comic {} from favorites for user: {}", comicId, userDetails.getUsername());

        UserPreference pref = preferenceService.removeFavorite(userDetails.getUsername(), comicId)
                .orElseThrow(() -> new RuntimeException("Failed to remove favorite"));
        return new FavoritePayload(pref, List.of());
    }

    /**
     * Update the last read date for a comic.
     */
    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public UpdateLastReadPayload updateLastRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @Argument int comicId,
            @Argument LocalDate date) {

        log.info("Updating last read date for comic {} to {} for user: {}", comicId, date, userDetails.getUsername());

        UserPreference pref = preferenceService.updateLastRead(userDetails.getUsername(), comicId, date)
                .orElseThrow(() -> new RuntimeException("Failed to update last read date"));
        return new UpdateLastReadPayload(pref, List.of());
    }

    /**
     * Update display settings (theme, layout, etc.).
     */
    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public UpdateDisplaySettingsPayload updateDisplaySettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @Argument Map<String, Object> settings) {

        log.info("Updating display settings for user: {}", userDetails.getUsername());

        UserPreference pref = preferenceService.updateDisplaySettings(userDetails.getUsername(), new HashMap<>(settings))
                .orElseThrow(() -> new RuntimeException("Failed to update display settings"));
        return new UpdateDisplaySettingsPayload(pref, List.of());
    }

    // =========================================================================
    // Record Types for GraphQL
    // =========================================================================

    public record LastReadEntry(int comicId, LocalDate date) {
    }
}
