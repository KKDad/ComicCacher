package org.stapledon.api.dto.auth;

/**
 * Request object for authentication containing user credentials.
 */
public record AuthRequest(String username, String password) {

    @Override
    public String toString() {
        // Exclude password from toString for security
        return "AuthRequest[username=" + username + "]";
    }
}