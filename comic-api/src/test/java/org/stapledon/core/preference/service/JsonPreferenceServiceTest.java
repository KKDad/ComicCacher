package org.stapledon.core.preference.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.api.dto.preference.UserPreference;
import org.stapledon.infrastructure.config.PreferenceConfigWriter;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class JsonPreferenceServiceTest {

    @Mock
    private PreferenceConfigWriter preferenceConfigWriter;

    private JsonPreferenceService preferenceService;

    @BeforeEach
    void setUp() {
        preferenceService = new JsonPreferenceService(preferenceConfigWriter);
    }

    @Test
    void getPreferenceShouldDelegateToConfigWriter() {
        // Given
        String username = "testuser";
        
        UserPreference expectedPreference = createTestPreference(username);
        when(preferenceConfigWriter.getPreference(username)).thenReturn(Optional.of(expectedPreference));

        // When
        Optional<UserPreference> result = preferenceService.getPreference(username);

        // Then
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        verify(preferenceConfigWriter).getPreference(username);
    }

    @Test
    void addFavoriteShouldDelegateToConfigWriter() {
        // Given
        String username = "testuser";
        int comicId = 123;
        
        UserPreference expectedPreference = createTestPreference(username);
        when(preferenceConfigWriter.addFavorite(username, comicId)).thenReturn(Optional.of(expectedPreference));

        // When
        Optional<UserPreference> result = preferenceService.addFavorite(username, comicId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        verify(preferenceConfigWriter).addFavorite(username, comicId);
    }

    @Test
    void removeFavoriteShouldDelegateToConfigWriter() {
        // Given
        String username = "testuser";
        int comicId = 123;
        
        UserPreference expectedPreference = createTestPreference(username);
        when(preferenceConfigWriter.removeFavorite(username, comicId)).thenReturn(Optional.of(expectedPreference));

        // When
        Optional<UserPreference> result = preferenceService.removeFavorite(username, comicId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        verify(preferenceConfigWriter).removeFavorite(username, comicId);
    }

    @Test
    void updateLastReadShouldDelegateToConfigWriter() {
        // Given
        String username = "testuser";
        int comicId = 123;
        LocalDate date = LocalDate.now();
        
        UserPreference expectedPreference = createTestPreference(username);
        when(preferenceConfigWriter.updateLastRead(username, comicId, date)).thenReturn(Optional.of(expectedPreference));

        // When
        Optional<UserPreference> result = preferenceService.updateLastRead(username, comicId, date);

        // Then
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        verify(preferenceConfigWriter).updateLastRead(username, comicId, date);
    }

    @Test
    void updateDisplaySettingsShouldDelegateToConfigWriter() {
        // Given
        String username = "testuser";
        HashMap<String, Object> settings = new HashMap<>();
        settings.put("darkMode", true);
        
        UserPreference expectedPreference = createTestPreference(username);
        when(preferenceConfigWriter.updateDisplaySettings(username, settings)).thenReturn(Optional.of(expectedPreference));

        // When
        Optional<UserPreference> result = preferenceService.updateDisplaySettings(username, settings);

        // Then
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        verify(preferenceConfigWriter).updateDisplaySettings(username, settings);
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
}