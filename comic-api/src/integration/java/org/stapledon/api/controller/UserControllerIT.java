package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.stapledon.AbstractIntegrationTest;
import org.stapledon.api.dto.user.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Integration tests for the UserController
 * Tests authentication, profile management, and password operations
 */
class UserControllerIT extends AbstractIntegrationTest {

    private static final String API_BASE_PATH = "/api/v1";
    private static final String USERS_PATH = API_BASE_PATH + "/users";

    @Test
    @DisplayName("Should allow authenticated user to view their profile")
    void shouldAllowAuthenticatedUserToViewProfile() throws Exception {
        // Create a test user and authenticate
        String token = authenticateUser();

        // Verify authentication succeeded
        assertThat(token)
            .as("Authentication should succeed for test user")
            .isNotNull();

        // Get user profile
        MockHttpServletRequestBuilder request = get(USERS_PATH + "/profile")
            .header("Authorization", "Bearer " + token);

        MvcResult profileResult = mockMvc.perform(request)
            .andDo(print())
            .andReturn();

        // Verify response status is 200 OK
        assertThat(profileResult.getResponse().getStatus())
            .as("Expected GET /users/profile to return status 200")
            .isEqualTo(HttpStatus.OK.value());

        // Verify response contains data field
        String responseContent = profileResult.getResponse().getContentAsString();
        assertThat(responseContent)
            .as("Response should contain 'data' field")
            .contains("data");

        // Verify user data can be parsed
        User user = extractFromResponse(responseContent, "data", User.class);
        assertThat(user)
            .as("Response should contain valid user data")
            .isNotNull();

        // Verify username matches test user
        assertThat(user.getUsername())
            .as("User data should contain correct username")
            .isEqualTo(TEST_USER);
    }

    @Test
    @DisplayName("Should reject unauthenticated profile access")
    void shouldRejectUnauthenticatedProfileAccess() throws Exception {
        // Call without authentication
        MvcResult result = mockMvc.perform(get(USERS_PATH + "/profile"))
            .andDo(print())
            .andReturn();

        // Verify response status is 401 Unauthorized
        assertThat(result.getResponse().getStatus())
            .as("Expected GET /users/profile without authentication to return status 401")
            .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Should allow user to update their profile")
    void shouldAllowUserToUpdateProfile() throws Exception {
        // Create a test user and authenticate
        String token = authenticateUser();

        // Verify authentication succeeded
        assertThat(token)
            .as("Authentication should succeed for test user")
            .isNotNull();

        // Create updated user object
        String updatedDisplayName = "Updated Display Name";
        String updatedEmail = "updated_" + TEST_USER + "@example.com";

        User updatedUser = User.builder()
                .username(TEST_USER)
                .email(updatedEmail)
                .displayName(updatedDisplayName)
                .build();

        // Update profile
        MockHttpServletRequestBuilder updateRequest = put(USERS_PATH + "/profile")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updatedUser));

        MvcResult updateResult = mockMvc.perform(updateRequest)
            .andDo(print())
            .andReturn();

        // Verify response status is 200 OK
        assertThat(updateResult.getResponse().getStatus())
            .as("Expected PUT /users/profile to return status 200")
            .isEqualTo(HttpStatus.OK.value());

        // Verify response contains data field
        String responseContent = updateResult.getResponse().getContentAsString();
        assertThat(responseContent)
            .as("Response should contain 'data' field")
            .contains("data");

        // Verify updated user data can be parsed
        User user = extractFromResponse(responseContent, "data", User.class);
        assertThat(user)
            .as("Response should contain valid user data")
            .isNotNull();

        // Verify display name was updated
        assertThat(user.getDisplayName())
            .as("Display name should be updated")
            .isEqualTo(updatedDisplayName);

        // Verify email was updated
        assertThat(user.getEmail())
            .as("Email should be updated")
            .isEqualTo(updatedEmail);
    }

    @Test
    @DisplayName("Should allow user to update password")
    void shouldAllowUserToUpdatePassword() throws Exception {
        // Create a test user and authenticate
        String token = authenticateUser();

        // Verify authentication succeeded
        assertThat(token)
            .as("Authentication should succeed for test user")
            .isNotNull();

        // Update password
        Map<String, String> passwordUpdateRequest = new HashMap<>();
        passwordUpdateRequest.put("newPassword", "new_password123");

        MockHttpServletRequestBuilder updatePasswordRequest = put(USERS_PATH + "/password")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(passwordUpdateRequest));

        MvcResult updateResult = mockMvc.perform(updatePasswordRequest)
            .andDo(print())
            .andReturn();

        // Verify response status is 200 OK
        assertThat(updateResult.getResponse().getStatus())
            .as("Expected PUT /users/password to return status 200")
            .isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Should reject empty password update")
    void shouldRejectEmptyPasswordUpdate() throws Exception {
        // Create a test user and authenticate
        String token = authenticateUser();

        // Verify authentication succeeded
        assertThat(token)
            .as("Authentication should succeed for test user")
            .isNotNull();

        // Try to update with empty password
        Map<String, String> passwordUpdateRequest = new HashMap<>();
        passwordUpdateRequest.put("newPassword", "");

        MockHttpServletRequestBuilder updatePasswordRequest = put(USERS_PATH + "/password")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(passwordUpdateRequest));

        MvcResult updateResult = mockMvc.perform(updatePasswordRequest)
            .andDo(print())
            .andReturn();

        // Verify response status is 401 Unauthorized
        assertThat(updateResult.getResponse().getStatus())
            .as("Expected PUT /users/password with empty password to return status 401")
            .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }
}