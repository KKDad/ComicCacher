package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Integration test for AuthResolver GraphQL operations.
 * Updated for Spring Boot 4 standards.
 */
@Slf4j
class AuthResolverIT extends AbstractHttpGraphQlIntegrationTest {

    // --- GraphQL Constants ---
    private static final String QUERY_VALIDATE_TOKEN = "query { validateToken }";

    private static final String MUTATION_REGISTER = """
            mutation Register($input: RegisterInput!) {
                register(input: $input) {
                    token
                    refreshToken
                    username
                    displayName
                }
            }
            """;

    private static final String MUTATION_LOGIN = """
            mutation Login($input: LoginInput!) {
                login(input: $input) {
                    token
                    refreshToken
                    username
                }
            }
            """;

    private static final String TEST_PASSWORD = "password123";

    @Test
    void validateToken_withoutAuthentication_returnsFalse() {
        getGraphQlTester()
                .document(QUERY_VALIDATE_TOKEN)
                .execute()
                .errors().verify() // Ensure no unexpected errors
                .path("validateToken")
                .entity(Boolean.class)
                .isEqualTo(false);
    }

    @Test
    void register_withValidInput_returnsAuthPayload() {
        String username = generateUsername("reg");
        var input = new AuthResolver.RegisterInput(username, TEST_PASSWORD, username + "@test.com", "Test User");

        getGraphQlTester()
                .document(MUTATION_REGISTER)
                .variable("input", input)
                .execute()
                .errors().verify()
                .path("register.username")
                .entity(String.class)
                .isEqualTo(username)
                .path("register.token")
                .entity(String.class)
                .satisfies(token -> assertThat(token).isNotEmpty());
    }

    @Test
    void login_withValidCredentials_returnsAuthPayload() {
        String username = generateUsername("login");
        var regInput = new AuthResolver.RegisterInput(username, TEST_PASSWORD, username + "@test.com", "Login Test");
        var loginInput = new AuthResolver.LoginInput(username, TEST_PASSWORD);

        // 1. Register the user using the inherited authentication context
        getGraphQlTester()
                .mutate()
                .headers(h -> h.setBearerAuth(authenticateUser()))
                .build()
                .document(MUTATION_REGISTER)
                .variable("input", regInput)
                .execute()
                .errors().verify()
                .path("register.username")
                .hasValue();

        // 2. Perform Login
        getGraphQlTester()
                .document(MUTATION_LOGIN)
                .variable("input", loginInput)
                .execute()
                .path("login.username")
                .entity(String.class)
                .isEqualTo(username)
                .path("login.token")
                .entity(String.class)
                .satisfies(token -> assertThat(token).isNotEmpty());
    }

    /**
     * Helper to generate unique usernames for test isolation.
     */
    private String generateUsername(String prefix) {
        return prefix + "_" + System.currentTimeMillis();
    }
}
