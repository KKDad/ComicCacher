package org.stapledon.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stapledon.config.PreferenceConfigWriter;
import org.stapledon.dto.UserPreference;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceServiceImpl implements PreferenceService {

    private final PreferenceConfigWriter preferenceConfigWriter;

    @Override
    public Optional<UserPreference> getPreference(String username) {
        log.info("Getting preferences for user: {}", username);
        return preferenceConfigWriter.getPreference(username);
    }

    @Override
    public Optional<UserPreference> addFavorite(String username, int comicId) {
        log.info("Adding comic {} to favorites for user: {}", comicId, username);
        return preferenceConfigWriter.addFavorite(username, comicId);
    }

    @Override
    public Optional<UserPreference> removeFavorite(String username, int comicId) {
        log.info("Removing comic {} from favorites for user: {}", comicId, username);
        return preferenceConfigWriter.removeFavorite(username, comicId);
    }

    @Override
    public Optional<UserPreference> updateLastRead(String username, int comicId, LocalDate date) {
        log.info("Updating last read date for comic {} for user: {}", comicId, username);
        return preferenceConfigWriter.updateLastRead(username, comicId, date);
    }

    @Override
    public Optional<UserPreference> updateDisplaySettings(String username, HashMap<String, Object> settings) {
        log.info("Updating display settings for user: {}", username);
        return preferenceConfigWriter.updateDisplaySettings(username, settings);
    }
}