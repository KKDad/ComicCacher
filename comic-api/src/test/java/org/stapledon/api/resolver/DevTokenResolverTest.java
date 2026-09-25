package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.api.dto.auth.AuthResponse;
import org.stapledon.api.dto.user.User;
import org.stapledon.core.auth.model.AuthenticationException;
import org.stapledon.core.user.service.UserService;
import org.stapledon.infrastructure.config.devtoken.DevTokenProperties;
import org.stapledon.infrastructure.security.JwtTokenUtil;

import java.util.List;
import java.util.Optional;

/**
 * Tests for DevTokenResolver.
 */
@ExtendWith(MockitoExtension.class)
class DevTokenResolverTest {

    private static final String SECRET = "a-dev-token-secret-of-at-least-32-chars";

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private UserService userService;

    private DevTokenResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DevTokenResolver(new DevTokenProperties("devuser", true, SECRET), jwtTokenUtil, userService);
    }

    @Test
    void issuesTokensForTheDefaultUser() {
        User user = user("devuser");
        when(userService.getUser("devuser")).thenReturn(Optional.of(user));
        when(jwtTokenUtil.generateToken(user)).thenReturn("access");
        when(jwtTokenUtil.generateRefreshToken(user)).thenReturn("refresh");

        AuthResponse response = resolver.devToken(SECRET, null);

        assertThat(response).isEqualTo(new AuthResponse("access", "refresh", "devuser", "Dev User"));
    }

    @Test
    void issuesTokensForTheNamedUser() {
        User user = user("admin");
        when(userService.getUser("admin")).thenReturn(Optional.of(user));
        when(jwtTokenUtil.generateToken(user)).thenReturn("access");
        when(jwtTokenUtil.generateRefreshToken(user)).thenReturn("refresh");

        assertThat(resolver.devToken(SECRET, "admin").username()).isEqualTo("admin");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"wrong", SECRET + "x"})
    void rejectsAWrongSecret(String secret) {
        assertThatThrownBy(() -> resolver.devToken(secret, "admin")).isInstanceOf(AuthenticationException.class);

        verify(userService, never()).getUser(any());
        verify(jwtTokenUtil, never()).generateToken(any());
    }

    @Test
    void rejectsAnUnknownUser() {
        when(userService.getUser("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.devToken(SECRET, "ghost")).isInstanceOf(AuthenticationException.class);
    }

    @Test
    void rejectsWhenNoUserIsNamedAndNoDefaultIsSet() {
        resolver = new DevTokenResolver(new DevTokenProperties(null, true, SECRET), jwtTokenUtil, userService);

        assertThatThrownBy(() -> resolver.devToken(SECRET, " ")).isInstanceOf(AuthenticationException.class);
    }

    private static User user(String username) {
        return User.builder().username(username).displayName("Dev User").roles(List.of("USER")).build();
    }
}
