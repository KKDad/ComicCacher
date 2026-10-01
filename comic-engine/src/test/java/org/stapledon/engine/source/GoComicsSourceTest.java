package org.stapledon.engine.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.stapledon.common.config.properties.DownloaderProperties;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.infrastructure.web.UserAgentService;
import org.stapledon.engine.downloader.BrowserFetcher;
import org.stapledon.engine.downloader.GoComicsDownloaderStrategy;
import org.stapledon.engine.downloader.RateLimitedException;
import org.stapledon.engine.downloader.SourceThrottleService;

class GoComicsSourceTest {

    private HttpServer server;
    private GoComicsSource source;
    private final AtomicInteger catalogRequests = new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.start();
        UserAgentService userAgents = mock(UserAgentService.class);
        when(userAgents.getUserAgent(anyString())).thenReturn("Mozilla/5.0 Chrome/154.0.0.0 Safari/537.36");
        // Two attempts and no backoff, so a 429 is retried at once
        DownloaderProperties properties = DownloaderProperties.builder()
                .sources(Map.of("gocomics", DownloaderProperties.Source.builder()
                        .retry(DownloaderProperties.Retry.builder().maxAttempts(2).initialBackoffMs(0).maxBackoffMs(0).build())
                        .build()))
                .build();
        source = new GoComicsSource(mock(GoComicsDownloaderStrategy.class), new BrowserFetcher(userAgents), new SourceThrottleService(properties),
                "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private static String fixture(String name) throws IOException {
        try (InputStream in = Objects.requireNonNull(GoComicsSourceTest.class.getResourceAsStream("/catalog/" + name))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void serve(String path, int status, String body) {
        server.createContext(path, exchange -> {
            catalogRequests.incrementAndGet();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
    }

    @Test
    void parseCatalogReadsEveryComicAndSkipsOtherLinks() throws IOException {
        List<SourceCatalogEntry> entries = GoComicsSource.parseCatalog(Jsoup.parse(fixture("gocomics-a-to-z.html")));

        assertThat(entries).extracting(SourceCatalogEntry::identifier)
                .containsExactly("9to5", "andycapp", "calvinandhobbes", "stonesoup_espanol");
        SourceCatalogEntry calvin = entries.get(2);
        assertThat(calvin.name()).isEqualTo("Calvin and Hobbes");
        assertThat(calvin.author()).isEqualTo("Bill Watterson");
        assertThat(calvin.thumbnailUrl()).startsWith("https://gocomicscmsassets.gocomics.com/");
        assertThat(calvin.startDate()).isNull();
    }

    @Test
    void parseFirstDateReadsTheStripPagesFlightData() throws IOException {
        assertThat(GoComicsSource.parseFirstDate(fixture("gocomics-strip-page.html"))).contains(LocalDate.of(1985, 11, 18));
        assertThat(GoComicsSource.parseFirstDate("<html>no dates here</html>")).isEmpty();
    }

    @Test
    void fetchReadsTheCatalogPage() throws IOException {
        serve("/comics/a-to-z", 200, fixture("gocomics-a-to-z.html"));

        List<SourceCatalogEntry> entries = source.catalog().orElseThrow().fetch();

        assertThat(entries).hasSize(4);
    }

    @Test
    void fetchRetriesAfterRateLimit() throws IOException {
        String page = fixture("gocomics-a-to-z.html");
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/comics/a-to-z", exchange -> {
            byte[] bytes = page.getBytes(StandardCharsets.UTF_8);
            if (calls.incrementAndGet() == 1) {
                exchange.sendResponseHeaders(429, -1);
            } else {
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });

        assertThat(source.catalog().orElseThrow().fetch()).hasSize(4);
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void fetchGivesUpAfterRepeatedRateLimits() {
        serve("/comics/a-to-z", 429, "");

        assertThatThrownBy(() -> source.catalog().orElseThrow().fetch()).isInstanceOf(RateLimitedException.class);
        assertThat(catalogRequests.get()).isEqualTo(2);
    }

    @Test
    void fetchTreatsAnEmptyPageAsAFailure() {
        serve("/comics/a-to-z", 200, "<html><body>Something new</body></html>");

        assertThatThrownBy(() -> source.catalog().orElseThrow().fetch()).isInstanceOf(IOException.class).hasMessageContaining("No comics found");
    }

    @Test
    void startDetectorReadsTheComicsPage() throws IOException {
        serve("/calvinandhobbes", 200, fixture("gocomics-strip-page.html"));
        ComicItem comic = ComicItem.builder().id(1).name("Calvin and Hobbes").source("gocomics").sourceIdentifier("calvinandhobbes").build();

        assertThat(source.startDetector().orElseThrow().detect(comic)).contains(StartInfo.ofDate(LocalDate.of(1985, 11, 18)));
    }

    @Test
    void identifierFallsBackToTheNameLikeTheDownloader() {
        assertThat(source.identifierFor(ComicItem.builder().name("Calvin and Hobbes").build())).isEqualTo("calvinandhobbes");
        assertThat(source.identifierFor(ComicItem.builder().name("Anything").sourceIdentifier("Peanuts").build())).isEqualTo("peanuts");
    }
}
