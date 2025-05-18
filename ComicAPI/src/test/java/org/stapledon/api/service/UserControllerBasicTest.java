package org.stapledon.api.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.stapledon.api.controller.UserController;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.dto.user.User;
import org.stapledon.core.auth.model.AuthenticationException;
import org.stapledon.core.user.service.UserService;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Basic unit test for UserController without using Spring context
 */
class UserControllerBasicTest {

    @Test
    void getProfileShouldReturnUserProfile() {
        // Given
        UserService userService = Mockito.mock(UserService.class);
        UserController controller = new UserController(userService);
        
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        Authentication authentication = Mockito.mock(Authentication.class);
        
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .created(LocalDateTime.now())
                .roles(Arrays.asList("USER"))
                .userToken(UUID.randomUUID())
                .build();
        
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userService.getUser("testuser")).thenReturn(Optional.of(user));

        // When
        ResponseEntity<ApiResponse<User>> response = controller.getProfile(userDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals("testuser", response.getBody().getData().getUsername());
        assertEquals("Test User", response.getBody().getData().getDisplayName());
    }

    @Test
    void getProfileShouldThrowExceptionWhenUserNotFound() {
        // Given
        UserService userService = Mockito.mock(UserService.class);
        UserController controller = new UserController(userService);
        
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        
        when(userDetails.getUsername()).thenReturn("nonexistentuser");
        when(userService.getUser("nonexistentuser")).thenReturn(Optional.empty());

        // When/Then
        assertThrows(AuthenticationException.class, () -> controller.getProfile(userDetails));
    }

    @Test
    void updateProfileShouldReturnUpdatedProfile() {
        // Given
        UserService userService = Mockito.mock(UserService.class);
        UserController controller = new UserController(userService);
        
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        
        User user = User.builder()
                .username("testuser")
                .email("updated@example.com")
                .displayName("Updated Name")
                .created(LocalDateTime.now())
                .roles(Arrays.asList("USER"))
                .userToken(UUID.randomUUID())
                .build();
        
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userService.updateUser(any(User.class))).thenReturn(Optional.of(user));

        // When
        ResponseEntity<ApiResponse<User>> response = controller.updateProfile(userDetails, user);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals("testuser", response.getBody().getData().getUsername());
        assertEquals("Updated Name", response.getBody().getData().getDisplayName());
        assertEquals("updated@example.com", response.getBody().getData().getEmail());
    }

    @Test
    void updatePasswordShouldReturnSuccessMessage() {
        // Given
        UserService userService = Mockito.mock(UserService.class);
        UserController controller = new UserController(userService);
        
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("newPassword", "newpassword123");
        
        User updatedUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .created(LocalDateTime.now())
                .roles(Arrays.asList("USER"))
                .userToken(UUID.randomUUID())
                .build();
        
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userService.updatePassword(anyString(), anyString())).thenReturn(Optional.of(updatedUser));

        // When
        ResponseEntity<ApiResponse<Map<String, String>>> response = controller.updatePassword(userDetails, passwordData);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals("Password updated successfully", response.getBody().getData().get("message"));
    }

    @Test
    void updatePasswordShouldThrowExceptionWhenUpdateFails() {
        // Given
        UserService userService = Mockito.mock(UserService.class);
        UserController controller = new UserController(userService);
        
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("newPassword", "newpassword123");
        
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userService.updatePassword(anyString(), anyString())).thenReturn(Optional.empty());

        // When/Then
        assertThrows(AuthenticationException.class, () -> controller.updatePassword(userDetails, passwordData));
    }
}