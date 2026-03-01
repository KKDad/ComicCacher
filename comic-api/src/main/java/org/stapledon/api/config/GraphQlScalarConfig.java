package org.stapledon.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import graphql.scalars.ExtendedScalars;

/**
 * Configuration for custom GraphQL scalars.
 * Registers Date, DateTime, LocalDateTime, and JSON scalars for use in the
 * GraphQL schema.
 */
@Configuration
public class GraphQlScalarConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.LocalTime)
                .scalar(ExtendedScalars.Json);
    }
}
