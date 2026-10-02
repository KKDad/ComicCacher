package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.stapledon.AbstractIntegrationTest;
import org.stapledon.batch.TestImageGenerator;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.dto.PromotionManifest;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.engine.promotion.DevPromotionService;
import org.stapledon.engine.promotion.DevPromotionService.PromotionResult;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;

/**
 * Promotion end to end. The serving side: the endpoints answer only a request carrying the shared token, and list and hand out the strips on
 * disk. The receiving side: PromoteFromDevJob's service, pointed at a stand-in for dev, writes a strip with its sidecar, date index and image
 * hashes, and a second run changes nothing.
 */
@TestPropertySource(properties = {
    "comics.promotion.serve=true",
    "comics.promotion.token=" + PromotionIT.TOKEN
})
class PromotionIT extends AbstractIntegrationTest {

    static final String TOKEN = "integration-promotion-token";

    private static final String BASE = "/api/v1/promotion";
    private static final LocalDate SERVED_DATE = LocalDate.of(2024, 5, 19);

    private static final HttpServer DEV;
    private static volatile byte[] devImage = new byte[0];
    private static volatile String devManifest = "{}";

    static {
        try {
            DEV = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        DEV.createContext("/api/v1/promotion/manifest", exchange -> {
            byte[] body = devManifest.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        DEV.createContext("/api/v1/promotion/strips/", exchange -> {
            exchange.getResponseHeaders().add(DevPromotionService.TRANSCRIPT_HEADER, "Promoted%20from%20dev");
            exchange.sendResponseHeaders(200, devImage.length);
            exchange.getResponseBody().write(devImage);
            exchange.close();
        });
        DEV.start();
    }

    @DynamicPropertySource
    static void devUrl(DynamicPropertyRegistry registry) {
        registry.add("comics.promotion.source-url", () -> "http://127.0.0.1:" + DEV.getAddress().getPort());
    }

    @Autowired
    private DevPromotionService promotionService;

    @Autowired
    private ManagementFacade managementFacade;

    @Autowired
    private Clock clock;

    @Autowired
    @Qualifier("gsonWithLocalDate")
    private Gson gson;

    @BeforeAll
    static void createServedStrip() throws IOException {
        Path yearDir = Paths.get("./integration-cache", "TestComic", "2024");
        Files.createDirectories(yearDir);
        TestImageGenerator.createTestImage(yearDir.resolve(SERVED_DATE + ".png").toFile(), 200, 100, ImageFormat.PNG);
    }

    @AfterAll
    static void stopDev() {
        DEV.stop(0);
    }

    @Test
    void endpointsNeedTheSharedToken() throws Exception {
        String manifest = BASE + "/manifest?from=" + SERVED_DATE + "&to=" + SERVED_DATE;
        mockMvc.perform(get(manifest)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(manifest).header(DevPromotionService.TOKEN_HEADER, "wrong")).andExpect(status().isUnauthorized());

        String adminJwt = givens.givenAdministrator().authenticate();
        mockMvc.perform(get(manifest).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)).andExpect(status().isForbidden());

        mockMvc.perform(get(manifest).header(DevPromotionService.TOKEN_HEADER, TOKEN)).andExpect(status().isOk());
    }

    @Test
    void servesTheManifestAndStrips() throws Exception {
        String body = mockMvc.perform(get(BASE + "/manifest?from=" + SERVED_DATE.minusDays(6) + "&to=" + SERVED_DATE)
                        .header(DevPromotionService.TOKEN_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        PromotionManifest manifest = gson.fromJson(body, PromotionManifest.class);
        assertThat(manifest.comics()).contains(new PromotionManifest.Comic("gocomics", "testcomic", "Test Comic", List.of(SERVED_DATE)));

        byte[] strip = mockMvc.perform(get(BASE + "/strips/gocomics/testcomic/" + SERVED_DATE).header(DevPromotionService.TOKEN_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(strip).isEqualTo(Files.readAllBytes(Paths.get("./integration-cache", "TestComic", "2024", SERVED_DATE + ".png")));

        mockMvc.perform(get(BASE + "/strips/gocomics/nobody/" + SERVED_DATE).header(DevPromotionService.TOKEN_HEADER, TOKEN))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(BASE + "/manifest?from=" + SERVED_DATE.minusDays(30) + "&to=" + SERVED_DATE).header(DevPromotionService.TOKEN_HEADER, TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void promotesAMissingStripWithItsMetadata_once() throws Exception {
        LocalDate today = LocalDate.now(clock);
        Path image = Files.createTempFile("promoted", ".png");
        TestImageGenerator.createTestImage(image.toFile(), 300, 120, ImageFormat.PNG);
        devImage = Files.readAllBytes(image);
        Files.delete(image);
        devManifest = gson.toJson(new PromotionManifest(today, today,
                List.of(new PromotionManifest.Comic("comicskingdom", "anothertestcomic", "Another Test Comic", List.of(today)))));

        PromotionResult first = promotionService.promote(1, "comicskingdom", null);

        assertThat(first.promoted()).isEqualTo(1);
        Path comicDir = Paths.get("./integration-cache", "AnotherTestComic");
        Path yearDir = comicDir.resolve(String.valueOf(today.getYear()));
        assertThat(yearDir.resolve(today + ".png")).hasBinaryContent(devImage);
        assertThat(yearDir.resolve(today + ".json")).content().contains("Promoted from dev");
        assertThat(yearDir.resolve("image-hashes.json")).exists();
        assertThat(comicDir.resolve("available-dates.json")).content().contains(today.toString());
        assertThat(managementFacade.getComic(2)).get().extracting(comic -> comic.getNewest()).isEqualTo(today);

        PromotionResult second = promotionService.promote(1, "comicskingdom", null);

        assertThat(second.promoted()).isZero();
        assertThat(second.alreadyHere()).isEqualTo(1);
    }
}
