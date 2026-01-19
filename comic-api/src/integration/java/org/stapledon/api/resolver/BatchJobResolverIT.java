package org.stapledon.api.resolver;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;

/**
 * Integration tests for BatchJobResolver GraphQL operations.
 * Note: Batch jobs are disabled in integration tests
 * (batch.comic-download.enabled=false),
 * so the BatchJobResolver bean is not created. These tests verify the expected
 * null-value errors occur when querying batch job endpoints.
 */
@Slf4j
class BatchJobResolverIT extends AbstractHttpGraphQlIntegrationTest {

    private static final String QUERY_RECENT_BATCH_JOBS = """
            query RecentBatchJobs($count: Int) {
                recentBatchJobs(count: $count) {
                    executionId
                    jobName
                    status
                }
            }
            """;

    @Test
    void recentBatchJobs_whenBatchDisabled_returnsNullError() {
        // Batch jobs are disabled in integration tests, so the resolver returns null
        // which violates the non-null schema. We expect this error.
        getGraphQlTester()
                .document(QUERY_RECENT_BATCH_JOBS)
                .variable("count", 5)
                .execute()
                .errors()
                .expect(e -> e.getMessage().contains("non null type") || e.getMessage().contains("null value"))
                .verify();
    }
}
