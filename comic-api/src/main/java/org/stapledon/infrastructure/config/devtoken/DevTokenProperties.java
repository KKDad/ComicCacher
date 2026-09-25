package org.stapledon.infrastructure.config.devtoken;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the dev-only {@code devToken} mutation. Off unless {@code enabled} is set,
 * which only the dev instance does.
 *
 * @param defaultUsername user to issue tokens for when the mutation names none
 * @param enabled         whether the mutation and its schema are loaded at all
 * @param secret          shared secret callers must pass; at least {@value DevTokenConfiguration#MIN_SECRET_LENGTH} characters
 */
@ConfigurationProperties(prefix = "comics.dev-token")
public record DevTokenProperties(String defaultUsername, boolean enabled, String secret) {
}
