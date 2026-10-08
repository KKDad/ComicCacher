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

    private static final String QUERY_RETRIEVAL_HEALTH = """
            query RetrievalHealth($days: Int) {
                retrievalHealth(days: $days) {
                    targetDate
                    lastRun { executionId status }
                    sources { source success unavailable rateLimited failed }
                    todaysErrors { recovered record { id status errorMessage } }
                    comics {
                        comicId
                        comicName
                        stale
                        missingStreak
                        days { date outcome recovered record { status } }
                    }
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

    @Test
    void retrievalHealth_returnsADayPerComicForTheWindow() {
        getGraphQlTester()
                .document(QUERY_RETRIEVAL_HEALTH)
                .variable("days", 14)
                .execute()
                .errors().verify()
                .path("retrievalHealth.targetDate").entity(String.class).satisfies(date -> assertThat(date).isNotBlank())
                .path("retrievalHealth.comics").entityList(Object.class).satisfies(list -> assertThat(list).isNotEmpty())
                .path("retrievalHealth.comics[0].days").entityList(Object.class).hasSize(14)
                .path("retrievalHealth.comics[0].days[0].outcome").entity(String.class)
                .satisfies(outcome -> assertThat(outcome).isIn("ON_DISK", "MISSING", "OFF_DAY", "PENDING"));
    }

    @Test
    void retrievalHealth_rejectsUserRole() {
        authenticateUser();
        getGraphQlTester()
                .document(QUERY_RETRIEVAL_HEALTH)
                .execute()
                .errors().satisfy(errors -> assertThat(errors).anyMatch(e -> e.getMessage().contains("permission")));
    }
}
