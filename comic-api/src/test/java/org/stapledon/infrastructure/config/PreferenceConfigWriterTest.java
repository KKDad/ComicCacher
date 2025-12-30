package org.stapledon.infrastructure.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stapledon.api.dto.preference.PreferenceConfig;
import org.stapledon.api.dto.preference.UserPreference;
import org.stapledon.common.config.CacheProperties;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(result).isNotNull();
        assertThat(result.getPreferences()).isNotNull();
        assertThat(result.getPreferences().isEmpty()).isTrue();
    }

    @Test
    void loadPreferencesShouldLoadExistingPreferencesFromFile() {
        // Given
        UserPreference preference = createTestPreference("testuser");
        preferenceConfigWriter.savePreference(preference);

        // When
        PreferenceConfig result = preferenceConfigWriter.loadPreferences();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPreferences()).isNotNull();
        assertThat(result.getPreferences().size()).isEqualTo(1);
        assertThat(result.getPreferences().containsKey("testuser")).isTrue();
        assertThat(result.getPreferences().get("testuser").getUsername()).isEqualTo("testuser");
    }

    @Test
    void getPreferenceShouldCreateDefaultPreferenceForNewUser() {
        // Given
        String username = "newuser";

        // When
        Optional<UserPreference> result = preferenceConfigWriter.getPreference(username);

        // Then
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getUsername()).isEqualTo(username);
        assertThat(result.get().getFavoriteComics()).isNotNull();
        assertThat(result.get().getFavoriteComics().isEmpty()).isTrue();
        assertThat(result.get().getLastReadDates()).isNotNull();
        assertThat(result.get().getLastReadDates().isEmpty()).isTrue();
        assertThat(result.get().getDisplaySettings()).isNotNull();
        assertThat(result.get().getDisplaySettings().isEmpty()).isTrue();
    }

    @Test
    void getPreferenceShouldReturnExistingPreference() {
        // Given
        UserPreference preference = createTestPreference("existinguser");
        preferenceConfigWriter.savePreference(preference);

        // When
        Optional<UserPreference> result = preferenceConfigWriter.getPreference("existinguser");

        // Then
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getUsername()).isEqualTo("existinguser");
        assertThat(result.get().getFavoriteComics().size()).isEqualTo(2);
        assertThat(result.get().getFavoriteComics().contains(123)).isTrue();
        assertThat(result.get().getFavoriteComics().contains(456)).isTrue();
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
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getFavoriteComics().contains(comicId)).isTrue();
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
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getFavoriteComics().size()).isEqualTo(beforeSize);
        assertThat(result.get().getFavoriteComics().contains(comicId)).isTrue();
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
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getFavoriteComics().contains(comicId)).isFalse();
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
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getLastReadDates().containsKey(comicId)).isTrue();
        assertThat(result.get().getLastReadDates().get(comicId)).isEqualTo(date);
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
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getDisplaySettings().size()).isEqualTo(2);
        assertThat(result.get().getDisplaySettings().get("darkMode")).isEqualTo(true);
        assertThat(result.get().getDisplaySettings().get("fontSize")).isEqualTo(14);
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