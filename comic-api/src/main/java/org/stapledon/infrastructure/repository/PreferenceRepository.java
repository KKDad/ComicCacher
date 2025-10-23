package org.stapledon.infrastructure.repository;

import org.stapledon.api.dto.preference.PreferenceConfig;
import org.stapledon.api.dto.preference.UserPreference;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User Preference persistence operations.
 * Abstracts the underlying storage mechanism (JSON files, database, etc.)
 * from the business logic layer.
 */
public interface PreferenceRepository {

    /**
     * Loads the complete preference configuration containing all user preferences.
     */
    PreferenceConfig loadPreferences();

    /**
     * Saves the complete preference configuration.
     */
    void savePreferences(PreferenceConfig config);

    /**
     * Finds preferences for a specific user by their username.
     */
    Optional<UserPreference> findByUsername(String username);

    /**
     * Retrieves all user preferences in the repository.
     */
    List<UserPreference> findAllPreferences();

    /**
     * Saves or updates preferences for a single user.
     * If preferences already exist for the user, they will be updated.
     */
    void savePreference(UserPreference preference);

    /**
     * Deletes preferences for a user by their username.
     */
    void deletePreference(String username);

    /**
     * Checks if preferences exist for the given username.
     */
    boolean existsByUsername(String username);

    /**
     * Adds a comic to the user's favorites.
     */
    void addFavoriteComic(String username, int comicId);

    /**
     * Removes a comic from the user's favorites.
     */
    void removeFavoriteComic(String username, int comicId);

    /**
     * Updates the last read date for a specific comic.
     */
    void updateLastReadDate(String username, int comicId, LocalDate date);
}
