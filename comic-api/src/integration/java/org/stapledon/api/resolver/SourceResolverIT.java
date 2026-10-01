package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.engine.downloader.ComicDownloaderStrategy;
import org.stapledon.engine.downloader.DailyComicDownloaderStrategy;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.engine.source.CatalogDetails;
import org.stapledon.engine.source.ComicSource;
import org.stapledon.engine.source.SourceCatalogEntry;
import org.stapledon.engine.source.SourceCatalogRepository;
import org.stapledon.engine.source.StartDetector;
import org.stapledon.engine.source.StartInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Integration tests for the Sources page's API and the comic mutations it relies on. A stub source stands in for the real ones so nothing touches
 * the network.
 */
class SourceResolverIT extends AbstractHttpGraphQlIntegrationTest {

    private static final String QUERY_SOURCES = """
            query Sources {
                sources { id displayName kind hasCatalog canDetectStart configuredCount catalogCount settings { throttleMaxDelayMs backfillMaxDaysBack } }
            }
            """;

    private static final String QUERY_SOURCE_CATALOG = """
            query SourceCatalog($id: String!, $filter: CatalogEntryFilter) {
                source(id: $id) {
                    configuredCount
                    catalog(filter: $filter) { totalCount edges { node { identifier name thumbnailUrl startDate comic { id name } } } }
                    orphans { id }
                }
            }
            """;

    private static final String MUTATION_ADD = """
            mutation Add($input: AddComicFromCatalogInput!) {
                addComicFromCatalog(input: $input) { comic { id name source sourceIdentifier active enabled sourceStartDate startSource } errors { field message code } }
            }
            """;

    private static final String MUTATION_CREATE = """
            mutation Create($input: CreateComicInput!) {
                createComic(input: $input) { comic { id name active enabled publicationDays firstStripNumber startSource } errors { field message code } }
            }
            """;

    private static final String MUTATION_UPDATE = """
            mutation Update($id: Int!, $input: UpdateComicInput!) {
                updateComic(id: $id, input: $input) { comic { id active enabled publicationDays sourceStartDate startSource } errors { field message } }
            }
            """;

    private static final String QUERY_COMIC = """
            query Comic($id: Int!) { comic(id: $id) { id name enabled } }
            """;

    private static final String QUERY_COMICS = """
            query Comics($includeHidden: Boolean) { comics(first: 50, includeHidden: $includeHidden) { totalCount edges { node { id } } } }
            """;

    @Autowired
    private SourceCatalogRepository catalogRepository;

    @Autowired
    private ManagementFacade comics;

    @LocalServerPort
    private int port;

    private final List<Integer> created = new ArrayList<>();

    @TestConfiguration
    static class StubSourceConfig {
        @Bean
        ComicSource stubSource() {
            DailyComicDownloaderStrategy downloader = Mockito.mock(DailyComicDownloaderStrategy.class);
            when(downloader.getSource()).thenReturn("stub");
            when(downloader.downloadAvatar(anyInt(), anyString(), anyString())).thenReturn(Optional.empty());
            return new ComicSource() {
                @Override
                public String id() {
                    return "stub";
                }

                @Override
                public String displayName() {
                    return "Stub Comics";
                }

                @Override
                public ComicDownloaderStrategy downloader() {
                    return downloader;
                }

                @Override
                public String identifierFor(ComicItem comic) {
                    return comic.getSourceIdentifier() != null ? comic.getSourceIdentifier() : comic.getName().toLowerCase().replace(" ", "");
                }

                @Override
                public String comicPageUrl(String identifier) {
                    return "https://stub.example/" + identifier;
                }

                @Override
                public Set<String> imageHosts() {
                    return Set.of();
                }

                @Override
                public Optional<StartDetector> startDetector() {
                    return Optional.of(_ -> Optional.of(StartInfo.ofDate(LocalDate.of(2001, 1, 1))));
                }
            };
        }
    }

    @AfterEach
    void removeCreatedComics() {
        created.forEach(comics::deleteComic);
        created.clear();
    }

    private void listInStubCatalog(String identifier, String name, LocalDate start) {
        catalogRepository.merge("stub", List.of(new SourceCatalogEntry(identifier, name, "Stub Author", null, start)));
    }

    @Test
    void sourcesAreHiddenFromReaders() {
        authenticateUser();

        getGraphQlTester().document(QUERY_SOURCES).execute()
                .errors().satisfy(errors -> assertThat(errors).anyMatch(e -> e.getMessage().contains("permission")));
    }

    @Test
    void operatorsSeeEveryRegisteredSource() {
        authenticateAsOperator();

        List<String> ids = getGraphQlTester().document(QUERY_SOURCES).execute()
                .errors().verify()
                .path("sources[*].id").entityList(String.class).get();

        assertThat(ids).contains("gocomics", "comicskingdom", "freefall", "stub");
    }

    @Test
    void catalogEntriesCarryDescriptionsAndTagsAndFilterByTag() {
        catalogRepository.merge("stub", List.of(
                new SourceCatalogEntry("funny", "Funny", null, null, null, new CatalogDetails("A funny one", List.of("Humor"))),
                new SourceCatalogEntry("sad", "Sad", null, null, null, new CatalogDetails(null, List.of("Drama", "Humor"))),
                new SourceCatalogEntry("plain", "Plain", null, null, null)));
        authenticateAsOperator();
        String query = """
                query Tagged($tag: String) {
                    source(id: "stub") {
                        tags
                        catalog(tag: $tag) { totalCount edges { node { identifier description tags } } }
                    }
                }
                """;

        // Other tests list their own stub comics, so only tagged entries are checked by position
        getGraphQlTester().document(query).variable("tag", "Humor").execute().errors().verify()
                .path("source.tags").entityList(String.class).containsExactly("Drama", "Humor")
                .path("source.catalog.edges[*].node.identifier").entityList(String.class).containsExactly("funny", "sad")
                .path("source.catalog.edges[0].node.description").entity(String.class).isEqualTo("A funny one")
                .path("source.catalog.edges[1].node.tags").entityList(String.class).containsExactly("Drama", "Humor");

        getGraphQlTester().document(query).variable("tag", "Drama").execute().errors().verify()
                .path("source.catalog.edges[*].node.identifier").entityList(String.class).containsExactly("sad");
    }

    @Test
    void operatorsCannotAddComics() {
        authenticateAsOperator();

        getGraphQlTester().document(MUTATION_ADD).variable("input", Map.of("source", "stub", "identifier", "anything")).execute()
                .errors().satisfy(errors -> assertThat(errors).anyMatch(e -> e.getMessage().contains("permission")));
    }

    @Test
    void adminAddsAComicFromTheCatalog() {
        listInStubCatalog("stubby", "Stubby", LocalDate.of(1999, 9, 9));
        authenticateAsAdmin();

        Map<String, Object> comic = getGraphQlTester().document(MUTATION_ADD)
                .variable("input", Map.of("source", "stub", "identifier", "stubby", "active", false, "enabled", true))
                .execute().errors().verify()
                .path("addComicFromCatalog.errors").entityList(Object.class).hasSize(0)
                .path("addComicFromCatalog.comic").entity(Map.class).get();
        created.add((Integer) comic.get("id"));

        assertThat(comic).containsEntry("name", "Stubby").containsEntry("source", "stub").containsEntry("sourceIdentifier", "stubby")
                .containsEntry("active", false).containsEntry("enabled", true).containsEntry("sourceStartDate", "1999-09-09")
                .containsEntry("startSource", "DETECTED");

        getGraphQlTester().document(QUERY_SOURCE_CATALOG).variable("id", "stub").variable("filter", "CONFIGURED").execute().errors().verify()
                .path("source.configuredCount").entity(Integer.class).isEqualTo(1)
                .path("source.catalog.edges[0].node.comic.name").entity(String.class).isEqualTo("Stubby");

        // Adding it twice is refused
        getGraphQlTester().document(MUTATION_ADD).variable("input", Map.of("source", "stub", "identifier", "stubby")).execute().errors().verify()
                .path("addComicFromCatalog.comic").valueIsNull()
                .path("addComicFromCatalog.errors[0].code").entity(String.class).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void createComicAssignsIdsAndKeepsEveryField() {
        authenticateAsAdmin();

        Map<String, Object> first = getGraphQlTester().document(MUTATION_CREATE)
                .variable("input", Map.of("name", "Created One", "source", "stub", "sourceIdentifier", "created-one", "active", false,
                        "publicationDays", List.of("MONDAY", "THURSDAY")))
                .execute().errors().verify()
                .path("createComic.comic").entity(Map.class).get();
        created.add((Integer) first.get("id"));
        Map<String, Object> second = getGraphQlTester().document(MUTATION_CREATE)
                .variable("input", Map.of("name", "Created Two", "source", "stub", "sourceIdentifier", "created-two"))
                .execute().errors().verify()
                .path("createComic.comic").entity(Map.class).get();
        created.add((Integer) second.get("id"));

        assertThat(first.get("id")).isNotEqualTo(second.get("id"));
        assertThat(first).containsEntry("active", false).containsEntry("publicationDays", List.of("MONDAY", "THURSDAY"));
        assertThat(second).containsEntry("active", true).containsEntry("enabled", true);
    }

    @Test
    void createComicRejectsABadIdentifier() {
        authenticateAsAdmin();

        getGraphQlTester().document(MUTATION_CREATE)
                .variable("input", Map.of("name", "Broken", "source", "stub", "sourceIdentifier", "../escape"))
                .execute().errors().verify()
                .path("createComic.comic").valueIsNull()
                .path("createComic.errors[0].field").entity(String.class).isEqualTo("input.sourceIdentifier");
    }

    @Test
    void updateComicSavesTheSwitchesAndAManualStart() {
        authenticateAsAdmin();
        Integer id = getGraphQlTester().document(MUTATION_CREATE)
                .variable("input", Map.of("name", "Switchy", "source", "stub", "sourceIdentifier", "switchy"))
                .execute().errors().verify()
                .path("createComic.comic.id").entity(Integer.class).get();
        created.add(id);

        getGraphQlTester().document(MUTATION_UPDATE)
                .variable("id", id)
                .variable("input", Map.of("active", false, "enabled", false, "publicationDays", List.of("SUNDAY"), "sourceStartDate", "2010-02-03"))
                .execute().errors().verify()
                .path("updateComic.comic.active").entity(Boolean.class).isEqualTo(false)
                .path("updateComic.comic.enabled").entity(Boolean.class).isEqualTo(false)
                .path("updateComic.comic.publicationDays").entityList(String.class).containsExactly("SUNDAY")
                .path("updateComic.comic.sourceStartDate").entity(String.class).isEqualTo("2010-02-03")
                .path("updateComic.comic.startSource").entity(String.class).isEqualTo("MANUAL");

        assertThat(comics.getComic(id).orElseThrow().isActive()).isFalse();
    }

    @Test
    void hiddenComicsAreOnlyVisibleToAdmins() {
        authenticateAsAdmin();
        Integer id = getGraphQlTester().document(MUTATION_CREATE)
                .variable("input", Map.of("name", "Hidden Away", "source", "stub", "sourceIdentifier", "hidden-away", "enabled", false))
                .execute().errors().verify()
                .path("createComic.comic.id").entity(Integer.class).get();
        created.add(id);

        getGraphQlTester().document(QUERY_COMIC).variable("id", id).execute().errors().verify().path("comic.id").entity(Integer.class).isEqualTo(id);
        int adminCount = getGraphQlTester().document(QUERY_COMICS).variable("includeHidden", true).execute().errors().verify()
                .path("comics.totalCount").entity(Integer.class).get();

        authenticateUser();
        getGraphQlTester().document(QUERY_COMIC).variable("id", id).execute().errors().verify().path("comic").valueIsNull();
        int readerCount = getGraphQlTester().document(QUERY_COMICS).variable("includeHidden", true).execute().errors().verify()
                .path("comics.totalCount").entity(Integer.class).get();

        assertThat(readerCount).isEqualTo(adminCount - 1);
    }

    @Test
    void thumbnailsAreServedFromCacheOnlyAndNeedNoSignIn() throws Exception {
        HttpResponse<Void> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/sources/stub/thumbnails/not-cached")).GET().build(),
                HttpResponse.BodyHandlers.discarding());

        assertThat(response.statusCode()).isEqualTo(404);
    }
}
