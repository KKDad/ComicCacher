package org.stapledon.api.resolver;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PreferenceResolver GraphQL operations.
 * Extends AbstractHttpGraphQlIntegrationTest for proper GraphQL testing setup.
 */
@Slf4j
class PreferenceResolverIT extends AbstractHttpGraphQlIntegrationTest {

    // --- GraphQL Queries ---
    private static final String QUERY_PREFERENCES = """
            query {
                preferences {
                    username
                    favoriteComics
                }
            }
            """;

    private static final String MUTATION_ADD_FAVORITE = """
            mutation AddFavorite($comicId: Int!) {
                addFavorite(comicId: $comicId) {
                    username
                    favoriteComics
                }
            }
            """;

    private static final String MUTATION_REMOVE_FAVORITE = """
            mutation RemoveFavorite($comicId: Int!) {
                removeFavorite(comicId: $comicId) {
                    username
                    favoriteComics
                }
            }
            """;

    private static final String MUTATION_UPDATE_LAST_READ = """
            mutation UpdateLastRead($comicId: Int!, $date: Date!) {
                updateLastRead(comicId: $comicId, date: $date) {
                    username
                    lastReadDates {
                        comicId
                        date
                    }
                }
            }
            """;

    // =========================================================================
    // Query Tests
    // =========================================================================

    @Test
    void preferences_withoutAuthentication_returnsUnauthorizedError() {
        getGraphQlTester()
                .document(QUERY_PREFERENCES)
                .execute()
                .errors()
                .expect(e -> e.getMessage().contains("Unauthorized"))
                .verify();
    }

    @Test
    void preferences_withAuthentication_returnsPreferences() {
        String jwtToken = authenticateUser();

        getGraphQlTester()
                .mutate()
                .headers(h -> h.setBearerAuth(jwtToken))
                .build()
                .document(QUERY_PREFERENCES)
                .execute()
                .errors().verify()
                .path("preferences.username")
                .entity(String.class)
                .satisfies(username -> assertThat(username).isNotEmpty());
    }

    // =========================================================================
    // Mutation Tests
    // =========================================================================

    @Test
    void addFavorite_withAuthentication_addsComic() {
        String jwtToken = authenticateUser();

        getGraphQlTester()
                .mutate()
                .headers(h -> h.setBearerAuth(jwtToken))
                .build()
                .document(MUTATION_ADD_FAVORITE)
                .variable("comicId", TEST_COMIC_ID)
                .execute()
                .errors().verify()
                .path("addFavorite.favoriteComics")
                .entityList(Integer.class)
                .satisfies(favorites -> assertThat(favorites).contains(TEST_COMIC_ID));
    }

    @Test
    void removeFavorite_withAuthentication_removesComic() {
        String jwtToken = authenticateUser();

        // First add a favorite
        getGraphQlTester()
                .mutate()
                .headers(h -> h.setBearerAuth(jwtToken))
                .build()
                .document(MUTATION_ADD_FAVORITE)
                .variable("comicId", TEST_COMIC_ID)
                .execute()
                .errors().verify();

        // Then remove it
        getGraphQlTester()
                .mutate()
                .headers(h -> h.setBearerAuth(jwtToken))
                .build()
                .document(MUTATION_REMOVE_FAVORITE)
                .variable("comicId", TEST_COMIC_ID)
                .execute()
                .errors().verify()
                .path("removeFavorite.favoriteComics")
                .entityList(Integer.class)
                .satisfies(favorites -> assertThat(favorites).doesNotContain(TEST_COMIC_ID));
    }

    @Test
    void updateLastRead_withAuthentication_updatesDate() {
        String jwtToken = authenticateUser();
        LocalDate today = LocalDate.now();

        getGraphQlTester()
                .mutate()
                .headers(h -> h.setBearerAuth(jwtToken))
                .build()
                .document(MUTATION_UPDATE_LAST_READ)
                .variable("comicId", TEST_COMIC_ID)
                .variable("date", today.toString())
                .execute()
                .errors().verify()
                .path("updateLastRead.lastReadDates")
                .entityList(Object.class)
                .hasSizeGreaterThan(0);
    }
}
