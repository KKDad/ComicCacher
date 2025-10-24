package org.stapledon.infrastructure.repository;

import org.springframework.stereotype.Repository;
import org.stapledon.api.dto.user.User;
import org.stapledon.api.dto.user.UserConfig;
import org.stapledon.api.dto.user.UserRegistrationDto;
import org.stapledon.infrastructure.config.ApplicationConfigurationFacade;
import org.stapledon.infrastructure.config.UserConfigWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON file-based implementation of UserRepository.
 * Delegates to ApplicationConfigurationFacade and UserConfigWriter for file I/O and authentication operations.
 */
@Slf4j
@ToString
@Repository
@RequiredArgsConstructor
public class JsonUserRepository implements UserRepository {

    private final ApplicationConfigurationFacade configurationFacade;
    private final UserConfigWriter userConfigWriter;

    @Override
    public UserConfig loadUserConfig() {
        return configurationFacade.loadUserConfig();
    }

    @Override
    public void saveUserConfig(UserConfig config) {
        boolean success = configurationFacade.saveUserConfig(config);
        if (!success) {
            log.error("Failed to save user configuration");
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userConfigWriter.getUser(username);
    }

    @Override
    public List<User> findAllUsers() {
        UserConfig config = loadUserConfig();
        if (config.getUsers() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(config.getUsers().values());
    }

    @Override
    public void saveUser(User user) {
        boolean success = userConfigWriter.saveUser(user);
        if (!success) {
            log.error("Failed to save user: {}", user.getUsername());
        }
    }

    @Override
    public void deleteUser(String username) {
        UserConfig config = loadUserConfig();
        if (config.getUsers() != null) {
            config.getUsers().remove(username);
            saveUserConfig(config);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return userConfigWriter.existsByUsername(username);
    }

    @Override
    public Optional<User> authenticateUser(String username, String password) {
        return userConfigWriter.authenticateUser(username, password);
    }

    @Override
    public Optional<User> registerUser(String username, String password, String email, String displayName) {
        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username(username)
                .password(password)
                .email(email)
                .displayName(displayName)
                .build();

        return userConfigWriter.registerUser(registrationDto);
    }
}
