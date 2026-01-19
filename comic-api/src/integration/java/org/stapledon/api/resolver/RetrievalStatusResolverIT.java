package org.stapledon.api.resolver;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;

/**
 * Integration tests for RetrievalStatusResolver GraphQL operations.
 */
@Slf4j
class RetrievalStatusResolverIT extends AbstractHttpGraphQlIntegrationTest {

    private static final String QUERY_RETRIEVAL_RECORDS = """
            query RetrievalRecords($comicName: String, $limit: Int) {
                retrievalRecords(comicName: $comicName, limit: $limit) {
                    id
                    comicName
                    status
                }
            }
            """;

    private static final String QUERY_RETRIEVAL_RECORDS_FOR_COMIC = """
            query RetrievalRecordsForComic($comicName: String!, $limit: Int) {
                retrievalRecordsForComic(comicName: $comicName, limit: $limit) {
                    id
                    comicName
                    status
                }
            }
            """;

    @Test
    void retrievalRecords_executesWithoutError() {
        getGraphQlTester()
                .document(QUERY_RETRIEVAL_RECORDS)
                .variable("limit", 10)
                .execute()
                .errors().verify();
    }

    @Test
    void retrievalRecordsForComic_executesWithoutError() {
        getGraphQlTester()
                .document(QUERY_RETRIEVAL_RECORDS_FOR_COMIC)
                .variable("comicName", "TestComic")
                .variable("limit", 5)
                .execute()
                .errors().verify();
    }
}
