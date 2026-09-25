package org.stapledon.infrastructure.config.devtoken;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import lombok.extern.slf4j.Slf4j;

/**
 * Wires the dev-only {@code devToken} mutation: adds its schema file, which lives outside
 * the scanned {@code graphql/} locations, and refuses to start with a weak secret.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "comics.dev-token", name = "enabled", havingValue = "true")
public class DevTokenConfiguration {

    static final int MIN_SECRET_LENGTH = 32;
    static final String SCHEMA_RESOURCE = "graphql-dev/dev-token.graphql";

    @Bean
    public GraphQlSourceBuilderCustomizer devTokenSchemaCustomizer(DevTokenProperties properties) {
        validate(properties);
        log.warn("Dev token mutation is ENABLED - tokens can be issued without a password. This must never be on in production");
        return builder -> builder.schemaResources(new ClassPathResource(SCHEMA_RESOURCE));
    }

    static void validate(DevTokenProperties properties) {
        if (properties.secret() == null || properties.secret().length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("comics.dev-token.secret must be at least " + MIN_SECRET_LENGTH + " characters when comics.dev-token.enabled=true");
        }
    }
}
