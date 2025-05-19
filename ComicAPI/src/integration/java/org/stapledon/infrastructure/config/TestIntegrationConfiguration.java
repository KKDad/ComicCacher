package org.stapledon.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.stapledon.common.util.Bootstrap;

/**
 * Main integration test configuration that brings together all the necessary beans
 * for running integration tests.
 */
@Configuration
@Profile("integration")
@Import({IntegrationTestConfig.class})
public class TestIntegrationConfiguration {
    
    /**
     * Provides a test implementation of Bootstrap for integration tests
     * This avoids serialization issues with the IComicsBootstrap interface
     */
    @Bean
    @Primary
    public Bootstrap testBootstrap() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.setDailyComics(new java.util.ArrayList<>());
        bootstrap.setKingComics(new java.util.ArrayList<>());
        return bootstrap;
    }
}