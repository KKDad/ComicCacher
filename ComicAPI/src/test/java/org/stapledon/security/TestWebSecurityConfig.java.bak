package org.stapledon.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableWebSecurity
@Profile("test") // Only activate in test profile, not integration
public class TestWebSecurityConfig {

    @Bean
    @Primary
    @Profile("test") // Only activate for test profile, not integration
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        // Configure security for tests - permit all for simplicity in tests
        // Add a specific matcher to avoid conflict with other filter chains
        http.securityMatcher("/test/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
    
    @Bean
    @Primary
    public UserDetailsService testUserDetailsService() {
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
}