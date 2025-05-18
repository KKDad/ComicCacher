package org.stapledon.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.stapledon.api.exception.GlobalExceptionHandler;
import org.stapledon.core.auth.service.AuthService;
import org.stapledon.api.dto.auth.AuthRequest;
import org.stapledon.api.dto.auth.AuthResponse;
import org.stapledon.api.dto.user.UserRegistrationDto;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone tests for AuthController that don't rely on Spring context
 */
class AuthControllerTest {

    private MockMvc mockMvc;
    
    @Mock
    private AuthService authService;
    
    private AuthController authController;
    
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();
    
    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        authController = new AuthController(authService);
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
}