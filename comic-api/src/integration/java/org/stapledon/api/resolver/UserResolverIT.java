package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;

import lombok.extern.slf4j.Slf4j;

/**
 * Integration tests for UserResolver GraphQL operations.
 * Extends AbstractHttpGraphQlIntegrationTest for proper GraphQL testing setup.
 */
@Slf4j
class UserResolverIT extends AbstractHttpGraphQlIntegrationTest {

    // --- GraphQL Queries ---
    private static final String QUERY_ME = """
            query {
                me {
                    username
                    email
                    displayName
                    roles
                }
            }
            """;

    private static final String MUTATION_UPDATE_PROFILE = """
            mutation UpdateProfile($input: UpdateProfileInput!) {
                updateProfile(input: $input) {
                    username
                    email
                    displayName
                }
            }
            """;

    private static final String MUTATION_UPDATE_PASSWORD = """
            mutation UpdatePassword($newPassword: String!) {
                updatePassword(newPassword: $newPassword)
            }
            """;

    // =========================================================================
    // Query Tests
    // =========================================================================

    @Test
    void me_withoutAuthentication_returnsUnauthorizedError() {
        // The 'me' query has @authenticated directive, so it returns an UNAUTHORIZED
        // error
        getGraphQlTester()
                .document(QUERY_ME)
                .execute()
                .errors()
                .expect(e -> e.getMessage().contains("Unauthorized"))
                .verify();
    }

    @Test
    void me_withAuthentication_returnsUserProfile() {
        // Create and authenticate a test user
        String jwtToken = authenticateUser();

        getGraphQlTester()
                .mutate()
                .headers(h -> h.setBearerAuth(jwtToken))
                .build()
                .document(QUERY_ME)
                .execute()
                .errors().verify()
                .path("me.username")
                .entity(String.class)
                .satisfies(username -> assertThat(username).isNotEmpty())
                .path("me.roles")
                .entityList(String.class)
                .hasSizeGreaterThan(0);
    }

    // =========================================================================
    // Mutation Tests
    // =========================================================================

    @Test
    void updateProfile_withAuthentication_updatesDisplayName() {
        // Create and authenticate a test user
        String jwtToken = authenticateUser();

        var input = new UserResolver.UpdateProfileInput(null, "Updated Display Name");

        getGraphQlTester()
                .mutate()
                .headers(h -> h.setBearerAuth(jwtToken))
                .build()
                .document(MUTATION_UPDATE_PROFILE)
                .variable("input", input)
                .execute()
                .errors().verify()
                .path("updateProfile.displayName")
                .entity(String.class)
                .isEqualTo("Updated Display Name");
    }

    @Test
    void updatePassword_withAuthentication_returnsTrue() {
        // Create and authenticate a test user
        String jwtToken = authenticateUser();

        getGraphQlTester()
                .mutate()
                .headers(h -> h.setBearerAuth(jwtToken))
                .build()
                .document(MUTATION_UPDATE_PASSWORD)
                .variable("newPassword", "newSecurePassword123!")
                .execute()
                .errors().verify()
                .path("updatePassword")
                .entity(Boolean.class)
                .isEqualTo(true);
    }
}
