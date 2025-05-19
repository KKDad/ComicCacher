package org.stapledon.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.stapledon.api.controller.AuthController;
import org.stapledon.api.dto.auth.AuthRequest;
import org.stapledon.api.dto.auth.AuthResponse;
import org.stapledon.api.dto.user.UserRegistrationDto;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.core.auth.model.AuthenticationException;
import org.stapledon.core.auth.service.AuthService;

import java.util.Optional;

/**
 * Basic unit test for AuthController without using Spring context
 */
class AuthControllerBasicTest {

    @Test
    void registerShouldReturnCreatedStatusWhenSuccessful() {
        // Given
        AuthService authService = Mockito.mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        
        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username("testuser")
                .password("password123")
                .email("test@example.com")
                .displayName("Test User")
                .build();
        
        AuthResponse authResponse = AuthResponse.builder()
                .token("jwtToken")
                .refreshToken("refreshToken")
                .username("testuser")
                .displayName("Test User")
                .build();
        
        when(authService.register(any(UserRegistrationDto.class)))
                .thenReturn(Optional.of(authResponse));

        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = controller.register(registrationDto);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals("testuser", response.getBody().getData().getUsername());
        assertEquals("Test User", response.getBody().getData().getDisplayName());
    }

    @Test
    void registerShouldThrowExceptionWhenRegistrationFails() {
        // Given
        AuthService authService = Mockito.mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        
        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username("existinguser")
                .password("password123")
                .email("existing@example.com")
                .displayName("Existing User")
                .build();
        
        when(authService.register(any(UserRegistrationDto.class)))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(AuthenticationException.class, () -> controller.register(registrationDto));
    }

    @Test
    void loginShouldReturnOkStatusWhenSuccessful() {
        // Given
        AuthService authService = Mockito.mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        
        AuthRequest authRequest = AuthRequest.builder()
                .username("testuser")
                .password("password123")
                .build();
        
        AuthResponse authResponse = AuthResponse.builder()
                .token("jwtToken")
                .refreshToken("refreshToken")
                .username("testuser")
                .displayName("Test User")
                .build();
        
        when(authService.login(any(AuthRequest.class)))
                .thenReturn(Optional.of(authResponse));

        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = controller.login(authRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
        assertEquals("testuser", response.getBody().getData().getUsername());
        assertEquals("jwtToken", response.getBody().getData().getToken());
    }

    @Test
    void loginShouldThrowExceptionWhenLoginFails() {
        // Given
        AuthService authService = Mockito.mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        
        AuthRequest authRequest = AuthRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();
        
        when(authService.login(any(AuthRequest.class)))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(AuthenticationException.class, () -> controller.login(authRequest));
    }
}