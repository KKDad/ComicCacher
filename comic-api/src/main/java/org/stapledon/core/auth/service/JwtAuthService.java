package org.stapledon.core.auth.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.stapledon.api.dto.auth.AuthRequest;
import org.stapledon.api.dto.auth.AuthResponse;
import org.stapledon.api.dto.user.User;
import org.stapledon.api.dto.user.UserRegistrationDto;
import org.stapledon.core.auth.model.AuthenticationException;
import org.stapledon.core.mail.service.MailService;
import org.stapledon.core.user.service.UserService;
import org.stapledon.infrastructure.security.JwtTokenUtil;
import org.stapledon.infrastructure.security.JwtUserDetailsService;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@Service
@RequiredArgsConstructor
public class JwtAuthService implements AuthService {

    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtUserDetailsService userDetailsService;
    private final MailService mailService;

    @Override
    public Optional<AuthResponse> register(UserRegistrationDto registrationDto) {
        log.info("Registering new user: {}", registrationDto.getUsername());
        if (userService.existsByUsername(registrationDto.getUsername())) {
            throw new AuthenticationException("Username already exists");
        }

        Optional<User> userOpt = userService.registerUser(registrationDto);

        if (userOpt.isEmpty()) {
            log.warn("User registration failed: {}", registrationDto.getUsername());
            return Optional.empty();
        }

        User user = userOpt.get();
        String token = jwtTokenUtil.generateToken(user);
        String refreshToken = jwtTokenUtil.generateRefreshToken(user);

        return Optional.of(new AuthResponse(
                token,
                refreshToken,
                user.getUsername(),
                user.getDisplayName()));
    }

    @Override
    public Optional<AuthResponse> login(AuthRequest authRequest) {
        log.info("User login: {}", authRequest.username());

        Optional<User> userOpt = userService.authenticateUser(authRequest.username(), authRequest.password());

        if (userOpt.isEmpty()) {
            log.warn("Authentication failed for user: {}", authRequest.username());
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userOpt.get();
        String token = jwtTokenUtil.generateToken(user);
        String refreshToken = jwtTokenUtil.generateRefreshToken(user);

        return Optional.of(new AuthResponse(
                token,
                refreshToken,
                user.getUsername(),
                user.getDisplayName()));
    }

    @Override
    public Optional<AuthResponse> refreshToken(String refreshToken) {
        try {
            log.info("Refreshing token");

            String username = jwtTokenUtil.extractUsername(refreshToken);

            if (username == null) {
                log.warn("Invalid refresh token");
                return Optional.empty();
            }

            Optional<User> userOpt = userService.getUser(username);

            if (userOpt.isEmpty()) {
                log.warn("User not found for refresh token: {}", username);
                return Optional.empty();
            }

            User user = userOpt.get();
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtTokenUtil.validateToken(refreshToken, userDetails)) {
                String newToken = jwtTokenUtil.generateToken(user);
                String newRefreshToken = jwtTokenUtil.generateRefreshToken(user);

                return Optional.of(new AuthResponse(
                        newToken,
                        newRefreshToken,
                        user.getUsername(),
                        user.getDisplayName()));
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage(), e);
            throw new AuthenticationException("Invalid refresh token");
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            String username = jwtTokenUtil.extractUsername(token);

            if (username == null) {
                return false;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            return jwtTokenUtil.validateToken(token, userDetails);
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void forgotPassword(String email) {
        log.info("Password reset requested for email: {}", email);

        // Always return silently to prevent email enumeration
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.debug("No user found for email: {}", email);
            return;
        }

        User user = userOpt.get();
        String resetToken = jwtTokenUtil.generatePasswordResetToken(user.getUsername());
        mailService.sendPasswordResetEmail(email, resetToken);
    }

    @Override
    public Optional<AuthResponse> resetPassword(String token, String newPassword) {
        log.info("Password reset attempt with token");

        Optional<String> usernameOpt = jwtTokenUtil.validatePasswordResetToken(token);
        if (usernameOpt.isEmpty()) {
            throw new AuthenticationException("Invalid or expired password reset token");
        }

        String username = usernameOpt.get();
        Optional<User> userOpt = userService.updatePassword(username, newPassword);
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Failed to reset password");
        }

        User user = userOpt.get();
        String accessToken = jwtTokenUtil.generateToken(user);
        String refreshToken = jwtTokenUtil.generateRefreshToken(user);

        return Optional.of(new AuthResponse(
                accessToken,
                refreshToken,
                user.getUsername(),
                user.getDisplayName()));
    }
}