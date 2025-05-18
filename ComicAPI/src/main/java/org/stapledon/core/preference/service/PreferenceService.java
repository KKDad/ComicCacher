package org.stapledon.core.preference.service;

import org.stapledon.api.dto.preference.UserPreference;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Optional;

public interface PreferenceService {

    /**
     * Get user preferences by username
     * 
     * @param username Username
     * @return The user preferences if found, empty otherwise
     */
    Optional<UserPreference> getPreference(String username);

    /**
     * Add a comic to user's favorites
     * 
     * @param username Username
     * @param comicId Comic ID
     * @return Updated user preferences if successful, empty otherwise
     */
    Optional<UserPreference> addFavorite(String username, int comicId);

    /**
     * Remove a comic from user's favorites
     * 
     * @param username Username
     * @param comicId Comic ID
     * @return Updated user preferences if successful, empty otherwise
     */
    Optional<UserPreference> removeFavorite(String username, int comicId);

    /**
     * Update last read date for a comic
     * 
     * @param username Username
     * @param comicId Comic ID
     * @param date Last read date
     * @return Updated user preferences if successful, empty otherwise
     */
    Optional<UserPreference> updateLastRead(String username, int comicId, LocalDate date);

    /**
     * Update display settings for a user
     * 
     * @param username Username
     * @param settings Display settings
     * @return Updated user preferences if successful, empty otherwise
     */
    Optional<UserPreference> updateDisplaySettings(String username, HashMap<String, Object> settings);
}