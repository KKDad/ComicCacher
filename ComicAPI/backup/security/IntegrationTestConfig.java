package org.stapledon.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.stapledon.config.properties.JwtProperties;
import org.stapledon.security.JwtAuthenticationEntryPoint;

@TestConfiguration
@EnableWebSecurity
@Profile("test") // Only activate in test profile, not integration
public class IntegrationTestConfig {

    /**
     * Configure security for tests
     * For simplicity of initial testing, we're disabling security filters
     * NOTE: Disabled for integration tests to avoid filter chain conflicts
     */
    @Bean
    @Primary
    @Profile("test") // Explicitly mark as test-only
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().permitAll()
            );

        return http.build();
    }
    
    /**
     * Password encoder for integration tests
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * JWT properties for integration tests
     */
    @Bean
    @Primary
    public JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("integration-test-secret-key-very-long-and-secure-for-testing");
        properties.setExpiration(300000); // 5 minutes
        properties.setRefreshExpiration(600000); // 10 minutes
        return properties;
    }
    
    /**
     * JWT authentication entry point for integration tests
     */
    @Bean
    @Primary
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }
}