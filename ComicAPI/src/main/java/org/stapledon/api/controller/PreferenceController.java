package org.stapledon.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.model.ResponseBuilder;
import org.stapledon.api.service.PreferenceService;
import org.stapledon.dto.UserPreference;
import org.stapledon.exceptions.AuthenticationException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;

    /**
     * Get user preferences
     * 
     * @param userDetails Current authenticated user
     * @return User preferences
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserPreference>> getPreferences(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Getting preferences for user: {}", userDetails.getUsername());
        
        return preferenceService.getPreference(userDetails.getUsername())
                .map(ResponseBuilder::ok)
                .orElseThrow(() -> new AuthenticationException("Failed to get preferences"));
    }

    /**
     * Add a comic to favorites
     * 
     * @param userDetails Current authenticated user
     * @param comicId Comic ID
     * @return Updated user preferences
     */
    @PostMapping("/comics/{comicId}/favorite")
    public ResponseEntity<ApiResponse<UserPreference>> addFavorite(
            @AuthenticationPrincipal UserDetails userDetails, 
            @PathVariable int comicId) {
        log.info("Adding comic {} to favorites for user: {}", comicId, userDetails.getUsername());
        
        return preferenceService.addFavorite(userDetails.getUsername(), comicId)
                .map(ResponseBuilder::ok)
                .orElseThrow(() -> new AuthenticationException("Failed to add favorite"));
    }

    /**
     * Remove a comic from favorites
     * 
     * @param userDetails Current authenticated user
     * @param comicId Comic ID
     * @return Updated user preferences
     */
    @DeleteMapping("/comics/{comicId}/favorite")
    public ResponseEntity<ApiResponse<UserPreference>> removeFavorite(
            @AuthenticationPrincipal UserDetails userDetails, 
            @PathVariable int comicId) {
        log.info("Removing comic {} from favorites for user: {}", comicId, userDetails.getUsername());
        
        return preferenceService.removeFavorite(userDetails.getUsername(), comicId)
                .map(ResponseBuilder::ok)
                .orElseThrow(() -> new AuthenticationException("Failed to remove favorite"));
    }

    /**
     * Update last read date for a comic
     * 
     * @param userDetails Current authenticated user
     * @param comicId Comic ID
     * @param dateData Map containing date
     * @return Updated user preferences
     */
    @PostMapping("/comics/{comicId}/lastread")
    public ResponseEntity<ApiResponse<UserPreference>> updateLastRead(
            @AuthenticationPrincipal UserDetails userDetails, 
            @PathVariable int comicId,
            @RequestBody Map<String, String> dateData) {
        log.info("Updating last read date for comic {} for user: {}", comicId, userDetails.getUsername());
        
        String dateStr = dateData.get("date");
        
        if (dateStr == null || dateStr.isEmpty()) {
            throw new IllegalArgumentException("Date is required");
        }
        
        LocalDate date = LocalDate.parse(dateStr);
        
        return preferenceService.updateLastRead(userDetails.getUsername(), comicId, date)
                .map(ResponseBuilder::ok)
                .orElseThrow(() -> new AuthenticationException("Failed to update last read date"));
    }

    /**
     * Update display settings
     * 
     * @param userDetails Current authenticated user
     * @param settings Display settings
     * @return Updated user preferences
     */
    @PostMapping("/display-settings")
    public ResponseEntity<ApiResponse<UserPreference>> updateDisplaySettings(
            @AuthenticationPrincipal UserDetails userDetails, 
            @RequestBody HashMap<String, Object> settings) {
        log.info("Updating display settings for user: {}", userDetails.getUsername());
        
        return preferenceService.updateDisplaySettings(userDetails.getUsername(), settings)
                .map(ResponseBuilder::ok)
                .orElseThrow(() -> new AuthenticationException("Failed to update display settings"));
    }
}