package org.stapledon.infrastructure.repository;

import org.stapledon.api.dto.user.User;
import org.stapledon.api.dto.user.UserConfig;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User persistence operations.
 * Abstracts the underlying storage mechanism (JSON files, database, etc.)
 * from the business logic layer.
 */
public interface UserRepository {

    /**
     * Loads the complete user configuration containing all users.
     */
    UserConfig loadUserConfig();

    /**
     * Saves the complete user configuration.
     */
    void saveUserConfig(UserConfig config);

    /**
     * Finds a user by their username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Retrieves all users in the repository.
     */
    List<User> findAllUsers();

    /**
     * Saves or updates a single user.
     * If the user already exists (by username), it will be updated.
     */
    void saveUser(User user);

    /**
     * Deletes a user by their username.
     */
    void deleteUser(String username);

    /**
     * Checks if a user with the given username exists.
     */
    boolean existsByUsername(String username);

    /**
     * Authenticates a user with the provided username and password.
     * Returns the user if authentication is successful, empty otherwise.
     */
    Optional<User> authenticateUser(String username, String password);

    /**
     * Registers a new user with the provided details.
     * Returns the created user if registration is successful, empty if username already exists.
     */
    Optional<User> registerUser(String username, String password, String email, String displayName);
}
