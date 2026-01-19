package org.stapledon.api.resolver;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for HealthResolver GraphQL operations.
 */
@Slf4j
class HealthResolverIT extends AbstractHttpGraphQlIntegrationTest {

    private static final String QUERY_HEALTH = """
            query Health($detailed: Boolean) {
                health(detailed: $detailed) {
                    status
                }
            }
            """;

    @Test
    void health_basic_returnsStatus() {
        getGraphQlTester()
                .document(QUERY_HEALTH)
                .variable("detailed", false)
                .execute()
                .errors().verify()
                .path("health.status")
                .entity(String.class)
                .satisfies(status -> assertThat(status).isIn("UP", "DEGRADED", "DOWN"));
    }

    @Test
    void health_detailed_returnsDetailedStatus() {
        getGraphQlTester()
                .document(QUERY_HEALTH)
                .variable("detailed", true)
                .execute()
                .errors().verify()
                .path("health.status")
                .entity(String.class)
                .satisfies(status -> assertThat(status).isIn("UP", "DEGRADED", "DOWN"));
    }
}
