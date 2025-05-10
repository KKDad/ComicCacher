package org.stapledon.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stapledon.config.UserConfigWriter;
import org.stapledon.dto.User;
import org.stapledon.dto.UserRegistrationDto;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserConfigWriter userConfigWriter;

    @Override
    public Optional<User> registerUser(UserRegistrationDto registrationDto) {
        log.info("Registering new user: {}", registrationDto.getUsername());
        return userConfigWriter.registerUser(registrationDto);
    }

    @Override
    public Optional<User> authenticateUser(String username, String password) {
        log.info("Authenticating user: {}", username);
        return userConfigWriter.authenticateUser(username, password);
    }

    @Override
    public Optional<User> getUser(String username) {
        log.info("Getting user: {}", username);
        return userConfigWriter.getUser(username);
    }

    @Override
    public Optional<User> updateUser(User user) {
        log.info("Updating user: {}", user.getUsername());
        return userConfigWriter.updateUser(user);
    }

    @Override
    public Optional<User> updatePassword(String username, String newPassword) {
        log.info("Updating password for user: {}", username);
        return userConfigWriter.updatePassword(username, newPassword);
    }
}