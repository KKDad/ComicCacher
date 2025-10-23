package org.stapledon.infrastructure.config;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.stapledon.api.dto.user.User;
import org.stapledon.api.dto.user.UserConfig;
import org.stapledon.api.dto.user.UserRegistrationDto;
import org.stapledon.common.config.CacheProperties;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration writer for user-related data.
 * This implementation now delegates to ApplicationConfigurationFacade for most operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserConfigWriter {
    @Qualifier("gsonWithLocalDate")
    private final Gson gson;
    private final CacheProperties cacheProperties;
    private final ApplicationConfigurationFacade configurationFacade;

    private UserConfig userConfig;

    /**
     * Save a user to the users.json file
     *
     * @param user The user to save
     * @return true if successful, false otherwise
     */
    public boolean saveUser(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isEmpty()) {
            log.error("Cannot save user: User or username is null/empty");
            return false;
        }

        try {
            // Ensure users are loaded before saving
            loadUsers();

            // Add user to map
            userConfig.getUsers().put(user.getUsername(), user);
            log.info("Saving user: {}", user.getUsername());

            // Save to file using the configuration facade
            return configurationFacade.saveUserConfig(userConfig);
        } catch (Exception e) {
            log.error("Failed to save user: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Register a new user
     *
     * @param registrationDto User registration data
     * @return The created user if successful, empty otherwise
     */
    public Optional<User> registerUser(UserRegistrationDto registrationDto) {
        // Validate input
        if (registrationDto == null ||
            registrationDto.getUsername() == null || registrationDto.getUsername().isEmpty() ||
            registrationDto.getPassword() == null || registrationDto.getPassword().isEmpty()) {
            log.error("Cannot register user: Missing required fields (username or password)");
            return Optional.empty();
        }

        try {
            loadUsers();

            // Check if username already exists
            if (userConfig.getUsers().containsKey(registrationDto.getUsername())) {
                log.warn("Username already exists: {}", registrationDto.getUsername());
                return Optional.empty();
            }

            // Create new user with hashed password
            String hashedPassword = BCrypt.hashpw(registrationDto.getPassword(), BCrypt.gensalt());
            User newUser = User.builder()
                    .username(registrationDto.getUsername())
                    .passwordHash(hashedPassword)
                    .email(registrationDto.getEmail())
                    .displayName(registrationDto.getDisplayName())
                    .created(LocalDateTime.now())
                    .roles(new ArrayList<>(List.of("USER")))
                    .build();

            // Save user by reusing saveUser method
            boolean saved = saveUser(newUser);
            if (!saved) {
                log.error("Failed to save new user during registration");
                return Optional.empty();
            }

            return Optional.of(newUser);
        } catch (Exception e) {
            log.error("Failed to register user: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Authenticate a user
     *
     * @param username Username
     * @param password Raw password
     * @return The authenticated user if successful, empty otherwise
     */
    public Optional<User> authenticateUser(String username, String password) {
        // Validate input
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            log.error("Cannot authenticate: Username or password is null/empty");
            return Optional.empty();
        }

        try {
            loadUsers();

            // Check if user exists
            if (!userConfig.getUsers().containsKey(username)) {
                log.debug("Authentication failed: User not found: {}", username);
                return Optional.empty();
            }

            User user = userConfig.getUsers().get(username);

            // Check for null password hash (shouldn't happen, but just in case)
            if (user.getPasswordHash() == null) {
                log.warn("User {} has null password hash", username);
                return Optional.empty();
            }

            // Verify password
            if (BCrypt.checkpw(password, user.getPasswordHash())) {
                // Update last login time
                user.setLastLogin(LocalDateTime.now());
                boolean saved = saveUser(user);
                if (!saved) {
                    log.warn("Failed to update last login time for user: {}", username);
                    // Continue anyway, the authentication is still successful
                }
                return Optional.of(user);
            }

            // Password incorrect
            log.debug("Authentication failed: Invalid password for user: {}", username);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to authenticate user: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Get a user by username
     *
     * @param username Username
     * @return The user if found, empty otherwise
     */
    public Optional<User> getUser(String username) {
        // Validate input
        if (username == null || username.isEmpty()) {
            log.error("Cannot get user: Username is null/empty");
            return Optional.empty();
        }

        try {
            loadUsers();

            if (userConfig.getUsers().containsKey(username)) {
                return Optional.of(userConfig.getUsers().get(username));
            }

            log.debug("User not found: {}", username);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get user: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Update a user's profile
     *
     * @param user Updated user data
     * @return The updated user if successful, empty otherwise
     */
    public Optional<User> updateUser(User user) {
        // Validate input
        if (user == null || user.getUsername() == null || user.getUsername().isEmpty()) {
            log.error("Cannot update user: User or username is null/empty");
            return Optional.empty();
        }

        try {
            loadUsers();

            // Check if user exists
            if (!userConfig.getUsers().containsKey(user.getUsername())) {
                log.warn("User not found for update: {}", user.getUsername());
                return Optional.empty();
            }

            // Update user while preserving sensitive fields
            User existingUser = userConfig.getUsers().get(user.getUsername());

            // Only update non-null fields
            if (user.getDisplayName() != null) {
                existingUser.setDisplayName(user.getDisplayName());
            }

            if (user.getEmail() != null) {
                existingUser.setEmail(user.getEmail());
            }

            // If roles are provided and not empty, update them
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                existingUser.setRoles(user.getRoles());
            }

            // Save the updated user
            boolean saved = saveUser(existingUser);
            if (!saved) {
                log.error("Failed to save updated user: {}", user.getUsername());
                return Optional.empty();
            }

            return Optional.of(existingUser);
        } catch (Exception e) {
            log.error("Failed to update user: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Update a user's password
     *
     * @param username    Username
     * @param newPassword New password
     * @return The updated user if successful, empty otherwise
     */
    public Optional<User> updatePassword(String username, String newPassword) {
        // Validate input
        if (username == null || username.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            log.error("Cannot update password: Username or new password is null/empty");
            return Optional.empty();
        }

        try {
            loadUsers();

            // Check if user exists
            if (!userConfig.getUsers().containsKey(username)) {
                log.warn("User not found for password update: {}", username);
                return Optional.empty();
            }

            // Update password
            User user = userConfig.getUsers().get(username);
            user.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));

            // Save the updated user
            boolean saved = saveUser(user);
            if (!saved) {
                log.error("Failed to save user after password update: {}", username);
                return Optional.empty();
            }

            return Optional.of(user);
        } catch (Exception e) {
            log.error("Failed to update password: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Load users from the users.json file
     *
     * @return UserConfig containing users
     * @throws JsonParseException if the file is malformed JSON (for testing purposes)
     */
    public UserConfig loadUsers() throws JsonParseException {
        // Return cached config if already loaded and valid
        if (userConfig != null && userConfig.getUsers() != null) {
            return userConfig;
        }

        try {
            userConfig = configurationFacade.loadUserConfig();
            return userConfig;
        } catch (JsonParseException e) {
            // For integration testing, propagate the original exception
            throw e;
        } catch (Exception e) {
            log.error("Error reading user configuration: {}", e.getMessage(), e);
            userConfig = new UserConfig();
            return userConfig;
        }
    }

    public boolean existsByUsername(String username) {
        loadUsers();
        return userConfig.getUsers().containsKey(username);
    }
}