package org.stapledon.engine.promotion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.dto.ComicSaveData;
import org.stapledon.common.dto.PromotionManifest;
import org.stapledon.common.dto.SaveResult;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.common.service.RetrievalStatusService;
import org.stapledon.common.util.GsonUtils;
import org.stapledon.engine.downloader.DownloaderFacade;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.engine.promotion.DevPromotionService.PromotionException;
import org.stapledon.engine.promotion.DevPromotionService.PromotionResult;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class DevPromotionServiceTest {

    private static final String TOKEN = "s3cret";
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);
    private static final byte[] IMAGE = {1, 2, 3};

    private final Gson gson = GsonUtils.createGsonBuilder().create();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T14:00:00Z"), ZoneId.of("America/Toronto"));

    private ManagementFacade managementFacade;
    private ComicStorageFacade storageFacade;
    private DownloaderFacade downloaderFacade;
    private RetrievalStatusService retrievalStatusService;
    private HttpServer server;
    private String baseUrl;
    private PromotionManifest manifest;
    private int manifestStatus;
    private final Map<String, String> lastQuery = new ConcurrentHashMap<>();
    private final Map<String, Integer> stripStatus = new ConcurrentHashMap<>();
    private final Map<String, String> transcripts = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() throws IOException {
        managementFacade = mock(ManagementFacade.class);
        storageFacade = mock(ComicStorageFacade.class);
        downloaderFacade = mock(DownloaderFacade.class);
        retrievalStatusService = mock(RetrievalStatusService.class);
        manifest = new PromotionManifest(YESTERDAY, TODAY, List.of());
        manifestStatus = 200;

        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/api/v1/promotion/manifest", exchange -> {
            if (!TOKEN.equals(exchange.getRequestHeaders().getFirst(DevPromotionService.TOKEN_HEADER))) {
                respond(exchange, 401, new byte[0]);
                return;
            }
            lastQuery.put("query", String.valueOf(exchange.getRequestURI().getRawQuery()));
            respond(exchange, manifestStatus, gson.toJson(manifest).getBytes(StandardCharsets.UTF_8));
        });
        server.createContext("/api/v1/promotion/strips/", exchange -> {
            String path = exchange.getRequestURI().getPath().substring("/api/v1/promotion/strips/".length());
            int status = stripStatus.getOrDefault(path, 200);
            String transcript = transcripts.get(path);
            if (transcript != null) {
                exchange.getResponseHeaders().add(DevPromotionService.TRANSCRIPT_HEADER, URLEncoder.encode(transcript, StandardCharsets.UTF_8));
            }
            respond(exchange, status, status == 200 ? IMAGE : new byte[0]);
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            exchange.getResponseBody().write(body);
        }
        exchange.close();
    }

    private DevPromotionService service(String url, String token) {
        return new DevPromotionService(managementFacade, storageFacade, downloaderFacade, retrievalStatusService, gson, clock, HttpClient.newHttpClient(), url, token, 7);
    }

    private static ComicItem comic(int id, String name, String source, String identifier) {
        return ComicItem.builder().id(id).name(name).source(source).sourceIdentifier(identifier).build();
    }

    @Test
    void notConfigured_doesNothing() {
        PromotionResult result = service("", TOKEN).promote(1, null, null);

        assertThat(result.configured()).isFalse();
        verifyNoInteractions(storageFacade, managementFacade);
    }

    @Test
    void daysOutsideTheLimit_isRefused() {
        DevPromotionService service = service(baseUrl, TOKEN);

        assertThatThrownBy(() -> service.promote(8, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.promote(0, null, null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(storageFacade);
    }

    @Test
    void promotesMissingStrips_matchingOnSourceAndIdentifier() {
        // Dev numbers the comic 3; here it's 42
        ComicItem garfield = comic(42, "Garfield", "gocomics", "garfield");
        when(managementFacade.getAllComics()).thenReturn(List.of(garfield));
        when(managementFacade.getComic(42)).thenReturn(Optional.of(garfield));
        manifest = new PromotionManifest(YESTERDAY, TODAY,
                List.of(new PromotionManifest.Comic("gocomics", "garfield", "Garfield", List.of(YESTERDAY, TODAY))));
        when(storageFacade.comicStripExists(any(), eq(YESTERDAY))).thenReturn(true);
        when(storageFacade.comicStripExists(any(), eq(TODAY))).thenReturn(false);
        when(storageFacade.saveComicStripWithResult(any(), eq(TODAY), any(ComicSaveData.class))).thenReturn(SaveResult.saved());
        transcripts.put("gocomics/garfield/" + TODAY, "Jon: Hi, Garfield & Odie");

        PromotionResult result = service(baseUrl, TOKEN).promote(2, null, null);

        assertThat(result).isEqualTo(new PromotionResult(true, 1, 1, 1, 0, 0, 0));
        ArgumentCaptor<ComicIdentifier> id = ArgumentCaptor.forClass(ComicIdentifier.class);
        ArgumentCaptor<ComicSaveData> data = ArgumentCaptor.forClass(ComicSaveData.class);
        verify(storageFacade).saveComicStripWithResult(id.capture(), eq(TODAY), data.capture());
        verify(storageFacade, never()).saveComicStripWithResult(any(), eq(YESTERDAY), any(ComicSaveData.class));
        assertThat(id.getValue().getId()).isEqualTo(42);
        assertThat(data.getValue().imageData()).isEqualTo(IMAGE);
        assertThat(data.getValue().transcript()).isEqualTo("Jon: Hi, Garfield & Odie");
        assertThat(lastQuery.get("query")).isEqualTo("from=" + YESTERDAY + "&to=" + TODAY + "&source=gocomics&sourceIdentifier=garfield");

        ArgumentCaptor<ComicItem> updated = ArgumentCaptor.forClass(ComicItem.class);
        verify(managementFacade).updateComic(eq(42), updated.capture());
        assertThat(updated.getValue().getNewest()).isEqualTo(TODAY);
        assertThat(updated.getValue().getOldest()).isEqualTo(TODAY);

        ArgumentCaptor<ComicRetrievalRecord> record = ArgumentCaptor.forClass(ComicRetrievalRecord.class);
        verify(retrievalStatusService).recordRetrievalResult(record.capture());
        assertThat(record.getValue().getId()).isEqualTo("42_" + TODAY);
        assertThat(record.getValue().getComicDate()).isEqualTo(TODAY);
        assertThat(record.getValue().getSource()).isEqualTo("gocomics");
        assertThat(record.getValue().getStatus()).isEqualTo(ComicRetrievalStatus.SUCCESS);
        assertThat(record.getValue().getImageSize()).isEqualTo((long) IMAGE.length);
    }

    @Test
    void skipsDisabledComicsIndexedSourcesAndComicsNotHere() {
        ComicItem disabled = comic(1, "Disabled", "gocomics", "disabled").toBuilder().enabled(false).build();
        ComicItem freefall = comic(2, "Freefall", "freefall", "freefall");
        ComicItem kept = comic(3, "Kept", "gocomics", "kept");
        when(managementFacade.getAllComics()).thenReturn(List.of(disabled, freefall, kept));
        when(downloaderFacade.isIndexedSource("freefall")).thenReturn(true);
        manifest = new PromotionManifest(TODAY, TODAY, List.of(
                new PromotionManifest.Comic("gocomics", "disabled", "Disabled", List.of(TODAY)),
                new PromotionManifest.Comic("freefall", "freefall", "Freefall", List.of(TODAY)),
                new PromotionManifest.Comic("gocomics", "devonly", "Dev Only", List.of(TODAY))));

        PromotionResult result = service(baseUrl, TOKEN).promote(1, "ALL", null);

        assertThat(result).isEqualTo(new PromotionResult(true, 0, 0, 0, 0, 0, 3));
        verify(storageFacade, never()).saveComicStripWithResult(any(), any(), any(ComicSaveData.class));
        assertThat(lastQuery.get("query")).isEqualTo("from=" + TODAY + "&to=" + TODAY + "&source=gocomics&sourceIdentifier=kept");
    }

    @Test
    void sourceFilter_asksDevForThatSourceOnly() {
        when(managementFacade.getAllComics()).thenReturn(List.of(
                comic(1, "A", "gocomics", "a"), comic(2, "B", "gocomics", "b"), comic(3, "C", "comicskingdom", "c")));

        service(baseUrl, TOKEN).promote(1, "gocomics", null);

        assertThat(lastQuery.get("query")).isEqualTo("from=" + TODAY + "&to=" + TODAY + "&source=gocomics");
    }

    @Test
    void noMatchingComicsHere_skipsDev() {
        when(managementFacade.getAllComics()).thenReturn(List.of(comic(1, "A", "gocomics", "a")));

        PromotionResult result = service(baseUrl, TOKEN).promote(1, null, 99);

        assertThat(result.comics()).isZero();
        assertThat(lastQuery).isEmpty();
    }

    @Test
    void failedStripAndDuplicate_areCountedAndTheRunGoesOn() {
        ComicItem a = comic(1, "A", "gocomics", "a");
        when(managementFacade.getAllComics()).thenReturn(List.of(a));
        manifest = new PromotionManifest(YESTERDAY, TODAY,
                List.of(new PromotionManifest.Comic("gocomics", "a", "A", List.of(YESTERDAY, TODAY))));
        stripStatus.put("gocomics/a/" + YESTERDAY, 404);
        when(storageFacade.saveComicStripWithResult(any(), eq(TODAY), any(ComicSaveData.class))).thenReturn(SaveResult.duplicateSkipped(YESTERDAY.minusDays(3)));

        PromotionResult result = service(baseUrl, TOKEN).promote(2, null, null);

        assertThat(result).isEqualTo(new PromotionResult(true, 1, 0, 0, 1, 1, 0));
        verify(managementFacade, never()).updateComic(anyInt(), any());
        verifyNoInteractions(retrievalStatusService);
    }

    @Test
    void rejectedToken_failsTheRun() {
        when(managementFacade.getAllComics()).thenReturn(List.of(comic(1, "A", "gocomics", "a")));

        assertThatThrownBy(() -> service(baseUrl, "wrong").promote(1, null, null))
                .isInstanceOf(PromotionException.class)
                .hasMessageContaining("HTTP 401");
    }

    @Test
    void devErrorOrUnreachable_failsTheRun() {
        when(managementFacade.getAllComics()).thenReturn(List.of(comic(1, "A", "gocomics", "a")));
        manifestStatus = 500;

        assertThatThrownBy(() -> service(baseUrl, TOKEN).promote(1, null, null))
                .isInstanceOf(PromotionException.class)
                .hasMessageContaining("HTTP 500");

        server.stop(0);
        assertThatThrownBy(() -> service(baseUrl, TOKEN).promote(1, null, null))
                .isInstanceOf(PromotionException.class)
                .hasMessageContaining("Can't read the promotion manifest");
    }
}
