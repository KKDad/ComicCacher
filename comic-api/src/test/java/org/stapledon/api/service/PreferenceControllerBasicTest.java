package org.stapledon.api.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.stapledon.api.controller.PreferenceController;
import org.stapledon.api.dto.preference.UserPreference;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.core.auth.model.AuthenticationException;
import org.stapledon.core.preference.service.PreferenceService;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().getData().getFavoriteComics().size()).isEqualTo(2);
        assertThat(response.getBody().getData().getFavoriteComics().contains(123)).isTrue();
        assertThat(response.getBody().getData().getFavoriteComics().contains(456)).isTrue();
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
         assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> controller.getPreferences(userDetails));
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
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().getData().getFavoriteComics().size()).isEqualTo(3);
        assertThat(response.getBody().getData().getFavoriteComics().contains(comicId)).isTrue();
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
                .favoriteComics(new ArrayList<>(List.of(456)))
                .lastReadDates(new HashMap<>())
                .displaySettings(new HashMap<>())
                .build();

        when(userDetails.getUsername()).thenReturn("testuser");
        when(preferenceService.removeFavorite("testuser", comicId)).thenReturn(Optional.of(preference));

        // When
        ResponseEntity<ApiResponse<UserPreference>> response = controller.removeFavorite(userDetails, comicId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().getData().getFavoriteComics().size()).isEqualTo(1);
        assertThat(response.getBody().getData().getFavoriteComics().contains(comicId)).isFalse();
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
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().getData().getLastReadDates().size()).isEqualTo(1);
        assertThat(response.getBody().getData().getLastReadDates().get(comicId)).isEqualTo(date);
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
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().getData().getDisplaySettings().size()).isEqualTo(2);
        assertThat(response.getBody().getData().getDisplaySettings().get("darkMode")).isEqualTo(true);
        assertThat(response.getBody().getData().getDisplaySettings().get("fontSize")).isEqualTo(16);
    }
}