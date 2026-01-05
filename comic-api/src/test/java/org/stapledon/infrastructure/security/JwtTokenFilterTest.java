package org.stapledon.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class JwtTokenFilterTest {

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private JwtUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserDetails userDetails;

    private JwtTokenFilter jwtTokenFilter;

    @BeforeEach
    void setUp() {
        jwtTokenFilter = new JwtTokenFilter(jwtTokenUtil, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternalShouldSetAuthenticationForValidToken() throws Exception {
        // Given
        String token = "valid_token";
        String username = "testuser";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtil.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtTokenUtil.validateToken(token, userDetails)).thenReturn(true);
        when(jwtTokenUtil.extractRoles(token)).thenReturn(Collections.singletonList("USER"));
        when(userDetails.getUsername()).thenReturn(username);

        // When
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenUtil).extractUsername(token);
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtTokenUtil).validateToken(token, userDetails);
        verify(jwtTokenUtil).extractRoles(token);
        verify(filterChain).doFilter(request, response);

        // Authentication should be set in security context
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(username);
    }

    @Test
    void doFilterInternalShouldNotSetAuthenticationForInvalidToken() throws Exception {
        // Given
        String token = "invalid_token";
        String username = "testuser";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtil.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtTokenUtil.validateToken(token, userDetails)).thenReturn(false);

        // When
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenUtil).extractUsername(token);
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtTokenUtil).validateToken(token, userDetails);
        verify(filterChain).doFilter(request, response);

        // Authentication should not be set in security context
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternalShouldNotSetAuthenticationWhenNoToken() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenUtil, userDetailsService);

        // Authentication should not be set in security context
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternalShouldHandleExceptions() throws Exception {
        // Given
        String token = "error_token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtil.extractUsername(token)).thenThrow(new RuntimeException("Test exception"));

        // When
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenUtil).extractUsername(token);
        verify(filterChain).doFilter(request, response);

        // Authentication should not be set in security context
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
