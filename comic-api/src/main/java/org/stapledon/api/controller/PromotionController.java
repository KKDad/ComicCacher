package org.stapledon.api.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.stapledon.common.dto.PromotionManifest;
import org.stapledon.engine.promotion.DevPromotionService;
import org.stapledon.engine.promotion.PromotionSourceService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

/**
 * Serves this instance's strips to another instance's PromoteFromDevJob (dev to prod): a manifest of the strips on disk for a date range, then
 * each strip. Machine-to-machine, so REST with a shared token ({@code PromotionTokenFilter}) rather than GraphQL. Unlike
 * {@link ComicController}, it serves disabled comics too and doesn't count toward access metrics.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/promotion")
public class PromotionController {

    private final PromotionSourceService promotionSourceService;
    private final Gson gson;

    public PromotionController(PromotionSourceService promotionSourceService, @Qualifier("gsonWithLocalDate") Gson gson) {
        this.promotionSourceService = promotionSourceService;
        this.gson = gson;
    }

    /**
     * The comics with strips between {@code from} and {@code to} (inclusive, at most {@code comics.promotion.max-days} days), optionally only
     * one source or one comic. Written with Gson, which the reading instance parses it with.
     */
    @GetMapping("/manifest")
    public ResponseEntity<String> manifest(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "source", required = false) String source,
            @RequestParam(name = "sourceIdentifier", required = false) String sourceIdentifier) {
        try {
            PromotionManifest manifest = promotionSourceService.manifest(from, to, source, sourceIdentifier);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(gson.toJson(manifest));
        } catch (IllegalArgumentException e) {
            log.warn("Promotion manifest refused: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage());
        }
    }

    /**
     * One strip's image, with its transcript (URL-encoded) in {@code X-Transcript} when it has one. 404 when there's no such comic or strip.
     */
    @GetMapping("/strips/{source}/{sourceIdentifier}/{date}")
    public ResponseEntity<byte[]> strip(
            @PathVariable("source") String source,
            @PathVariable("sourceIdentifier") String sourceIdentifier,
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return promotionSourceService.strip(source, sourceIdentifier, date)
                .map(image -> {
                    ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(image.getMimeType()));
                    if (image.getTranscript() != null && !image.getTranscript().isBlank()) {
                        builder.header(DevPromotionService.TRANSCRIPT_HEADER, URLEncoder.encode(image.getTranscript(), StandardCharsets.UTF_8));
                    }
                    return builder.body(Base64.getDecoder().decode(image.getImageData()));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
