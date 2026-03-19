package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;

import lombok.extern.slf4j.Slf4j;

/**
 * Integration tests for RetrievalResolver GraphQL operations.
 */
@Slf4j
class RetrievalResolverIT extends AbstractHttpGraphQlIntegrationTest {

    private static final String QUERY_RETRIEVAL_RECORDS = """
            query RetrievalRecords($limit: Int) {
                retrievalRecords(limit: $limit) {
                    id
                    comicName
                    comicDate
                    status
                }
            }
            """;

    private static final String QUERY_RETRIEVAL_SUMMARY = """
            query RetrievalSummary {
                retrievalSummary {
                    totalAttempts
                    successCount
                    failureCount
                    skippedCount
                    successRate
                }
            }
            """;

    @BeforeEach
    void authenticate() {
        authenticateAsOperator();
    }

    @Test
    void retrievalRecords_returnsList() {
        getGraphQlTester()
                .document(QUERY_RETRIEVAL_RECORDS)
                .variable("limit", 10)
                .execute()
                .errors().verify()
                .path("retrievalRecords").entityList(Object.class).satisfies(list -> assertThat(list).isNotNull());
    }

    @Test
    void retrievalSummary_returnsSummaryStructure() {
        getGraphQlTester()
                .document(QUERY_RETRIEVAL_SUMMARY)
                .execute()
                .errors().verify()
                .path("retrievalSummary.totalAttempts").entity(Integer.class).satisfies(count -> assertThat(count).isGreaterThanOrEqualTo(0))
                .path("retrievalSummary.successRate").entity(Double.class).satisfies(rate -> assertThat(rate).isGreaterThanOrEqualTo(0.0));
    }
}
