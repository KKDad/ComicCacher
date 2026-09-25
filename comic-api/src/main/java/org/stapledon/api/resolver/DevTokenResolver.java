package org.stapledon.api.resolver;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.api.dto.auth.AuthResponse;
import org.stapledon.api.dto.user.User;
import org.stapledon.core.auth.model.AuthenticationException;
import org.stapledon.core.user.service.UserService;
import org.stapledon.infrastructure.config.devtoken.DevTokenProperties;
import org.stapledon.infrastructure.security.JwtTokenUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dev-only resolver that issues tokens without a password, so tools and agents can test
 * against the dev instance. Exists only when {@code comics.dev-token.enabled=true}.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "comics.dev-token", name = "enabled", havingValue = "true")
public class DevTokenResolver {

    private final DevTokenProperties properties;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;

    /**
     * Issue an access and refresh token for a user.
     *
     * @param secret   must match {@code comics.dev-token.secret}
     * @param username user to issue for; defaults to {@code comics.dev-token.default-username}
     * @return Authentication payload with JWT tokens
     */
    @MutationMapping
    public AuthResponse devToken(@Argument String secret, @Argument String username) {
        if (!secretMatches(secret)) {
            log.warn("Dev token request rejected: wrong secret");
            throw new AuthenticationException("Invalid dev token secret");
        }

        String target = username != null && !username.isBlank() ? username : properties.defaultUsername();
        if (target == null || target.isBlank()) {
            throw new AuthenticationException("No username given and comics.dev-token.default-username is not set");
        }

        User user = userService.getUser(target).orElseThrow(() -> new AuthenticationException("Unknown user: " + target));
        log.info("AUDIT dev token issued for user {} with roles {}", user.getUsername(), user.getRoles());

        return new AuthResponse(jwtTokenUtil.generateToken(user), jwtTokenUtil.generateRefreshToken(user), user.getUsername(), user.getDisplayName());
    }

    private boolean secretMatches(String secret) {
        return secret != null && MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8), properties.secret().getBytes(StandardCharsets.UTF_8));
    }
}
