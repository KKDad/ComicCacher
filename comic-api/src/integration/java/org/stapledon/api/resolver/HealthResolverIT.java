package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;

import lombok.extern.slf4j.Slf4j;

/**
 * Integration tests for HealthResolver GraphQL operations.
 */
@Slf4j
class HealthResolverIT extends AbstractHttpGraphQlIntegrationTest {

    private static final String QUERY_HEALTH = """
            query Health($detailed: Boolean) {
                health(detailed: $detailed) {
                    status
                    uptime
                    components {
                        name
                        status
                    }
                }
            }
            """;

    private static final String QUERY_HEALTH_DETAILED = """
            query HealthDetailed {
                health(detailed: true) {
                    status
                    uptime
                    buildInfo {
                        version
                    }
                    systemResources {
                        availableProcessors
                        totalMemory
                        freeMemory
                        memoryUsagePercent
                    }
                    components {
                        name
                        status
                    }
                }
            }
            """;

    private static final String QUERY_ERROR_CODES = """
            query ErrorCodes {
                errorCodes
            }
            """;

    @Test
    void health_returnsStatus() {
        getGraphQlTester()
                .document(QUERY_HEALTH)
                .variable("detailed", false)
                .execute()
                .errors().verify()
                .path("health.status").entity(String.class).satisfies(status -> assertThat(status).isNotEmpty())
                .path("health.uptime").entity(Double.class).satisfies(uptime -> assertThat(uptime).isGreaterThanOrEqualTo(0));
    }

    @Test
    void health_detailed_returnsSystemResources() {
        getGraphQlTester()
                .document(QUERY_HEALTH_DETAILED)
                .execute()
                .errors().verify()
                .path("health.status").entity(String.class).satisfies(status -> assertThat(status).isNotEmpty())
                .path("health.systemResources.availableProcessors").entity(Integer.class).satisfies(p -> assertThat(p).isGreaterThan(0));
    }

    @Test
    void errorCodes_returnsList() {
        getGraphQlTester()
                .document(QUERY_ERROR_CODES)
                .execute()
                .errors().verify()
                .path("errorCodes").entityList(String.class).hasSizeGreaterThan(0);
    }
}
