package org.stapledon;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Smoke tests for the GraphQL endpoint.
 */
class SmokeIT extends AbstractHttpGraphQlIntegrationTest {

    /**
     * Verifies that the GraphQL endpoint is reachable and responds. The `validateToken` query should return false without an auth header.
     */
    @Test
    void graphqlEndpointIsAlive() {
        getGraphQlTester()
            .document("query { validateToken }")
            .execute()
            .path("validateToken")
            .entity(Boolean.class)
            .satisfies(Assertions::assertFalse);
    }
}
