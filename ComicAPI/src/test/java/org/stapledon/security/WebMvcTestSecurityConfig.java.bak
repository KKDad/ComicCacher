package org.stapledon.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration specifically for WebMvc tests
 * This configuration disables security features and provides test users
 */
@TestConfiguration
@EnableWebSecurity
public class WebMvcTestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Completely disable security for WebMvc tests
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(AbstractHttpConfigurer::disable)
            .securityMatcher("/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        
        return http.build();
    }
    
    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        // Create a test user for tests that need authentication
        UserDetails testUser = User.builder()
                .username("testuser")
                .password("{noop}testpassword") // {noop} prefix to use plain text password
                .roles("USER")
                .build();
        
        UserDetails testAdmin = User.builder()
                .username("testadmin")
                .password("{noop}testpassword")
                .roles("USER", "ADMIN")
                .build();
        
        return new InMemoryUserDetailsManager(testUser, testAdmin);
    }
    
    // Mock the JWT filter to avoid token validation
    @Bean
    @Primary
    public JwtTokenFilter jwtTokenFilter() {
        return new JwtTokenFilter(null, null) {
            @Override
            protected boolean shouldNotFilter(jakarta.servlet.http.HttpServletRequest request) {
                return true; // Skip filter for all requests in tests
            }
        };
    }
}