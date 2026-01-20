package org.stapledon.api.resolver;

import org.junit.jupiter.api.Test;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;

import lombok.extern.slf4j.Slf4j;

/**
 * Integration tests for MetricsResolver GraphQL operations.
 * Extends AbstractHttpGraphQlIntegrationTest for proper GraphQL testing setup.
 * 
 * Note: Metrics queries may return null in integration tests since metrics
 * files may not exist. Tests verify no unexpected errors occur.
 */
@Slf4j
class MetricsResolverIT extends AbstractHttpGraphQlIntegrationTest {

    // --- GraphQL Queries ---
    // Use minimal queries since DTOs may not perfectly match schema
    private static final String QUERY_STORAGE_METRICS = """
            query {
                storageMetrics {
                    totalBytes
                    comicCount
                }
            }
            """;

    private static final String QUERY_ACCESS_METRICS = """
            query {
                accessMetrics {
                    totalAccesses
                }
            }
            """;

    private static final String QUERY_COMBINED_METRICS = """
            query {
                combinedMetrics {
                    lastUpdated
                }
            }
            """;

    // =========================================================================
    // Query Tests
    // =========================================================================

    @Test
    void storageMetrics_executesWithoutError() {
        // Metrics may be null in integration tests - just verify no GraphQL errors
        getGraphQlTester()
                .document(QUERY_STORAGE_METRICS)
                .execute()
                .errors().verify();
    }

    @Test
    void accessMetrics_executesWithoutError() {
        getGraphQlTester()
                .document(QUERY_ACCESS_METRICS)
                .execute()
                .errors().verify();
    }

    @Test
    void combinedMetrics_executesWithoutError() {
        getGraphQlTester()
                .document(QUERY_COMBINED_METRICS)
                .execute()
                .errors().verify();
    }
}
