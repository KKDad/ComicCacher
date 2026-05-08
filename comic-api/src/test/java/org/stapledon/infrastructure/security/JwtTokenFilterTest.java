package org.stapledon.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.stapledon.api.dto.user.User;
import org.stapledon.core.user.service.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtTokenFilterTest {

    @Mock private JwtTokenUtil jwtTokenUtil;

    @Mock private JwtUserDetailsService userDetailsService;

    @Mock private UserService userService;

    @Mock private HttpServletRequest request;

    @Mock private HttpServletResponse response;

    @Mock private FilterChain filterChain;

    @Mock private UserDetails userDetails;

    private JwtTokenFilter jwtTokenFilter;

    @BeforeEach
    void setUp() {
        jwtTokenFilter = new JwtTokenFilter(jwtTokenUtil, userDetailsService, userService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternalShouldSetAuthenticationForValidToken() throws Exception {
        String token = "valid_token";
        String username = "testuser";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtil.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(userService.getUser(username)).thenReturn(Optional.of(User.builder().username(username).build()));
        when(jwtTokenUtil.isTokenInvalidatedByLogout(anyString(), any())).thenReturn(false);
        when(jwtTokenUtil.validateToken(token, userDetails)).thenReturn(true);
        when(jwtTokenUtil.extractRoles(token)).thenReturn(Collections.singletonList("USER"));
        when(userDetails.getUsername()).thenReturn(username);

        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        verify(jwtTokenUtil).validateToken(token, userDetails);
        verify(jwtTokenUtil).extractRoles(token);
        verify(filterChain).doFilter(request, response);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(username);
    }

    @Test
    void doFilterInternalShouldNotSetAuthenticationForInvalidToken() throws Exception {
        String token = "invalid_token";
        String username = "testuser";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtil.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(userService.getUser(username)).thenReturn(Optional.of(User.builder().username(username).build()));
        when(jwtTokenUtil.isTokenInvalidatedByLogout(anyString(), any())).thenReturn(false);
        when(jwtTokenUtil.validateToken(token, userDetails)).thenReturn(false);

        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        verify(jwtTokenUtil).validateToken(token, userDetails);
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternalShouldRejectTokenInvalidatedByLogout() throws Exception {
        String token = "stale_token";
        String username = "testuser";
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtil.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(userService.getUser(username))
                .thenReturn(Optional.of(User.builder().username(username).tokensInvalidatedBefore(cutoff).build()));
        when(jwtTokenUtil.isTokenInvalidatedByLogout(token, cutoff)).thenReturn(true);

        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        verify(jwtTokenUtil, never()).validateToken(anyString(), any(UserDetails.class));
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternalShouldNotSetAuthenticationWhenNoToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenUtil, userDetailsService, userService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternalShouldHandleExceptions() throws Exception {
        String token = "error_token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtil.extractUsername(token)).thenThrow(new RuntimeException("Test exception"));

        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        verify(jwtTokenUtil).extractUsername(token);
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @ParameterizedTest
    @MethodSource("invalidAuthorizationHeaders")
    void doFilterInternalShouldSkipAuthForInvalidHeaders(String headerValue) throws Exception {
        when(request.getHeader("Authorization")).thenReturn(headerValue);

        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenUtil, userDetailsService, userService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    static Stream<String> invalidAuthorizationHeaders() {
        return Stream.of(
                "",
                "Basic dXNlcjpwYXNz",
                "Bearertoken"
        );
    }
}
