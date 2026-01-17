package org.stapledon.comics.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

/**
 * Integration tests for the errorCodes query.
 */
class ErrorCodesIT extends BaseGraphQLIntegrationTest {

    @Autowired
    private ResourcePatternResolver resourceResolver;

    @Test
    void printSchemas() throws IOException {
        Resource[] resources = resourceResolver.getResources("classpath*:graphql/**.graphql");
        for (Resource r : resources) {
            System.out.println("Found Schema: " + r.getURL());
        }
    }

    /**
     * Verifies that the errorCodes query returns a list of error codes.
     */
    @Test
    void shouldReturnErrorCodes() {
        graphQlTester
            .document("query { errorCodes }")
            .execute()
            .path("errorCodes")
            .entityList(String.class)
            .satisfies(codes -> {
                assertThat(codes).hasSize(5);
                assertThat(codes).contains("UNAUTHENTICATED", "FORBIDDEN");
            });
    }
}
