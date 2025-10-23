package org.stapledon.infrastructure.repository;

import org.springframework.stereotype.Repository;
import org.stapledon.api.dto.preference.PreferenceConfig;
import org.stapledon.api.dto.preference.UserPreference;
import org.stapledon.infrastructure.config.ApplicationConfigurationFacade;
import org.stapledon.infrastructure.config.PreferenceConfigWriter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON file-based implementation of PreferenceRepository.
 * Delegates to ApplicationConfigurationFacade and PreferenceConfigWriter for file I/O operations.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JsonPreferenceRepository implements PreferenceRepository {

    private final ApplicationConfigurationFacade configurationFacade;
    private final PreferenceConfigWriter preferenceConfigWriter;

    @Override
    public PreferenceConfig loadPreferences() {
        return configurationFacade.loadPreferenceConfig();
    }

    @Override
    public void savePreferences(PreferenceConfig config) {
        boolean success = configurationFacade.savePreferenceConfig(config);
        if (!success) {
            log.error("Failed to save preference configuration");
        }
    }

    @Override
    public Optional<UserPreference> findByUsername(String username) {
        return preferenceConfigWriter.getPreference(username);
    }

    @Override
    public List<UserPreference> findAllPreferences() {
        PreferenceConfig config = loadPreferences();
        if (config.getPreferences() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(config.getPreferences().values());
    }

    @Override
    public void savePreference(UserPreference preference) {
        boolean success = preferenceConfigWriter.savePreference(preference);
        if (!success) {
            log.error("Failed to save preference for user: {}", preference.getUsername());
        }
    }

    @Override
    public void deletePreference(String username) {
        PreferenceConfig config = loadPreferences();
        if (config.getPreferences() != null) {
            config.getPreferences().remove(username);
            savePreferences(config);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    @Override
    public void addFavoriteComic(String username, int comicId) {
        Optional<UserPreference> preferenceOpt = preferenceConfigWriter.addFavorite(username, comicId);
        if (preferenceOpt.isEmpty()) {
            log.error("Failed to add favorite comic {} for user {}", comicId, username);
        }
    }

    @Override
    public void removeFavoriteComic(String username, int comicId) {
        Optional<UserPreference> preferenceOpt = preferenceConfigWriter.removeFavorite(username, comicId);
        if (preferenceOpt.isEmpty()) {
            log.error("Failed to remove favorite comic {} for user {}", comicId, username);
        }
    }

    @Override
    public void updateLastReadDate(String username, int comicId, LocalDate date) {
        Optional<UserPreference> preferenceOpt = preferenceConfigWriter.updateLastRead(username, comicId, date);
        if (preferenceOpt.isEmpty()) {
            log.error("Failed to update last read date for comic {} for user {}", comicId, username);
        }
    }
}
