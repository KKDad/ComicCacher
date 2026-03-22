package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;

import lombok.extern.slf4j.Slf4j;

/**
 * Integration tests for ComicResolver GraphQL operations.
 * Extends AbstractHttpGraphQlIntegrationTest for proper GraphQL testing setup.
 */
@Slf4j
class ComicResolverIT extends AbstractHttpGraphQlIntegrationTest {

    // Test strip dates created by AbstractHttpGraphQlIntegrationTest
    private static final String TEST_STRIP_DATE = "2023-06-16";

    // --- GraphQL Queries ---
    private static final String QUERY_COMICS = """
            query Comics($first: Int, $search: String) {
                comics(first: $first, search: $search) {
                    edges {
                        node {
                            id
                            name
                            author
                            enabled
                        }
                        cursor
                    }
                    pageInfo {
                        hasNextPage
                        hasPreviousPage
                    }
                    totalCount
                }
            }
            """;

    private static final String QUERY_COMIC = """
            query Comic($id: Int!) {
                comic(id: $id) {
                    id
                    name
                    author
                    description
                    enabled
                    avatarUrl
                }
            }
            """;

    private static final String QUERY_SEARCH = """
            query Search($query: String!, $limit: Int) {
                search(query: $query, limit: $limit) {
                    comics {
                        id
                        name
                    }
                    totalCount
                    query
                }
            }
            """;

    private static final String QUERY_RANDOM_STRIP = """
            query RandomStrip($comicId: Int) {
                randomStrip(comicId: $comicId) {
                    date
                    available
                    imageUrl
                }
            }
            """;

    private static final String QUERY_STRIP_WINDOW = """
            query StripWindow($id: Int!, $center: Date!, $before: Int!, $after: Int!) {
                comic(id: $id) {
                    stripWindow(center: $center, before: $before, after: $after) {
                        date
                        available
                        imageUrl
                    }
                }
            }
            """;

    private static final String QUERY_STRIPS = """
            query Strips($id: Int!, $dates: [Date!]!) {
                comic(id: $id) {
                    strips(dates: $dates) {
                        date
                        available
                        imageUrl
                    }
                }
            }
            """;

    @BeforeEach
    void authenticate() {
        authenticateUser();
    }

    // =========================================================================
    // Query Tests
    // =========================================================================

    @Test
    void comics_returnsAllComics() {
        getGraphQlTester()
                .document(QUERY_COMICS)
                .variable("first", 10)
                .execute()
                .errors().verify()
                .path("comics.edges").entityList(Object.class).hasSizeGreaterThan(0)
                .path("comics.totalCount").entity(Integer.class).satisfies(count -> assertThat(count).isGreaterThan(0));
    }

    @Test
    void comics_withSearch_filtersResults() {
        getGraphQlTester()
                .document(QUERY_COMICS)
                .variable("first", 10)
                .variable("search", "Test")
                .execute()
                .errors().verify()
                .path("comics.edges").entityList(Object.class).hasSizeGreaterThan(0);
    }

    @Test
    void comics_withPagination_returnsPageInfo() {
        getGraphQlTester()
                .document(QUERY_COMICS)
                .variable("first", 1)
                .execute()
                .errors().verify()
                .path("comics.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(true)
                .path("comics.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(false);
    }

    @Test
    void comic_withValidId_returnsComic() {
        // Query for the test comic by ID - should return data after caching fix
        getGraphQlTester()
                .document(QUERY_COMIC)
                .variable("id", TEST_COMIC_ID)
                .execute()
                .errors().verify()
                .path("comic.id")
                .entity(Integer.class)
                .isEqualTo(TEST_COMIC_ID)
                .path("comic.name")
                .entity(String.class)
                .satisfies(name -> assertThat(name).isNotEmpty());
    }

    @Test
    void comic_withInvalidId_returnsNull() {
        getGraphQlTester()
                .document(QUERY_COMIC)
                .variable("id", 9999)
                .execute()
                .errors().verify()
                .path("comic").valueIsNull();
    }

    @Test
    void search_withMatchingQuery_returnsResults() {
        getGraphQlTester()
                .document(QUERY_SEARCH)
                .variable("query", "Test")
                .variable("limit", 10)
                .execute()
                .errors().verify()
                .path("search.query").entity(String.class).isEqualTo("Test")
                .path("search.totalCount").entity(Integer.class).satisfies(count -> assertThat(count).isGreaterThan(0))
                .path("search.comics").entityList(Object.class).hasSizeGreaterThan(0);
    }

    @Test
    void search_withNoMatches_returnsEmptyResults() {
        getGraphQlTester()
                .document(QUERY_SEARCH)
                .variable("query", "NonExistentComic12345")
                .variable("limit", 10)
                .execute()
                .errors().verify()
                .path("search.totalCount").entity(Integer.class).isEqualTo(0)
                .path("search.comics").entityList(Object.class).hasSize(0);
    }

    // =========================================================================
    // randomStrip
    // =========================================================================

    @Test
    void randomStrip_withComicId_returnsStrip() {
        getGraphQlTester()
                .document(QUERY_RANDOM_STRIP)
                .variable("comicId", TEST_COMIC_ID)
                .execute()
                .errors().verify()
                .path("randomStrip.available").entity(Boolean.class).isEqualTo(true)
                .path("randomStrip.date").entity(String.class).satisfies(date -> assertThat(date).isNotEmpty())
                .path("randomStrip.imageUrl").entity(String.class).satisfies(url -> assertThat(url).contains("/api/v1/comics/"));
    }

    @Test
    void randomStrip_withoutComicId_executesWithoutError() {
        // Without comicId, a random comic is picked. Some test comics may have
        // no strips, so the result can be null — we just verify no GraphQL errors.
        getGraphQlTester()
                .document(QUERY_RANDOM_STRIP)
                .execute()
                .errors().verify();
    }

    @Test
    void randomStrip_withInvalidComicId_returnsNull() {
        getGraphQlTester()
                .document(QUERY_RANDOM_STRIP)
                .variable("comicId", 9999)
                .execute()
                .errors().verify()
                .path("randomStrip").valueIsNull();
    }

    // =========================================================================
    // stripWindow
    // =========================================================================

    @Test
    void stripWindow_returnsCenteredWindow() {
        getGraphQlTester()
                .document(QUERY_STRIP_WINDOW)
                .variable("id", TEST_COMIC_ID)
                .variable("center", TEST_STRIP_DATE)
                .variable("before", 1)
                .variable("after", 1)
                .execute()
                .errors().verify()
                .path("comic.stripWindow").entityList(Object.class).hasSizeGreaterThan(0);
    }

    @Test
    void stripWindow_atBoundary_returnsFewerStrips() {
        // Request many before/after at a boundary — should still succeed
        getGraphQlTester()
                .document(QUERY_STRIP_WINDOW)
                .variable("id", TEST_COMIC_ID)
                .variable("center", "2023-06-15")
                .variable("before", 10)
                .variable("after", 10)
                .execute()
                .errors().verify()
                .path("comic.stripWindow").entityList(Object.class).hasSizeGreaterThan(0);
    }

    // =========================================================================
    // strips (batch date fetch)
    // =========================================================================

    @Test
    void strips_returnsStripsForRequestedDates() {
        getGraphQlTester()
                .document(QUERY_STRIPS)
                .variable("id", TEST_COMIC_ID)
                .variable("dates", java.util.List.of("2023-06-15", "2023-06-16", "2023-06-17"))
                .execute()
                .errors().verify()
                .path("comic.strips").entityList(Object.class).hasSize(3);
    }

    @Test
    void strips_withUnavailableDate_returnsNotAvailable() {
        getGraphQlTester()
                .document(QUERY_STRIPS)
                .variable("id", TEST_COMIC_ID)
                .variable("dates", java.util.List.of("2023-01-01"))
                .execute()
                .errors().verify()
                .path("comic.strips[0].available").entity(Boolean.class).isEqualTo(false);
    }
}
