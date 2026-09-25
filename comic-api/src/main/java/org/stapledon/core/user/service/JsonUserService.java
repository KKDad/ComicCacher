package org.stapledon.core.user.service;

import org.springframework.stereotype.Service;
import org.stapledon.api.dto.user.User;
import org.stapledon.api.dto.user.UserRegistrationDto;
import org.stapledon.common.util.LogContext;
import org.stapledon.infrastructure.config.UserConfigWriter;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@Service
@RequiredArgsConstructor
public class JsonUserService implements UserService {

    private final UserConfigWriter userConfigWriter;

    @Override
    public Optional<User> registerUser(UserRegistrationDto registrationDto) {
        log.debug("Registering new user: {}", registrationDto.getUsername());
        return userConfigWriter.registerUser(registrationDto);
    }

    @Override
    public Optional<User> authenticateUser(String username, String password) {
        log.debug("Authenticating user: {}", username);
        return userConfigWriter.authenticateUser(username, password);
    }

    @Override
    public Optional<User> getUser(String username) {
        log.debug("Getting user: {}", username);
        return userConfigWriter.getUser(username);
    }

    @Override
    public Optional<User> updateUser(User user) {
        log.debug("Updating user: {}", user.getUsername());
        return userConfigWriter.updateUser(user);
    }

    @Override
    public Optional<User> updatePassword(String username, String newPassword) {
        log.debug("Updating password for user: {}", username);
        return userConfigWriter.updatePassword(username, newPassword);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userConfigWriter.existsByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email {}", LogContext.maskEmail(email));
        return userConfigWriter.loadUsers().getUsers().values().stream()
                .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                .findFirst();
    }

    @Override
    public boolean deleteUser(String username) {
        log.debug("Deleting user: {}", username);
        return userConfigWriter.deleteUser(username);
    }
}
