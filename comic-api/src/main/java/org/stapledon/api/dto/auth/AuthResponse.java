package org.stapledon.api.dto.auth;

/**
 * Response object for successful authentication containing tokens and user
 * info.
 */
public record AuthResponse(
        String token,
        String refreshToken,
        String username,
        String displayName) {

    @Override
    public String toString() {
        // Exclude tokens from toString for security
        return "AuthResponse[username=" + username + ", displayName=" + displayName + "]";
    }
}