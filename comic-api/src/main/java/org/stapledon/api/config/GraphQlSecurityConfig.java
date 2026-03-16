package org.stapledon.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import graphql.analysis.MaxQueryDepthInstrumentation;

/**
 * GraphQL security configuration.
 * Limits query depth to prevent deeply nested queries from overloading the server.
 */
@Configuration
public class GraphQlSecurityConfig {

    private static final int MAX_QUERY_DEPTH = 10;

    /**
     * Limits the maximum depth of GraphQL queries.
     */
    @Bean
    public MaxQueryDepthInstrumentation maxQueryDepthInstrumentation() {
        return new MaxQueryDepthInstrumentation(MAX_QUERY_DEPTH);
    }
}
