package org.stapledon.api.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;

import lombok.extern.slf4j.Slf4j;

/**
 * Integration tests for BatchJobResolver GraphQL operations.
 * Batch jobs are disabled in integration profile, so queries return empty lists.
 */
@Slf4j
class BatchJobResolverIT extends AbstractHttpGraphQlIntegrationTest {

    private static final String QUERY_RECENT_BATCH_JOBS = """
            query RecentBatchJobs($count: Int) {
                recentBatchJobs(count: $count) {
                    executionId
                    jobName
                    status
                    startTime
                }
            }
            """;

    private static final String QUERY_BATCH_JOB_SUMMARY = """
            query BatchJobSummary($days: Int) {
                batchJobSummary(days: $days) {
                    daysIncluded
                    totalExecutions
                    successCount
                    failureCount
                    runningCount
                }
            }
            """;

    @BeforeEach
    void authenticate() {
        authenticateUser();
    }

    @Test
    void recentBatchJobs_returnsList() {
        getGraphQlTester()
                .document(QUERY_RECENT_BATCH_JOBS)
                .variable("count", 5)
                .execute()
                .errors().verify()
                .path("recentBatchJobs").entityList(Object.class).hasSize(0);
    }

    @Test
    void batchJobSummary_returnsSummary() {
        getGraphQlTester()
                .document(QUERY_BATCH_JOB_SUMMARY)
                .variable("days", 7)
                .execute()
                .errors().verify()
                .path("batchJobSummary.daysIncluded").entity(Integer.class).isEqualTo(7)
                .path("batchJobSummary.totalExecutions").entity(Integer.class).isEqualTo(0);
    }
}
