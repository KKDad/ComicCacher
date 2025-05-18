package org.stapledon.infrastructure.config;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.stapledon.infrastructure.config.properties.CacheProperties;
import org.stapledon.api.dto.preference.PreferenceConfig;
import org.stapledon.api.dto.preference.UserPreference;

import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PreferenceConfigWriter {
    @Qualifier("gsonWithLocalDate")
    private final Gson gson;

    private final CacheProperties cacheProperties;

    private PreferenceConfig preferenceConfig;

    /**
     * Save user preferences to the preferences.json file
     * 
     * @param preference User preferences to save
     * @return true if successful, false otherwise
     */
    public boolean savePreference(UserPreference preference) {
        try {
            loadPreferences();
            preferenceConfig.getPreferences().put(preference.getUsername(), preference);
            log.info("Saving preferences for user: {}", preference.getUsername());

            Writer writer = new FileWriter(Paths.get(cacheProperties.getLocation(), cacheProperties.getPreferencesConfig()).toFile());
            gson.toJson(preferenceConfig, writer);
            writer.flush();
            writer.close();
            return true;
        } catch (IOException e) {
            log.error("Failed to save preferences: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get user preferences by username
     * 
     * @param username Username
     * @return The user preferences if found, empty otherwise
     */
    public Optional<UserPreference> getPreference(String username) {
        try {
            loadPreferences();
            
            if (preferenceConfig.getPreferences().containsKey(username)) {
                return Optional.of(preferenceConfig.getPreferences().get(username));
            }
            
            // Create default preferences if not exists
            UserPreference newPreference = UserPreference.builder()
                    .username(username)
                    .build();
            
            preferenceConfig.getPreferences().put(username, newPreference);
            savePreference(newPreference);
            
            return Optional.of(newPreference);
        } catch (FileNotFoundException e) {
            log.error("Failed to get preferences: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Add a comic to user's favorites
     * 
     * @param username Username
     * @param comicId Comic ID
     * @return Updated user preferences if successful, empty otherwise
     */
    public Optional<UserPreference> addFavorite(String username, int comicId) {
        try {
            Optional<UserPreference> preferenceOpt = getPreference(username);
            
            if (preferenceOpt.isEmpty()) {
                return Optional.empty();
            }
            
            UserPreference preference = preferenceOpt.get();
            
            if (!preference.getFavoriteComics().contains(comicId)) {
                preference.getFavoriteComics().add(comicId);
                savePreference(preference);
            }
            
            return Optional.of(preference);
        } catch (Exception e) {
            log.error("Failed to add favorite: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Remove a comic from user's favorites
     * 
     * @param username Username
     * @param comicId Comic ID
     * @return Updated user preferences if successful, empty otherwise
     */
    public Optional<UserPreference> removeFavorite(String username, int comicId) {
        try {
            Optional<UserPreference> preferenceOpt = getPreference(username);
            
            if (preferenceOpt.isEmpty()) {
                return Optional.empty();
            }
            
            UserPreference preference = preferenceOpt.get();
            preference.getFavoriteComics().remove(Integer.valueOf(comicId));
            savePreference(preference);
            
            return Optional.of(preference);
        } catch (Exception e) {
            log.error("Failed to remove favorite: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Update last read date for a comic
     * 
     * @param username Username
     * @param comicId Comic ID
     * @param date Last read date
     * @return Updated user preferences if successful, empty otherwise
     */
    public Optional<UserPreference> updateLastRead(String username, int comicId, LocalDate date) {
        try {
            Optional<UserPreference> preferenceOpt = getPreference(username);
            
            if (preferenceOpt.isEmpty()) {
                return Optional.empty();
            }
            
            UserPreference preference = preferenceOpt.get();
            preference.getLastReadDates().put(comicId, date);
            savePreference(preference);
            
            return Optional.of(preference);
        } catch (Exception e) {
            log.error("Failed to update last read date: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Update display settings for a user
     * 
     * @param username Username
     * @param settings Display settings
     * @return Updated user preferences if successful, empty otherwise
     */
    public Optional<UserPreference> updateDisplaySettings(String username, HashMap<String, Object> settings) {
        try {
            Optional<UserPreference> preferenceOpt = getPreference(username);
            
            if (preferenceOpt.isEmpty()) {
                return Optional.empty();
            }
            
            UserPreference preference = preferenceOpt.get();
            preference.setDisplaySettings(settings);
            savePreference(preference);
            
            return Optional.of(preference);
        } catch (Exception e) {
            log.error("Failed to update display settings: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Load preferences from the preferences.json file
     * 
     * @return PreferenceConfig containing user preferences
     */
    public PreferenceConfig loadPreferences() throws FileNotFoundException {
        if (preferenceConfig != null && preferenceConfig.getPreferences() != null && !preferenceConfig.getPreferences().isEmpty()) {
            return preferenceConfig;
        }

        var preferencesFile = Paths.get(cacheProperties.getLocation(), cacheProperties.getPreferencesConfig()).toFile();
        if (preferencesFile.exists()) {
            InputStream inputStream = new FileInputStream(preferencesFile);
            Reader reader = new InputStreamReader(inputStream);

            preferenceConfig = gson.fromJson(reader, PreferenceConfig.class);
            log.info("Loaded {} user preferences from {}", preferenceConfig.getPreferences().size(), preferencesFile);
        } else {
            log.warn("{} does not exist, creating", preferencesFile);
            preferenceConfig = new PreferenceConfig();
        }
        return preferenceConfig;
    }
}