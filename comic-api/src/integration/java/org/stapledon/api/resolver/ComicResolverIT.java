package org.stapledon.api.resolver;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ComicResolver GraphQL operations.
 * Extends AbstractHttpGraphQlIntegrationTest for proper GraphQL testing setup.
 */
@Slf4j
class ComicResolverIT extends AbstractHttpGraphQlIntegrationTest {

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
}
