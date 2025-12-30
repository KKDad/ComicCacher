package org.stapledon.core.user.service;

import org.stapledon.api.dto.user.User;
import org.stapledon.api.dto.user.UserRegistrationDto;

import java.util.Optional;

public interface UserService {

    /**
     * Register a new user
     *
     * @param registrationDto User registration data
     * @return The created user if successful, empty otherwise
     */
    Optional<User> registerUser(UserRegistrationDto registrationDto);

    /**
     * Authenticate a user
     *
     * @param username Username
     * @param password Raw password
     * @return The authenticated user if successful, empty otherwise
     */
    Optional<User> authenticateUser(String username, String password);

    /**
     * Get a user by username
     *
     * @param username Username
     * @return The user if found, empty otherwise
     */
    Optional<User> getUser(String username);

    /**
     * Update a user's profile
     *
     * @param user Updated user data
     * @return The updated user if successful, empty otherwise
     */
    Optional<User> updateUser(User user);

    /**
     * Update a user's password
     *
     * @param username Username
     * @param newPassword New password
     * @return The updated user if successful, empty otherwise
     */
    Optional<User> updatePassword(String username, String newPassword);

    boolean existsByUsername(String username);
}