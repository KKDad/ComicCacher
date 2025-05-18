package org.stapledon.api.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.stapledon.api.controller.PreferenceController;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.dto.preference.UserPreference;
import org.stapledon.core.auth.model.AuthenticationException;
import org.stapledon.core.preference.service.PreferenceService;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Basic unit test for PreferenceController without using Spring context
 */
class PreferenceControllerBasicTest {

    @Test
    void getPreferencesShouldReturnUserPreferences() {
        // Given
        PreferenceService preferenceService = Mockito.mock(PreferenceService.class);
        PreferenceController controller = new PreferenceController(preferenceService);
        
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        
        UserPreference preference = UserPreference.builder()
                .username("testuser")
                .favoriteComics(Arrays.asList(123, 456))
                .lastReadDates(new HashMap<>())
                .displaySettings(new HashMap<>())
                .build();
        
        when(userDetails.getUsername()).thenReturn("testuser");
        when(preferenceService.getPreference("testuser")).thenReturn(Optional.of(preference));

        // When
        ResponseEntity<ApiResponse<UserPreference>> response = controller.getPreferences(userDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals("testuser", response.getBody().getData().getUsername());
        assertEquals(2, response.getBody().getData().getFavoriteComics().size());
        assertTrue(response.getBody().getData().getFavoriteComics().contains(123));
        assertTrue(response.getBody().getData().getFavoriteComics().contains(456));
    }

    @Test
    void getPreferencesShouldThrowExceptionWhenNotFound() {
        // Given
        PreferenceService preferenceService = Mockito.mock(PreferenceService.class);
        PreferenceController controller = new PreferenceController(preferenceService);
        
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        
        when(userDetails.getUsername()).thenReturn("testuser");
        when(preferenceService.getPreference("testuser")).thenReturn(Optional.empty());

        // When/Then
        assertThrows(AuthenticationException.class, () -> controller.getPreferences(userDetails));
    }

    @Test
    void addFavoriteShouldAddComicToFavorites() {
        // Given
        PreferenceService preferenceService = Mockito.mock(PreferenceService.class);
        PreferenceController controller = new PreferenceController(preferenceService);
        
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        int comicId = 789;
        
        UserPreference preference = UserPreference.builder()
                .username("testuser")
                .favoriteComics(new ArrayList<>(Arrays.asList(123, 456, 789)))
                .lastReadDates(new HashMap<>())
                .displaySettings(new HashMap<>())
                .build();
        
        when(userDetails.getUsername()).thenReturn("testuser");
        when(preferenceService.addFavorite("testuser", comicId)).thenReturn(Optional.of(preference));

        // When
        ResponseEntity<ApiResponse<UserPreference>> response = controller.addFavorite(userDetails, comicId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals("testuser", response.getBody().getData().getUsername());
        assertEquals(3, response.getBody().getData().getFavoriteComics().size());
        assertTrue(response.getBody().getData().getFavoriteComics().contains(comicId));
    }

    @Test
    void removeFavoriteShouldRemoveComicFromFavorites() {
        // Given
        PreferenceService preferenceService = Mockito.mock(PreferenceService.class);
        PreferenceController controller = new PreferenceController(preferenceService);
        
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        int comicId = 123;
        
        UserPreference preference = UserPreference.builder()
                .username("testuser")
                .favoriteComics(new ArrayList<>(Arrays.asList(456)))
                .lastReadDates(new HashMap<>())
                .displaySettings(new HashMap<>())
                .build();
        
        when(userDetails.getUsername()).thenReturn("testuser");
        when(preferenceService.removeFavorite("testuser", comicId)).thenReturn(Optional.of(preference));

        // When
        ResponseEntity<ApiResponse<UserPreference>> response = controller.removeFavorite(userDetails, comicId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals("testuser", response.getBody().getData().getUsername());
        assertEquals(1, response.getBody().getData().getFavoriteComics().size());
        assertFalse(response.getBody().getData().getFavoriteComics().contains(comicId));
    }

    @Test
    void updateLastReadShouldUpdateComicReadDate() {
        // Given
        PreferenceService preferenceService = Mockito.mock(PreferenceService.class);
        PreferenceController controller = new PreferenceController(preferenceService);
        
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        int comicId = 123;
        LocalDate date = LocalDate.now();
        Map<String, String> dateData = new HashMap<>();
        dateData.put("date", date.toString());
        
        Map<Integer, LocalDate> lastReadDates = new HashMap<>();
        lastReadDates.put(comicId, date);
        
        UserPreference preference = UserPreference.builder()
                .username("testuser")
                .favoriteComics(new ArrayList<>())
                .lastReadDates(lastReadDates)
                .displaySettings(new HashMap<>())
                .build();
        
        when(userDetails.getUsername()).thenReturn("testuser");
        when(preferenceService.updateLastRead(eq("testuser"), eq(comicId), any(LocalDate.class)))
                .thenReturn(Optional.of(preference));

        // When
        ResponseEntity<ApiResponse<UserPreference>> response = controller.updateLastRead(userDetails, comicId, dateData);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals("testuser", response.getBody().getData().getUsername());
        assertEquals(1, response.getBody().getData().getLastReadDates().size());
        assertEquals(date, response.getBody().getData().getLastReadDates().get(comicId));
    }

    @Test
    void updateDisplaySettingsShouldUpdateUserSettings() {
        // Given
        PreferenceService preferenceService = Mockito.mock(PreferenceService.class);
        PreferenceController controller = new PreferenceController(preferenceService);
        
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        HashMap<String, Object> settings = new HashMap<>();
        settings.put("darkMode", true);
        settings.put("fontSize", 16);
        
        UserPreference preference = UserPreference.builder()
                .username("testuser")
                .favoriteComics(new ArrayList<>())
                .lastReadDates(new HashMap<>())
                .displaySettings(settings)
                .build();
        
        when(userDetails.getUsername()).thenReturn("testuser");
        when(preferenceService.updateDisplaySettings(eq("testuser"), any(HashMap.class)))
                .thenReturn(Optional.of(preference));

        // When
        ResponseEntity<ApiResponse<UserPreference>> response = controller.updateDisplaySettings(userDetails, settings);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals("testuser", response.getBody().getData().getUsername());
        assertEquals(2, response.getBody().getData().getDisplaySettings().size());
        assertEquals(true, response.getBody().getData().getDisplaySettings().get("darkMode"));
        assertEquals(16, response.getBody().getData().getDisplaySettings().get("fontSize"));
    }
}