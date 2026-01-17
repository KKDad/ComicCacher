package org.stapledon.comics.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import lombok.Getter;

/**
 * Base class for GraphQL integration tests. Provides a pre-configured {@link HttpGraphQlTester} for subclasses.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@Getter
abstract class BaseGraphQLIntegrationTest {

    @Autowired
    protected HttpGraphQlTester graphQlTester;
}
