package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.GraphQlSource;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;

/**
 * With the default settings (as in production) the devToken mutation doesn't exist.
 */
class DevTokenDisabledIT extends AbstractHttpGraphQlIntegrationTest {

    @Autowired
    private GraphQlSource graphQlSource;

    @Test
    void devTokenIsNotInTheSchema() {
        assertThat(graphQlSource.schema().getMutationType().getFieldDefinition("devToken")).isNull();
    }
}
