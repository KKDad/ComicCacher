package org.stapledon.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.model.ResponseBuilder;
import org.stapledon.core.user.service.UserService;
import org.stapledon.api.dto.user.User;
import org.stapledon.core.auth.model.AuthenticationException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    /**
     * Get current user profile
     * 
     * @param userDetails Current authenticated user
     * @return User profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<User>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Getting profile for user: {}", userDetails.getUsername());
        
        return userService.getUser(userDetails.getUsername())
                .map(ResponseBuilder::ok)
                .orElseThrow(() -> new AuthenticationException("User not found"));
    }

    /**
     * Update user profile
     * 
     * @param userDetails Current authenticated user
     * @param user Updated user data
     * @return Updated user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(@AuthenticationPrincipal UserDetails userDetails, @RequestBody User user) {
        log.info("Updating profile for user: {}", userDetails.getUsername());
        
        // Ensure username matches authenticated user
        if (!userDetails.getUsername().equals(user.getUsername())) {
            throw new AuthenticationException("Username mismatch");
        }
        
        return userService.updateUser(user)
                .map(ResponseBuilder::ok)
                .orElseThrow(() -> new AuthenticationException("Failed to update profile"));
    }

    /**
     * Update user password
     * 
     * @param userDetails Current authenticated user
     * @param passwordData Map containing new password
     * @return Success message
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Map<String, String>>> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails, 
            @RequestBody Map<String, String> passwordData) {
        log.info("Updating password for user: {}", userDetails.getUsername());
        
        String newPassword = passwordData.get("newPassword");
        
        if (newPassword == null || newPassword.isEmpty()) {
            throw new AuthenticationException("New password is required");
        }
        
        userService.updatePassword(userDetails.getUsername(), newPassword)
                .orElseThrow(() -> new AuthenticationException("Failed to update password"));
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password updated successfully");
        
        return ResponseBuilder.ok(response);
    }
}