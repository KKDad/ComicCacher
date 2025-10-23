package org.stapledon.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.stapledon.api.dto.auth.AuthRequest;
import org.stapledon.api.dto.auth.AuthResponse;
import org.stapledon.api.dto.user.UserRegistrationDto;
import org.stapledon.api.exception.GlobalExceptionHandler;
import org.stapledon.core.auth.service.AuthService;

import java.util.Optional;

/**
 * Standalone tests for AuthController that don't rely on Spring context
 */
class AuthControllerTest {

    private MockMvc mockMvc;
    
    @Mock
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();
    
    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
    
    @Test
    void registerShouldReturnCreatedStatus() throws Exception {
        // Given
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

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated());
        
        verify(authService).register(any(UserRegistrationDto.class));
    }

    @Test
    void loginShouldReturnOkStatus() throws Exception {
        // Given
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

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk());
        
        verify(authService).login(any(AuthRequest.class));
    }
    
    @Test
    void registerShouldAttemptToRegisterWithInvalidData() throws Exception {
        // Given
        UserRegistrationDto invalidDto = UserRegistrationDto.builder()
                .username("") // Invalid: empty username
                .password("password123")
                .email("test@example.com")
                .displayName("Test User")
                .build();

        // Since there's no validation in the controller or DTO, this would be processed
        // by the service which would likely reject it
        when(authService.register(any(UserRegistrationDto.class)))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isUnauthorized()); // 401 from the exception handler
    }
    
    @Test
    void loginShouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        // Given
        AuthRequest authRequest = AuthRequest.builder()
                .username("testuser")
                .password("password123")
                .build();
        
        when(authService.login(any(AuthRequest.class)))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized());
        
        verify(authService).login(any(AuthRequest.class));
    }
    
    @Test
    void refreshTokenShouldHandleQuotedToken() throws Exception {
        // Given - a token with quotes (simulating direct string body)
        String refreshToken = "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciJ9.signature\"";
        String unquotedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciJ9.signature";
        
        AuthResponse authResponse = AuthResponse.builder()
                .token("newJwtToken")
                .refreshToken("newRefreshToken")
                .username("testuser")
                .displayName("Test User")
                .build();
        
        // Service should receive the unquoted token
        when(authService.refreshToken(unquotedToken))
                .thenReturn(Optional.of(authResponse));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshToken))
                .andExpect(status().isOk());
        
        verify(authService).refreshToken(unquotedToken);
    }
    
    @Test
    void validateTokenShouldReturnTrueForValidToken() throws Exception {
        // Given
        String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciJ9.signature";
        
        when(authService.validateToken(validToken))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/validate-token")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        
        verify(authService).validateToken(validToken);
    }
    
    @Test
    void validateTokenShouldReturnFalseForInvalidToken() throws Exception {
        // Given
        String invalidToken = "invalid.token.here";
        
        when(authService.validateToken(invalidToken))
                .thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/validate-token")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
        
        verify(authService).validateToken(invalidToken);
    }
    
    @Test
    void validateTokenShouldReturnFalseWhenNoAuthorizationHeader() throws Exception {
        // Given - no authorization header
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/validate-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
        
        // Verify service is not called when no token provided
        verify(authService, never()).validateToken(any());
    }
    
    @Test
    void validateTokenShouldReturnFalseWhenAuthorizationHeaderIsInvalid() throws Exception {
        // Given
        String invalidAuthHeader = "InvalidBearer token";
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/validate-token")
                .header("Authorization", invalidAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
        
        // Verify service is not called when header format is invalid
        verify(authService, never()).validateToken(any());
    }
}