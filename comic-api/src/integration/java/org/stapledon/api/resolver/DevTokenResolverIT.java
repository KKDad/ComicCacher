package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;

/**
 * The devToken mutation, with the dev instance's settings turned on.
 */
@TestPropertySource(properties = {
    "comics.dev-token.enabled=true",
    "comics.dev-token.secret=" + DevTokenResolverIT.SECRET,
    "comics.dev-token.default-username=testuser"
})
class DevTokenResolverIT extends AbstractHttpGraphQlIntegrationTest {

    static final String SECRET = "integration-dev-token-secret-0123456789";

    private static final String DEV_TOKEN = """
            mutation DevToken($secret: String!) {
              devToken(secret: $secret) { token username }
            }""";

    @Test
    void issuedTokenAuthenticatesRequests() {
        givens.givenUser();

        String token = graphQlTester.document(DEV_TOKEN)
                .variable("secret", SECRET)
                .execute()
                .errors().verify()
                .path("devToken.username").entity(String.class).isEqualTo(TEST_USER)
                .path("devToken.token").entity(String.class).get();

        graphQlTester.mutate().headers(h -> h.setBearerAuth(token)).build()
                .document("query { validateToken }")
                .execute()
                .errors().verify()
                .path("validateToken").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    void wrongSecretIsRejected() {
        graphQlTester.document(DEV_TOKEN)
                .variable("secret", "wrong")
                .execute()
                .errors().satisfy(errors -> assertThat(errors)
                        .anySatisfy(e -> assertThat(e.getExtensions()).containsEntry("errorCode", "UNAUTHENTICATED")));
    }
}
