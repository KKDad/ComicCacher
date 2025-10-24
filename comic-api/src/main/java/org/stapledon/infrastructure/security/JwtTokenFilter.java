package org.stapledon.infrastructure.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final JwtUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            
            if (jwt != null) {
                String username = jwtTokenUtil.extractUsername(jwt);
                log.debug("JWT token found for user: {}", username);
                
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    log.debug("Loaded user details for: {}", username);
                    
                    if (jwtTokenUtil.validateToken(jwt, userDetails)) {
                        log.debug("JWT token validated for user: {}", username);
                        
                        // Extract roles from JWT and convert to authorities
                        List<SimpleGrantedAuthority> authorities = jwtTokenUtil.extractRoles(jwt)
                                .stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .toList();
                        log.debug("User roles: {}", authorities);
                        
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, authorities);
                        
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Authentication set in SecurityContext for user: {}", username);
                    } else {
                        log.warn("Invalid JWT token for user: {}", username);
                    }
                } else {
                    if (username == null) {
                        log.warn("No username extracted from JWT token");
                    } else if (SecurityContextHolder.getContext().getAuthentication() != null) {
                        log.debug("SecurityContext already contains authentication: {}", 
                                 SecurityContextHolder.getContext().getAuthentication().getName());
                    }
                }
            } else {
                log.debug("No JWT token found in request");
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage(), e);
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Parse JWT from Authorization header
     *
     * @param request HTTP request
     * @return JWT token or null
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        
        if (headerAuth == null) {
            log.debug("Authorization header is missing in request");
            return null;
        }
        
        if (!StringUtils.hasText(headerAuth)) {
            log.debug("Authorization header is empty");
            return null;
        }
        
        if (!headerAuth.startsWith("Bearer ")) {
            log.debug("Authorization header does not start with 'Bearer ': {}", 
                     headerAuth.length() > 10 ? headerAuth.substring(0, 10) + "..." : headerAuth);
            return null;
        }
        
        String token = headerAuth.substring(7);
        log.debug("Successfully extracted JWT token from Authorization header (length: {})", token.length());
        return token;
    }
}