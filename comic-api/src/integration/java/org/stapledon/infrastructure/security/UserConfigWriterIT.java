package org.stapledon.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.stapledon.AbstractIntegrationTest;
import org.stapledon.api.dto.user.User;
import org.stapledon.api.dto.user.UserRegistrationDto;
import org.stapledon.infrastructure.config.UserConfigWriter;

/**
 * Integration test to verify UserConfigWriter in integration tests
 */
class UserConfigWriterIT extends AbstractIntegrationTest {

    @Autowired
    private UserConfigWriter userConfigWriter;

    @Test
    @DisplayName("Should be able to register and retrieve users")
    void registerAndRetrieveUserTest() {
        // Create a unique username for testing
        String username = "test_user_" + System.currentTimeMillis();

        // Register new user
        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username(username)
                .password("test_password")
                .email(username + "@example.com")
                .displayName("Test User")
                .build();

        // Register user and get result
        User newUser = userConfigWriter.registerUser(registrationDto)
                .orElse(null);

        // Verify user was created
        assertThat(newUser).isNotNull();
        assertThat(newUser.getUsername()).isEqualTo(username);

        // Retrieve user by username
        User retrievedUser = userConfigWriter.getUser(username)
                .orElse(null);

        // Verify user can be retrieved
        assertThat(retrievedUser).isNotNull();
        assertThat(retrievedUser.getUsername()).isEqualTo(username);

        // Verify user properties
        assertThat(retrievedUser.getEmail()).isEqualTo(username + "@example.com");
        assertThat(retrievedUser.getDisplayName()).isEqualTo("Test User");
        assertThat(retrievedUser.getRoles()).containsExactly("USER");
    }
}