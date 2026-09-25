package org.stapledon.infrastructure.config.devtoken;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

/**
 * Tests for DevTokenConfiguration.
 */
class DevTokenConfigurationTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "too-short")
    void refusesAWeakSecret(String secret) {
        assertThatThrownBy(() -> DevTokenConfiguration.validate(new DevTokenProperties("devuser", true, secret)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptsALongSecret() {
        assertThatCode(() -> DevTokenConfiguration.validate(new DevTokenProperties("devuser", true, "x".repeat(32))))
                .doesNotThrowAnyException();
    }

    @Test
    void schemaFileIsOnTheClasspath() {
        assertThatCode(() -> new ClassPathResource(DevTokenConfiguration.SCHEMA_RESOURCE).getContentAsString(StandardCharsets.UTF_8))
                .doesNotThrowAnyException();
    }
}
