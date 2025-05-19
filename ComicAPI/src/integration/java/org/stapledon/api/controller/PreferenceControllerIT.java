package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.stapledon.AbstractIntegrationTest;
import org.stapledon.api.dto.preference.UserPreference;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

class PreferenceControllerIT extends AbstractIntegrationTest {

    private static final String API_BASE_PATH = "/api/v1";
    private static final String PREFERENCES_PATH = API_BASE_PATH + "/preferences";

    @Test
    @DisplayName("Should retrieve user preferences when authenticated")
    void getPreferencesTest() throws Exception {
        // Create a test user and authenticate
        String token = authenticateUser();
        
        // Verify authentication succeeded
        assertThat(token)
            .as("Authentication should succeed for test user")
            .isNotNull();
        
        // Get user preferences
        MvcResult result = mockMvc.perform(get(PREFERENCES_PATH)
                .header("Authorization", "Bearer " + token))
            .andDo(print())
            .andReturn();
            
        // Verify response status is 200 OK
        assertThat(result.getResponse().getStatus())
            .as("Expected GET /preferences to return status 200")
            .isEqualTo(HttpStatus.OK.value());
        
        // Verify response contains data field
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent)
            .as("Response should contain 'data' field")
            .contains("data");
        
        // Verify preferences can be parsed
        UserPreference preferences = extractFromResponse(responseContent, "data", UserPreference.class);
        assertThat(preferences)
            .as("Response should contain valid preference data")
            .isNotNull();
            
        // Verify username in preferences matches test user
        assertThat(preferences.getUsername())
            .as("Preferences should contain correct username")
            .isEqualTo(TEST_USER);
    }

    @Test
    @DisplayName("Should reject preference request when not authenticated")
    void getPreferencesUnauthenticatedTest() throws Exception {
        // Call without authentication
        MvcResult result = mockMvc.perform(get(PREFERENCES_PATH))
            .andDo(print())
            .andReturn();
            
        // Verify response status is 401 Unauthorized
        assertThat(result.getResponse().getStatus())
            .as("Expected GET /preferences without authentication to return status 401")
            .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Should allow adding and removing comic from favorites")
    void addAndRemoveFavoriteTest() throws Exception {
        // Create a test user and authenticate
        String token = authenticateUser();
        
        // Verify authentication succeeded
        assertThat(token)
            .as("Authentication should succeed for test user")
            .isNotNull();
        
        // Use test comic ID from constants
        int comicId = TEST_COMIC_ID;
        
        // First remove the comic from favorites if it's there
        mockMvc.perform(delete(PREFERENCES_PATH + "/comics/{comicId}/favorite", comicId)
                .header("Authorization", "Bearer " + token))
            .andReturn();
        
        // Add the comic to favorites
        MvcResult addResult = mockMvc.perform(post(PREFERENCES_PATH + "/comics/{comicId}/favorite", comicId)
                .header("Authorization", "Bearer " + token))
            .andDo(print())
            .andReturn();
            
        // Verify response status is 200 OK
        assertThat(addResult.getResponse().getStatus())
            .as("Expected POST /preferences/comics/{comicId}/favorite to return status 200")
            .isEqualTo(HttpStatus.OK.value());
        
        // Verify the comic is now in favorites
        UserPreference prefs = extractFromResponse(addResult.getResponse().getContentAsString(), "data", UserPreference.class);
        assertThat(prefs)
            .as("Response should contain valid preference data")
            .isNotNull();
        assertThat(prefs.getFavoriteComics())
            .as("Favorites should include the added comic ID")
            .contains(comicId);
        
        // Remove the comic from favorites
        MvcResult removeResult = mockMvc.perform(delete(PREFERENCES_PATH + "/comics/{comicId}/favorite", comicId)
                .header("Authorization", "Bearer " + token))
            .andDo(print())
            .andReturn();
            
        // Verify response status is 200 OK
        assertThat(removeResult.getResponse().getStatus())
            .as("Expected DELETE /preferences/comics/{comicId}/favorite to return status 200")
            .isEqualTo(HttpStatus.OK.value());
        
        // Verify the comic is no longer in favorites
        prefs = extractFromResponse(removeResult.getResponse().getContentAsString(), "data", UserPreference.class);
        assertThat(prefs)
            .as("Response should contain valid preference data")
            .isNotNull();
        assertThat(prefs.getFavoriteComics())
            .as("Favorites should not include the removed comic ID")
            .doesNotContain(comicId);
    }

    @Test
    @DisplayName("Should update last read date for a comic")
    void updateLastReadTest() throws Exception {
        // Create a test user and authenticate
        String token = authenticateUser();
        
        // Verify authentication succeeded
        assertThat(token)
            .as("Authentication should succeed for test user")
            .isNotNull();
        
        // Use test comic ID from constants
        int comicId = TEST_COMIC_ID;
        
        // Set a last read date
        LocalDate today = LocalDate.now();
        Map<String, String> dateData = new HashMap<>();
        dateData.put("date", today.format(DateTimeFormatter.ISO_DATE));
        
        // Update last read date
        MvcResult updateResult = mockMvc.perform(post(PREFERENCES_PATH + "/comics/{comicId}/lastread", comicId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dateData)))
            .andDo(print())
            .andReturn();
            
        // Verify response status is 200 OK
        assertThat(updateResult.getResponse().getStatus())
            .as("Expected POST /preferences/comics/{comicId}/lastread to return status 200")
            .isEqualTo(HttpStatus.OK.value());
        
        // Verify the last read date was updated
        UserPreference prefs = extractFromResponse(updateResult.getResponse().getContentAsString(), "data", UserPreference.class);
        assertThat(prefs)
            .as("Response should contain valid preference data")
            .isNotNull();
        assertThat(prefs.getLastReadDates())
            .as("Last read dates should contain updated comic")
            .containsKey(comicId);
        LocalDate storedDate = prefs.getLastReadDates().get(comicId);
        assertThat(storedDate)
            .as("Stored last read date should match the date we set")
            .isEqualTo(today);
    }

    @Test
    @DisplayName("Should update display settings")
    void updateDisplaySettingsTest() throws Exception {
        // Create a test user and authenticate
        String token = authenticateUser();
        
        // Verify authentication succeeded
        assertThat(token)
            .as("Authentication should succeed for test user")
            .isNotNull();
        
        // Create display settings
        HashMap<String, Object> settings = new HashMap<>();
        settings.put("theme", "dark");
        settings.put("fontSize", 16);
        settings.put("showTutorial", false);
        
        // Update display settings
        MvcResult updateResult = mockMvc.perform(post(PREFERENCES_PATH + "/display-settings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(settings)))
            .andDo(print())
            .andReturn();
            
        // Verify response status is 200 OK
        assertThat(updateResult.getResponse().getStatus())
            .as("Expected POST /preferences/display-settings to return status 200")
            .isEqualTo(HttpStatus.OK.value());
        
        // Verify the display settings were updated
        UserPreference prefs = extractFromResponse(updateResult.getResponse().getContentAsString(), "data", UserPreference.class);
        assertThat(prefs)
            .as("Response should contain valid preference data")
            .isNotNull();
        assertThat(prefs.getDisplaySettings())
            .as("Display settings should contain theme")
            .containsKey("theme");
        assertThat(prefs.getDisplaySettings().get("theme"))
            .as("Theme should be dark")
            .isEqualTo("dark");
        assertThat(prefs.getDisplaySettings())
            .as("Display settings should contain fontSize")
            .containsKey("fontSize");
        assertThat(prefs.getDisplaySettings())
            .as("Display settings should contain showTutorial")
            .containsKey("showTutorial");
        assertThat(prefs.getDisplaySettings().get("showTutorial"))
            .as("showTutorial should be false")
            .isEqualTo(false);
    }

    @Test
    @DisplayName("Should reject invalid date format when updating last read date")
    void updateLastReadWithInvalidDateFormatTest() throws Exception {
        // Create a test user and authenticate
        String token = authenticateUser();
        
        // Verify authentication succeeded
        assertThat(token)
            .as("Authentication should succeed for test user")
            .isNotNull();
        
        // Use test comic ID from constants
        int comicId = TEST_COMIC_ID;
        
        // Set an invalid date format
        Map<String, String> dateData = new HashMap<>();
        dateData.put("date", "not-a-date");
        
        // Attempt to update with invalid date
        MvcResult result = mockMvc.perform(post(PREFERENCES_PATH + "/comics/{comicId}/lastread", comicId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dateData)))
            .andDo(print())
            .andReturn();
            
        // Verify response status is 400 Bad Request
        assertThat(result.getResponse().getStatus())
            .as("Expected POST /preferences/comics/{comicId}/lastread with invalid date to return status 400")
            .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }
}