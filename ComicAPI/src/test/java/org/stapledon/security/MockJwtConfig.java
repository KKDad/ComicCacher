package org.stapledon.security;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.stapledon.config.properties.JwtProperties;

@TestConfiguration
public class MockJwtConfig {

    @Bean
    @Primary
    public JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-for-unit-tests-should-be-long-enough");
        properties.setExpiration(300000); // 5 minutes
        properties.setRefreshExpiration(600000); // 10 minutes
        return properties;
    }
    
    @Bean
    @Primary
    public JwtTokenUtil jwtTokenUtil() {
        return new JwtTokenUtil(jwtProperties());
    }
    
    @Bean
    @Primary
    public JwtUserDetailsService jwtUserDetailsService() {
        return Mockito.mock(JwtUserDetailsService.class);
    }
    
    @Bean
    @Primary
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }
}