package org.stapledon.engine.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.stapledon.common.config.properties.DownloaderProperties;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.infrastructure.web.UserAgentService;
import org.stapledon.engine.downloader.BrowserFetcher;
import org.stapledon.engine.downloader.ComicsKingdomDownloaderStrategy;
import org.stapledon.engine.downloader.SourceThrottleService;

class ComicsKingdomSourceTest {

    private HttpServer server;
    private ComicsKingdomSource source;
    private final List<String> queries = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.start();
        UserAgentService userAgents = mock(UserAgentService.class);
        when(userAgents.getUserAgent(anyString())).thenReturn("Mozilla/5.0 Chrome/154.0.0.0 Safari/537.36");
        source = new ComicsKingdomSource(mock(ComicsKingdomDownloaderStrategy.class), new BrowserFetcher(userAgents),
                new SourceThrottleService(DownloaderProperties.builder().build()), "http://127.0.0.1:" + server.getAddress().getPort() + "/features");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private static String fixture() throws IOException {
        try (InputStream in = Objects.requireNonNull(ComicsKingdomSourceTest.class.getResourceAsStream("/catalog/comicskingdom-features.json"))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void serve(String body) {
        server.createContext("/features", exchange -> {
            queries.add(exchange.getRequestURI().getRawQuery());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    @Test
    void parseFeaturesReadsNamesBylinesImagesAndStartDates() throws IOException {
        List<SourceCatalogEntry> entries = ComicsKingdomSource.parseFeatures(JsonParser.parseString(fixture()).getAsJsonArray());

        assertThat(entries).extracting(SourceCatalogEntry::identifier)
                .containsExactly("wannabe", "mostly-gravy", "phantom-2040-a-new-shadow", "hagar-the-horrible");
        SourceCatalogEntry wannabe = entries.getFirst();
        assertThat(wannabe.name()).isEqualTo("Wannabe");
        assertThat(wannabe.author()).isEqualTo("Luca Debus");
        assertThat(wannabe.startDate()).isEqualTo(LocalDate.of(2024, 1, 14));
        assertThat(wannabe.thumbnailUrl()).startsWith("https://wp.comicskingdom.com/");
        assertThat(wannabe.details().description()).isEqualTo("A young artist’s life in Berlin.");
        assertThat(wannabe.details().tags()).containsExactly("Slice Of Life", "Humor");
        SourceCatalogEntry hagar = entries.getLast();
        assertThat(hagar.name()).isEqualTo("Hagar – The Horrible");
        assertThat(hagar.thumbnailUrl()).isNull();
        // The catalog carries details, so they are never fetched separately, even when empty
        assertThat(hagar.details()).isEqualTo(new CatalogDetails(null, List.of()));
    }

    @Test
    void fetchStopsAtAShortPage() throws IOException {
        serve(fixture());

        List<SourceCatalogEntry> entries = source.catalog().orElseThrow().fetch();

        assertThat(entries).hasSize(4);
        assertThat(queries).singleElement().satisfies(query -> {
            assertThat(query).contains("per_page=100").contains("page=1").contains("_embed=wp:featuredmedia");
        });
    }

    @Test
    void fetchTreatsAnEmptyListAsAFailure() {
        serve("[]");

        assertThatThrownBy(() -> source.catalog().orElseThrow().fetch()).isInstanceOf(IOException.class).hasMessageContaining("No comics found");
    }

    @Test
    void fetchRejectsSomethingOtherThanAList() {
        serve("{\"code\":\"rest_no_route\"}");

        assertThatThrownBy(() -> source.catalog().orElseThrow().fetch()).isInstanceOf(IOException.class).hasMessageContaining("JSON array");
    }

    @Test
    void startDetectorAsksForTheOneFeature() throws IOException {
        serve(fixture());
        ComicItem comic = ComicItem.builder().id(1).name("Wannabe").source("comicskingdom").sourceIdentifier("wannabe").build();

        assertThat(source.startDetector().orElseThrow().detect(comic)).contains(StartInfo.ofDate(LocalDate.of(2024, 1, 14)));
        assertThat(queries).singleElement().satisfies(query -> assertThat(query).contains("slug=wannabe"));
    }

    @Test
    void identifierFallsBackToTheNameLikeTheDownloader() {
        assertThat(source.identifierFor(ComicItem.builder().name("Beetle Bailey").build())).isEqualTo("beetle-bailey");
    }
}
