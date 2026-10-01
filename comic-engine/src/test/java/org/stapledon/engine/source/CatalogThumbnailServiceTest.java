package org.stapledon.engine.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;

import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.config.properties.DownloaderProperties;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.infrastructure.web.UserAgentService;
import org.stapledon.common.service.ValidationService;
import org.stapledon.common.util.GsonUtils;
import org.stapledon.engine.downloader.DownloaderFacade;
import org.stapledon.engine.downloader.SourceThrottleService;
import org.stapledon.engine.source.CatalogThumbnailService.RequestOutcome;

class CatalogThumbnailServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-28T11:00:00Z");
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};

    @TempDir
    Path cacheRoot;

    private HttpServer server;
    private String base;
    private final AtomicInteger hits = new AtomicInteger();
    private final List<Runnable> queuedWork = new ArrayList<>();
    private final StubSource source = new StubSource("daily");
    private SourceCatalogRepository catalog;
    private SourceThrottleService throttle;
    private ValidationService validation;
    private Path root;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        catalog = new SourceCatalogRepository(CacheProperties.builder().location(cacheRoot.toString()).build(), GsonUtils.createGson(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        throttle = mock(SourceThrottleService.class);
        when(throttle.backOff(anyString(), anyInt(), any())).thenReturn(Duration.ofSeconds(60));
        validation = mock(ValidationService.class);
        when(validation.validate(any())).thenReturn(ImageValidationResult.success(ImageFormat.PNG, 10, 10, PNG.length));
        root = cacheRoot.resolve("tmp").resolve("catalog-thumbnails");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private CatalogThumbnailService service(Duration failureMemo, boolean runNow) {
        UserAgentService userAgents = mock(UserAgentService.class);
        when(userAgents.getUserAgent(anyString())).thenReturn("test-agent");
        SourceRegistry registry = new SourceRegistry(List.of(source), mock(DownloaderFacade.class));
        return new CatalogThumbnailService(registry, catalog, throttle, DownloaderProperties.builder().build(), userAgents, validation,
                runNow ? Runnable::run : queuedWork::add, root, failureMemo, HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(),
                false, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void listInCatalog(String identifier, String thumbnailUrl) {
        catalog.merge("daily", List.of(new SourceCatalogEntry(identifier, identifier, null, thumbnailUrl, null)));
    }

    private void serveImage(String path, int status, byte[] body) {
        server.createContext(path, exchange -> {
            hits.incrementAndGet();
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            if (body.length > 0) {
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
    }

    @Test
    void requestDownloadsAndCachesTheThumbnail() {
        serveImage("/one.png", 200, PNG);
        listInCatalog("one", base + "/one.png");
        CatalogThumbnailService service = service(Duration.ZERO, true);

        assertThat(service.request("daily", "one")).isEqualTo(RequestOutcome.QUEUED);

        assertThat(service.cached("daily", "one")).contains(root.resolve("daily").resolve("one.png"));
        assertThat(service.load("daily", "one")).hasValueSatisfying(t -> {
            assertThat(t.data()).isEqualTo(PNG);
            assertThat(t.mediaType()).isEqualTo("image/png");
        });
        assertThat(service.request("daily", "one")).isEqualTo(RequestOutcome.CACHED);
        assertThat(hits.get()).isEqualTo(1);
    }

    @Test
    void requestWhileQueuedIsNotQueuedTwice() {
        listInCatalog("one", base + "/one.png");
        CatalogThumbnailService service = service(Duration.ZERO, false);

        assertThat(service.request("daily", "one")).isEqualTo(RequestOutcome.QUEUED);
        assertThat(service.isPending("daily", "one")).isTrue();
        assertThat(service.request("daily", "one")).isEqualTo(RequestOutcome.PENDING);
        assertThat(queuedWork).hasSize(1);
    }

    @Test
    void unknownSourcesAndMalformedIdentifiersAreRefused() {
        CatalogThumbnailService service = service(Duration.ZERO, true);

        assertThat(service.request("nowhere", "one")).isEqualTo(RequestOutcome.UNKNOWN);
        assertThat(service.request("daily", "../../etc/passwd")).isEqualTo(RequestOutcome.UNKNOWN);
        assertThat(service.cached("daily", "../x")).isEmpty();
    }

    @Test
    void hostsOffTheAllowListAreNeverFetched() {
        listInCatalog("one", "http://localhost:" + server.getAddress().getPort() + "/one.png");
        serveImage("/one.png", 200, PNG);

        service(Duration.ZERO, true).request("daily", "one");

        assertThat(hits.get()).isZero();
        assertThat(Files.exists(root.resolve("daily").resolve("one.png"))).isFalse();
    }

    @Test
    void redirectsMustStayOnAllowedHosts() {
        server.createContext("/moved.png", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://localhost:" + server.getAddress().getPort() + "/one.png");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        serveImage("/one.png", 200, PNG);
        listInCatalog("one", base + "/moved.png");

        service(Duration.ZERO, true).request("daily", "one");

        assertThat(hits.get()).isZero();
        assertThat(Files.exists(root.resolve("daily").resolve("one.png"))).isFalse();
    }

    @Test
    void oversizedImagesAreRefused() {
        serveImage("/big.png", 200, new byte[CatalogThumbnailService.MAX_BYTES + 1]);
        listInCatalog("one", base + "/big.png");

        service(Duration.ZERO, true).request("daily", "one");

        assertThat(Files.exists(root.resolve("daily").resolve("one.png"))).isFalse();
    }

    @Test
    void rateLimitBacksTheSourceOffAndRemembersTheFailure() {
        serveImage("/one.png", 429, new byte[0]);
        listInCatalog("one", base + "/one.png");
        CatalogThumbnailService service = service(Duration.ofHours(24), true);

        service.request("daily", "one");

        verify(throttle).backOff(eq("daily"), eq(1), any(Optional.class));
        assertThat(service.request("daily", "one")).isEqualTo(RequestOutcome.FAILED_RECENTLY);
        assertThat(hits.get()).isEqualTo(1);
    }

    @Test
    void invalidImagesAreNotSaved() {
        serveImage("/one.png", 200, PNG);
        listInCatalog("one", base + "/one.png");
        when(validation.validate(any())).thenReturn(ImageValidationResult.failure("not an image"));

        service(Duration.ZERO, true).request("daily", "one");

        assertThat(Files.exists(root.resolve("daily").resolve("one.png"))).isFalse();
    }

    @Test
    void purgeDeletesOldThumbnailsAndThoseOfRemovedComics() throws IOException {
        catalog.merge("daily", List.of(new SourceCatalogEntry("kept", "Kept", null, null, null), new SourceCatalogEntry("old", "Old", null, null, null),
                new SourceCatalogEntry("gone", "Gone", null, null, null)));
        catalog.merge("daily", List.of(new SourceCatalogEntry("kept", "Kept", null, null, null), new SourceCatalogEntry("old", "Old", null, null, null)));
        Path dir = Files.createDirectories(root.resolve("daily"));
        Files.write(dir.resolve("kept.png"), PNG);
        Files.write(dir.resolve("gone.png"), PNG);
        Files.write(dir.resolve("old.png"), PNG);
        // Older than the age plus the largest jitter
        Files.setLastModifiedTime(dir.resolve("old.png"), FileTime.from(NOW.minus(Duration.ofDays(30 + CatalogThumbnailService.PURGE_JITTER_DAYS + 1))));
        Files.setLastModifiedTime(dir.resolve("kept.png"), FileTime.from(NOW));
        Files.setLastModifiedTime(dir.resolve("gone.png"), FileTime.from(NOW));

        int deleted = service(Duration.ZERO, true).purge(Duration.ofDays(30));

        assertThat(deleted).isEqualTo(2);
        assertThat(Files.exists(dir.resolve("kept.png"))).isTrue();
    }

    @Test
    void purgeJitterIsFixedPerComicAndWithinRange() {
        Duration jitter = CatalogThumbnailService.jitter("daily", "one");

        assertThat(CatalogThumbnailService.jitter("daily", "one")).isEqualTo(jitter);
        assertThat(jitter.toDays()).isBetween(0L, (long) CatalogThumbnailService.PURGE_JITTER_DAYS);
    }

    @Test
    void aPurgedThumbnailBecomesDueAgain() throws IOException {
        serveImage("/old.png", 200, PNG);
        listInCatalog("old", base + "/old.png");
        CatalogThumbnailService service = service(Duration.ZERO, true);
        service.prefetchDue(10);
        Path file = root.resolve("daily").resolve("old.png");
        Files.setLastModifiedTime(file, FileTime.from(NOW.minus(Duration.ofDays(30 + CatalogThumbnailService.PURGE_JITTER_DAYS + 1))));

        service.purge(Duration.ofDays(30));

        assertThat(catalog.find("daily").orElseThrow().getEntries().get("old").getThumbnailSavedAt()).isNull();
        assertThat(service.prefetchDue(10)).isEqualTo(1);
        assertThat(hits.get()).isEqualTo(2);
    }

    @Test
    void prefetchDownloadsDueThumbnailsUpToTheLimit() {
        serveImage("/a.png", 200, PNG);
        serveImage("/b.png", 200, PNG);
        catalog.merge("daily", List.of(new SourceCatalogEntry("a", "A", null, base + "/a.png", null), new SourceCatalogEntry("b", "B", null, base + "/b.png", null)));
        CatalogThumbnailService service = service(Duration.ZERO, true);

        assertThat(service.prefetchDue(1)).isEqualTo(1);
        assertThat(service.prefetchDue(1)).isEqualTo(1);
        assertThat(service.prefetchDue(1)).isZero();

        assertThat(hits.get()).isEqualTo(2);
        assertThat(catalog.find("daily").orElseThrow().getEntries().get("a").getThumbnailSavedAt()).isNotNull();
    }

    @Test
    void prefetchRecordsAThumbnailAlreadyOnDiskWithoutFetching() throws IOException {
        listInCatalog("one", base + "/one.png");
        Files.write(Files.createDirectories(root.resolve("daily")).resolve("one.png"), PNG);

        assertThat(service(Duration.ZERO, true).prefetchDue(10)).isZero();

        assertThat(hits.get()).isZero();
        assertThat(catalog.find("daily").orElseThrow().getEntries().get("one").getThumbnailSavedAt()).isNotNull();
    }

    @Test
    void prefetchStopsTheSourceAtTheFirstRateLimit() {
        serveImage("/a.png", 429, new byte[0]);
        serveImage("/b.png", 200, PNG);
        catalog.merge("daily", List.of(new SourceCatalogEntry("a", "A", null, base + "/a.png", null), new SourceCatalogEntry("b", "B", null, base + "/b.png", null)));

        assertThat(service(Duration.ZERO, true).prefetchDue(10)).isZero();

        verify(throttle).backOff(eq("daily"), eq(1), any(Optional.class));
        assertThat(hits.get()).isEqualTo(1);
        // A 429 isn't the image's fault: it stays due for the next run
        assertThat(catalog.find("daily").orElseThrow().getEntries().get("a").getThumbnailFailedAt()).isNull();
    }

    @Test
    void aFailedThumbnailWaitsAWeek() {
        serveImage("/one.png", 404, new byte[0]);
        listInCatalog("one", base + "/one.png");
        CatalogThumbnailService service = service(Duration.ZERO, true);

        service.prefetchDue(10);

        var entry = catalog.find("daily").orElseThrow().getEntries().get("one");
        assertThat(entry.getThumbnailFailedAt()).isNotNull();
        assertThat(SourceCatalogRepository.isThumbnailDue(entry, NOW.atOffset(ZoneOffset.UTC).plusDays(6), CatalogThumbnailService.FAILURE_RETRY)).isFalse();
        assertThat(SourceCatalogRepository.isThumbnailDue(entry, NOW.atOffset(ZoneOffset.UTC).plusDays(7), CatalogThumbnailService.FAILURE_RETRY)).isTrue();
    }

    @Test
    void wideImagesAreShrunkToPng() throws IOException {
        BufferedImage wide = new BufferedImage(1200, 600, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        ImageIO.write(wide, "jpg", jpeg);
        serveImage("/wide.jpg", 200, jpeg.toByteArray());
        listInCatalog("wide", base + "/wide.jpg");
        when(validation.validate(any())).thenReturn(ImageValidationResult.success(ImageFormat.JPEG, 1200, 600, jpeg.size()));

        service(Duration.ZERO, true).request("daily", "wide");

        Path saved = root.resolve("daily").resolve("wide.png");
        BufferedImage stored = ImageIO.read(saved.toFile());
        assertThat(stored.getWidth()).isEqualTo(CatalogThumbnailService.MAX_WIDTH);
        assertThat(stored.getHeight()).isEqualTo(200);
        assertThat(Files.exists(root.resolve("daily").resolve("wide.jpg"))).isFalse();
    }
}
