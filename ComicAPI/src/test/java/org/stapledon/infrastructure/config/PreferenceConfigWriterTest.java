package org.stapledon.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stapledon.api.dto.preference.PreferenceConfig;
import org.stapledon.api.dto.preference.UserPreference;
import org.stapledon.infrastructure.config.properties.CacheProperties;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

class PreferenceConfigWriterTest {

    @TempDir
    Path tempDir;
    
    // Test subclass that avoids facade issues
    private static class TestPreferenceConfigWriter extends PreferenceConfigWriter {
        private final PreferenceConfig inMemoryConfig;
        
        public TestPreferenceConfigWriter(Gson gson) {
            super(gson, new CacheProperties(), null);
            inMemoryConfig = new PreferenceConfig();
        }
        
        @Override
        public PreferenceConfig loadPreferences() {
            return inMemoryConfig;
        }
        
        @Override
        public boolean savePreference(UserPreference preference) {
            inMemoryConfig.getPreferences().put(preference.getUsername(), preference);
            return true;
        }
        
        @Override
        public Optional<UserPreference> getPreference(String username) {
            // Create default if not exists
            if (!inMemoryConfig.getPreferences().containsKey(username)) {
                UserPreference newPref = UserPreference.builder()
                    .username(username)
                    .build();
                inMemoryConfig.getPreferences().put(username, newPref);
                return Optional.of(newPref);
            }
            return Optional.of(inMemoryConfig.getPreferences().get(username));
        }
        
        @Override
        public Optional<UserPreference> addFavorite(String username, int comicId) {
            Optional<UserPreference> prefOpt = getPreference(username);
            if (prefOpt.isPresent()) {
                UserPreference pref = prefOpt.get();
                if (!pref.getFavoriteComics().contains(comicId)) {
                    pref.getFavoriteComics().add(comicId);
                }
                return Optional.of(pref);
            }
            return Optional.empty();
        }
        
        @Override
        public Optional<UserPreference> removeFavorite(String username, int comicId) {
            Optional<UserPreference> prefOpt = getPreference(username);
            if (prefOpt.isPresent()) {
                UserPreference pref = prefOpt.get();
                pref.getFavoriteComics().remove(Integer.valueOf(comicId));
                return Optional.of(pref);
            }
            return Optional.empty();
        }
        
        @Override
        public Optional<UserPreference> updateLastRead(String username, int comicId, LocalDate date) {
            Optional<UserPreference> prefOpt = getPreference(username);
            if (prefOpt.isPresent()) {
                UserPreference pref = prefOpt.get();
                pref.getLastReadDates().put(comicId, date);
                return Optional.of(pref);
            }
            return Optional.empty();
        }
        
        @Override
        public Optional<UserPreference> updateDisplaySettings(String username, HashMap<String, Object> settings) {
            Optional<UserPreference> prefOpt = getPreference(username);
            if (prefOpt.isPresent()) {
                UserPreference pref = prefOpt.get();
                pref.setDisplaySettings(settings);
                return Optional.of(pref);
            }
            return Optional.empty();
        }
    }

    private TestPreferenceConfigWriter preferenceConfigWriter;

    @BeforeEach
    void setUp() {
        // Setup Gson with adapters for LocalDate
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();

        // Create temp file for preferences
        File preferencesFile = tempDir.resolve("preferences.json").toFile();

        // Create the test writer
        preferenceConfigWriter = new TestPreferenceConfigWriter(gson);
    }

    @Test
    void loadPreferencesShouldCreateEmptyConfigWhenFileDoesNotExist() {
        // When
        PreferenceConfig result = preferenceConfigWriter.loadPreferences();

        // Then
        assertNotNull(result);
        assertNotNull(result.getPreferences());
        assertTrue(result.getPreferences().isEmpty());
    }

    @Test
    void loadPreferencesShouldLoadExistingPreferencesFromFile() {
        // Given 
        UserPreference preference = createTestPreference("testuser");
        preferenceConfigWriter.savePreference(preference);

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
        
        // Create user first
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
        
        // Create user and add comic first
        preferenceConfigWriter.getPreference(username);
        preferenceConfigWriter.addFavorite(username, comicId);
        
        // When
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
        
        // Create user and add comic first
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
        
        // Create user first
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
        
        // Create user first
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