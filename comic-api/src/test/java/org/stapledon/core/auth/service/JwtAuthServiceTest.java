package org.stapledon.core.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.stapledon.api.dto.auth.AuthRequest;
import org.stapledon.api.dto.auth.AuthResponse;
import org.stapledon.api.dto.user.User;
import org.stapledon.api.dto.user.UserRegistrationDto;
import org.stapledon.core.user.service.UserService;
import org.stapledon.infrastructure.security.JwtTokenUtil;
import org.stapledon.infrastructure.security.JwtUserDetailsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class JwtAuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private JwtUserDetailsService userDetailsService;

    @Mock
    private UserDetails userDetails;

    private JwtAuthService authService;

    @BeforeEach
    void setUp() {
        authService = new JwtAuthService(userService, jwtTokenUtil, userDetailsService);
    }

    @Test
    void registerShouldReturnAuthResponseForValidRegistration() {
        // Given
        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username("newuser")
                .password("password123")
                .email("newuser@example.com")
                .displayName("New User")
                .build();
        
        // Create a test user with specific display name that matches expected test value
        User newUser = User.builder()
                .username("newuser")
                .passwordHash("hashedPassword")
                .email("newuser@example.com")
                .displayName("New User")  // Explicit display name that matches test
                .created(LocalDateTime.now())
                .roles(List.of("USER"))
                .userToken(UUID.randomUUID())
                .build();
                
        when(userService.registerUser(registrationDto)).thenReturn(Optional.of(newUser));
        when(jwtTokenUtil.generateToken(newUser)).thenReturn("jwtToken");
        when(jwtTokenUtil.generateRefreshToken(newUser)).thenReturn("refreshToken");

        // When
        Optional<AuthResponse> result = authService.register(registrationDto);

        // Then
        assertTrue(result.isPresent());
        assertEquals("newuser", result.get().getUsername());
        assertEquals("New User", result.get().getDisplayName());
        assertEquals("jwtToken", result.get().getToken());
        assertEquals("refreshToken", result.get().getRefreshToken());
        
        verify(userService).registerUser(registrationDto);
        verify(jwtTokenUtil).generateToken(newUser);
        verify(jwtTokenUtil).generateRefreshToken(newUser);
    }

    @Test
    void registerShouldReturnEmptyForFailedRegistration() {
        // Given
        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username("existinguser")
                .password("password123")
                .email("existing@example.com")
                .displayName("Existing User")
                .build();
        
        when(userService.registerUser(registrationDto)).thenReturn(Optional.empty());

        // When
        Optional<AuthResponse> result = authService.register(registrationDto);

        // Then
        assertTrue(result.isEmpty());
        verify(userService).registerUser(registrationDto);
        verifyNoInteractions(jwtTokenUtil);
    }

    @Test
    void loginShouldReturnAuthResponseForValidCredentials() {
        // Given
        AuthRequest authRequest = AuthRequest.builder()
                .username("user")
                .password("password")
                .build();
        
        User user = createTestUser("user");
        when(userService.authenticateUser(authRequest.getUsername(), authRequest.getPassword())).thenReturn(Optional.of(user));
        when(jwtTokenUtil.generateToken(user)).thenReturn("jwtToken");
        when(jwtTokenUtil.generateRefreshToken(user)).thenReturn("refreshToken");

        // When
        Optional<AuthResponse> result = authService.login(authRequest);

        // Then
        assertTrue(result.isPresent());
        assertEquals("user", result.get().getUsername());
        assertEquals("jwtToken", result.get().getToken());
        assertEquals("refreshToken", result.get().getRefreshToken());
        
        verify(userService).authenticateUser(authRequest.getUsername(), authRequest.getPassword());
        verify(jwtTokenUtil).generateToken(user);
        verify(jwtTokenUtil).generateRefreshToken(user);
    }

    @Test
    void loginShouldThrowExceptionForInvalidCredentials() {
        // Given
        AuthRequest authRequest = AuthRequest.builder()
                .username("user")
                .password("wrongpassword")
                .build();
        
        when(userService.authenticateUser(authRequest.getUsername(), authRequest.getPassword())).thenReturn(Optional.empty());

        // When/Then
        assertThrows(BadCredentialsException.class, () -> authService.login(authRequest));
        
        verify(userService).authenticateUser(authRequest.getUsername(), authRequest.getPassword());
        verifyNoInteractions(jwtTokenUtil);
    }

    @Test
    void refreshTokenShouldReturnNewTokensForValidRefreshToken() {
        // Given
        String refreshToken = "validRefreshToken";
        String username = "user";
        
        when(jwtTokenUtil.extractUsername(refreshToken)).thenReturn(username);
        
        User user = createTestUser(username);
        when(userService.getUser(username)).thenReturn(Optional.of(user));
        
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtTokenUtil.validateToken(refreshToken, userDetails)).thenReturn(true);
        
        when(jwtTokenUtil.generateToken(user)).thenReturn("newJwtToken");
        when(jwtTokenUtil.generateRefreshToken(user)).thenReturn("newRefreshToken");

        // When
        Optional<AuthResponse> result = authService.refreshToken(refreshToken);

        // Then
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        assertEquals("newJwtToken", result.get().getToken());
        assertEquals("newRefreshToken", result.get().getRefreshToken());
        
        verify(jwtTokenUtil).extractUsername(refreshToken);
        verify(userService).getUser(username);
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtTokenUtil).validateToken(refreshToken, userDetails);
        verify(jwtTokenUtil).generateToken(user);
        verify(jwtTokenUtil).generateRefreshToken(user);
    }

    @Test
    void refreshTokenShouldReturnEmptyForInvalidRefreshToken() {
        // Given
        String refreshToken = "invalidRefreshToken";
        
        when(jwtTokenUtil.extractUsername(refreshToken)).thenReturn(null);

        // When
        Optional<AuthResponse> result = authService.refreshToken(refreshToken);

        // Then
        assertTrue(result.isEmpty());
        verify(jwtTokenUtil).extractUsername(refreshToken);
        verifyNoMoreInteractions(userService, userDetailsService);
        verify(jwtTokenUtil, never()).generateToken(any());
        verify(jwtTokenUtil, never()).generateRefreshToken(any());
    }

    @Test
    void validateTokenShouldReturnTrueForValidToken() {
        // Given
        String token = "validToken";
        String username = "user";
        
        when(jwtTokenUtil.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtTokenUtil.validateToken(token, userDetails)).thenReturn(true);

        // When
        boolean result = authService.validateToken(token);

        // Then
        assertTrue(result);
        verify(jwtTokenUtil).extractUsername(token);
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtTokenUtil).validateToken(token, userDetails);
    }

    @Test
    void validateTokenShouldReturnFalseForInvalidToken() {
        // Given
        String token = "invalidToken";
        
        when(jwtTokenUtil.extractUsername(token)).thenReturn(null);

        // When
        boolean result = authService.validateToken(token);

        // Then
        assertFalse(result);
        verify(jwtTokenUtil).extractUsername(token);
        verifyNoInteractions(userDetailsService);
    }

    private User createTestUser(String username) {
        return User.builder()
                .username(username)
                .passwordHash("hashedPassword")
                .email(username + "@example.com")
                .displayName("Test " + username)
                .created(LocalDateTime.now())
                .roles(List.of("USER"))
                .userToken(UUID.randomUUID())
                .build();
    }
}