package org.stapledon.infrastructure.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.infrastructure.config.properties.CacheProperties;
import org.stapledon.api.dto.preference.PreferenceConfig;
import org.stapledon.api.dto.preference.UserPreference;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreferenceConfigWriterTest {

    @TempDir
    Path tempDir;

    @Mock
    private CacheProperties cacheProperties;

    private PreferenceConfigWriter preferenceConfigWriter;
    private Gson gson;
    private File preferencesFile;

    @BeforeEach
    void setUp() {
        // Setup Gson with adapters for LocalDate
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();

        // Create temp file for preferences
        preferencesFile = tempDir.resolve("preferences.json").toFile();

        // Configure mock properties
        when(cacheProperties.getLocation()).thenReturn(tempDir.toString());
        when(cacheProperties.getPreferencesConfig()).thenReturn(preferencesFile.getName());

        // Create the PreferenceConfigWriter with mocked dependencies
        preferenceConfigWriter = new PreferenceConfigWriter(gson, cacheProperties);
    }

    @Test
    void loadPreferencesShouldCreateEmptyConfigWhenFileDoesNotExist() throws Exception {
        // When
        PreferenceConfig result = preferenceConfigWriter.loadPreferences();

        // Then
        assertNotNull(result);
        assertNotNull(result.getPreferences());
        assertTrue(result.getPreferences().isEmpty());
    }

    @Test
    void loadPreferencesShouldLoadExistingPreferencesFromFile() throws Exception {
        // Given
        PreferenceConfig initialConfig = new PreferenceConfig();
        UserPreference preference = createTestPreference("testuser");
        initialConfig.getPreferences().put(preference.getUsername(), preference);
        
        try (FileWriter writer = new FileWriter(preferencesFile)) {
            gson.toJson(initialConfig, writer);
        }

        // When
        PreferenceConfig result = preferenceConfigWriter.loadPreferences();

        // Then
        assertNotNull(result);
        assertNotNull(result.getPreferences());
        assertEquals(1, result.getPreferences().size());
        assertTrue(result.getPreferences().containsKey("testuser"));
        assertEquals("testuser", result.getPreferences().get("testuser").getUsername());
    }

    @Test
    void getPreferenceShouldCreateDefaultPreferenceForNewUser() {
        // Given
        String username = "newuser";

        // When
        Optional<UserPreference> result = preferenceConfigWriter.getPreference(username);

        // Then
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        assertNotNull(result.get().getFavoriteComics());
        assertTrue(result.get().getFavoriteComics().isEmpty());
        assertNotNull(result.get().getLastReadDates());
        assertTrue(result.get().getLastReadDates().isEmpty());
        assertNotNull(result.get().getDisplaySettings());
        assertTrue(result.get().getDisplaySettings().isEmpty());
    }

    @Test
    void getPreferenceShouldReturnExistingPreference() {
        // Given
        UserPreference preference = createTestPreference("existinguser");
        preferenceConfigWriter.savePreference(preference);

        // When
        Optional<UserPreference> result = preferenceConfigWriter.getPreference("existinguser");

        // Then
        assertTrue(result.isPresent());
        assertEquals("existinguser", result.get().getUsername());
        assertEquals(2, result.get().getFavoriteComics().size());
        assertTrue(result.get().getFavoriteComics().contains(123));
        assertTrue(result.get().getFavoriteComics().contains(456));
    }

    @Test
    void addFavoriteShouldAddComicToFavorites() {
        // Given
        String username = "favoriteuser";
        int comicId = 789;
        
        // First make sure user has preferences
        preferenceConfigWriter.getPreference(username);

        // When
        Optional<UserPreference> result = preferenceConfigWriter.addFavorite(username, comicId);

        // Then
        assertTrue(result.isPresent());
        assertTrue(result.get().getFavoriteComics().contains(comicId));
    }

    @Test
    void addFavoriteShouldNotAddDuplicateComic() {
        // Given
        String username = "duplicateuser";
        int comicId = 789;
        
        // First add the comic
        preferenceConfigWriter.getPreference(username);
        preferenceConfigWriter.addFavorite(username, comicId);
        
        // When adding it again
        Optional<UserPreference> beforeResult = preferenceConfigWriter.getPreference(username);
        int beforeSize = beforeResult.get().getFavoriteComics().size();
        
        Optional<UserPreference> result = preferenceConfigWriter.addFavorite(username, comicId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(beforeSize, result.get().getFavoriteComics().size());
        assertTrue(result.get().getFavoriteComics().contains(comicId));
    }

    @Test
    void removeFavoriteShouldRemoveComicFromFavorites() {
        // Given
        String username = "removeuser";
        int comicId = 789;
        
        // First add the comic
        preferenceConfigWriter.getPreference(username);
        preferenceConfigWriter.addFavorite(username, comicId);
        
        // When
        Optional<UserPreference> result = preferenceConfigWriter.removeFavorite(username, comicId);
        
        // Then
        assertTrue(result.isPresent());
        assertFalse(result.get().getFavoriteComics().contains(comicId));
    }

    @Test
    void updateLastReadShouldUpdateComicReadDate() {
        // Given
        String username = "readuser";
        int comicId = 101;
        LocalDate date = LocalDate.now();
        
        // First ensure user has preferences
        preferenceConfigWriter.getPreference(username);
        
        // When
        Optional<UserPreference> result = preferenceConfigWriter.updateLastRead(username, comicId, date);
        
        // Then
        assertTrue(result.isPresent());
        assertTrue(result.get().getLastReadDates().containsKey(comicId));
        assertEquals(date, result.get().getLastReadDates().get(comicId));
    }

    @Test
    void updateDisplaySettingsShouldUpdateUserSettings() {
        // Given
        String username = "settingsuser";
        HashMap<String, Object> settings = new HashMap<>();
        settings.put("darkMode", true);
        settings.put("fontSize", 14);
        
        // First ensure user has preferences
        preferenceConfigWriter.getPreference(username);
        
        // When
        Optional<UserPreference> result = preferenceConfigWriter.updateDisplaySettings(username, settings);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(2, result.get().getDisplaySettings().size());
        assertEquals(true, result.get().getDisplaySettings().get("darkMode"));
        assertEquals(14, result.get().getDisplaySettings().get("fontSize"));
    }

    private UserPreference createTestPreference(String username) {
        HashMap<String, Object> settings = new HashMap<>();
        settings.put("theme", "light");
        
        HashMap<Integer, LocalDate> lastReadDates = new HashMap<>();
        lastReadDates.put(123, LocalDate.now().minusDays(5));
        
        return UserPreference.builder()
                .username(username)
                .favoriteComics(Arrays.asList(123, 456))
                .lastReadDates(lastReadDates)
                .displaySettings(settings)
                .build();
    }

    // Simple adapter for LocalDate serialization
    static class LocalDateAdapter implements com.google.gson.JsonSerializer<LocalDate>, com.google.gson.JsonDeserializer<LocalDate> {
        @Override
        public com.google.gson.JsonElement serialize(LocalDate src, java.lang.reflect.Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(src.toString());
        }

        @Override
        public LocalDate deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT, com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
            return LocalDate.parse(json.getAsString());
        }
    }
}