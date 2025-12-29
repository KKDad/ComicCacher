package org.stapledon.core.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.api.dto.user.User;
import org.stapledon.api.dto.user.UserRegistrationDto;
import org.stapledon.infrastructure.config.UserConfigWriter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonUserServiceTest {

    @Mock
    private UserConfigWriter userConfigWriter;

    private JsonUserService userService;

    @BeforeEach
    void setUp() {
        userService = new JsonUserService(userConfigWriter);
    }

    @Test
    void registerUserShouldDelegateToConfigWriter() {
        // Given
        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username("testuser")
                .password("password123")
                .email("test@example.com")
                .displayName("Test User")
                .build();
        
        User expectedUser = createTestUser("testuser");
        when(userConfigWriter.registerUser(registrationDto)).thenReturn(Optional.of(expectedUser));

        // When
        Optional<User> result = userService.registerUser(registrationDto);

        // Then
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        verify(userConfigWriter).registerUser(registrationDto);
    }

    @Test
    void authenticateUserShouldDelegateToConfigWriter() {
        // Given
        String username = "testuser";
        String password = "password123";
        
        User expectedUser = createTestUser(username);
        when(userConfigWriter.authenticateUser(username, password)).thenReturn(Optional.of(expectedUser));

        // When
        Optional<User> result = userService.authenticateUser(username, password);

        // Then
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getUsername()).isEqualTo(username);
        verify(userConfigWriter).authenticateUser(username, password);
    }

    @Test
    void getUserShouldDelegateToConfigWriter() {
        // Given
        String username = "testuser";
        
        User expectedUser = createTestUser(username);
        when(userConfigWriter.getUser(username)).thenReturn(Optional.of(expectedUser));

        // When
        Optional<User> result = userService.getUser(username);

        // Then
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getUsername()).isEqualTo(username);
        verify(userConfigWriter).getUser(username);
    }

    @Test
    void updateUserShouldDelegateToConfigWriter() {
        // Given
        User user = createTestUser("testuser");
        
        when(userConfigWriter.updateUser(user)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.updateUser(user);

        // Then
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        verify(userConfigWriter).updateUser(user);
    }

    @Test
    void updatePasswordShouldDelegateToConfigWriter() {
        // Given
        String username = "testuser";
        String newPassword = "newpassword";
        
        User expectedUser = createTestUser(username);
        when(userConfigWriter.updatePassword(username, newPassword)).thenReturn(Optional.of(expectedUser));

        // When
        Optional<User> result = userService.updatePassword(username, newPassword);

        // Then
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getUsername()).isEqualTo(username);
        verify(userConfigWriter).updatePassword(username, newPassword);
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