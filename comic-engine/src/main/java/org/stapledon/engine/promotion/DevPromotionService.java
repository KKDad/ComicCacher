package org.stapledon.engine.promotion;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicSaveData;
import org.stapledon.common.dto.PromotionManifest;
import org.stapledon.common.dto.SaveResult;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.common.util.LogContext;
import org.stapledon.engine.downloader.DownloaderFacade;
import org.stapledon.engine.management.ManagementFacade;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import lombok.extern.slf4j.Slf4j;

/**
 * The receiving side of promotion: copies the strips that another instance (dev, at {@code comics.promotion.source-url}) has and this one is
 * missing, so this instance doesn't download them a second time. Reads that instance's manifest for the last few days, then fetches each strip
 * this instance has no file for and saves it through {@link ComicStorageFacade#saveComicStripWithResult}, which validates it, skips duplicates
 * and updates the date index, image hashes and metadata sidecar like any download. Never overwrites a strip.
 * <p>
 * Comics are matched on source and source identifier. Only comics that exist and are enabled here are promoted; indexed sources (Freefall)
 * are left to their own download, since their strip numbers aren't part of the manifest.
 */
@Slf4j
@Service
public class DevPromotionService {

    public static final String TOKEN_HEADER = "X-Promotion-Token";
    public static final String TRANSCRIPT_HEADER = "X-Transcript";

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final ManagementFacade managementFacade;
    private final ComicStorageFacade storageFacade;
    private final DownloaderFacade downloaderFacade;
    private final Gson gson;
    private final Clock clock;
    private final HttpClient http;
    private final String sourceUrl;
    private final String token;
    private final int maxDays;

    @Autowired
    public DevPromotionService(ManagementFacade managementFacade, ComicStorageFacade storageFacade, DownloaderFacade downloaderFacade,
            @Qualifier("gsonWithLocalDate") Gson gson, Clock clock,
            @Value("${comics.promotion.source-url:}") String sourceUrl,
            @Value("${comics.promotion.token:}") String token,
            @Value("${comics.promotion.max-days:7}") int maxDays) {
        this(managementFacade, storageFacade, downloaderFacade, gson, clock,
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(TIMEOUT).build(), sourceUrl, token, maxDays);
    }

    DevPromotionService(ManagementFacade managementFacade, ComicStorageFacade storageFacade, DownloaderFacade downloaderFacade, Gson gson,
            Clock clock, HttpClient http, String sourceUrl, String token, int maxDays) {
        this.managementFacade = managementFacade;
        this.storageFacade = storageFacade;
        this.downloaderFacade = downloaderFacade;
        this.gson = gson;
        this.clock = clock;
        this.http = http;
        this.sourceUrl = sourceUrl == null ? "" : sourceUrl.strip().replaceAll("/+$", "");
        this.token = token == null ? "" : token.strip();
        this.maxDays = maxDays;
    }

    /**
     * Counts from one promotion run. {@code notHere} is the number of the other instance's comics with no enabled match here.
     */
    public record PromotionResult(boolean configured, int comics, int promoted, int alreadyHere, int duplicates, int failed, int notHere) {

        static PromotionResult notConfigured() {
            return new PromotionResult(false, 0, 0, 0, 0, 0, 0);
        }
    }

    /**
     * Thrown when the other instance can't be reached or refuses the request, so nothing can be promoted.
     */
    public static class PromotionException extends RuntimeException {

        public PromotionException(String message) {
            super(message);
        }
    }

    /**
     * True when both the other instance's URL and the shared token are set.
     */
    public boolean isConfigured() {
        return !sourceUrl.isEmpty() && !token.isEmpty();
    }

    /**
     * Promotes the strips of the last {@code days} days (1 = today only), optionally only one source ({@code null}, blank or "ALL" for all)
     * or one comic (this instance's id). Returns without doing anything when promotion isn't configured. Throws IllegalArgumentException when
     * {@code days} is outside 1..{@code comics.promotion.max-days}, and {@link PromotionException} when the other instance can't be read.
     */
    public PromotionResult promote(int days, String sourceFilter, Integer comicId) {
        if (days < 1 || days > maxDays) {
            throw new IllegalArgumentException("days must be between 1 and " + maxDays + ", not " + days);
        }
        if (!isConfigured()) {
            log.info("Promotion isn't configured (comics.promotion.source-url and comics.promotion.token); nothing to do");
            return PromotionResult.notConfigured();
        }
        long start = System.currentTimeMillis();
        LocalDate to = LocalDate.now(clock);
        LocalDate from = to.minusDays(days - 1L);
        boolean allSources = sourceFilter == null || sourceFilter.isBlank() || "ALL".equalsIgnoreCase(sourceFilter);

        Map<String, ComicItem> candidates = new HashMap<>();
        for (ComicItem comic : managementFacade.getAllComics()) {
            if (comic.isEnabled() && comic.getSource() != null && comic.getSourceIdentifier() != null
                    && (allSources || sourceFilter.equals(comic.getSource()))
                    && (comicId == null || comicId == comic.getId())
                    && !downloaderFacade.isIndexedSource(comic.getSource())) {
                candidates.put(key(comic.getSource(), comic.getSourceIdentifier()), comic);
            }
        }
        if (candidates.isEmpty()) {
            log.info("No enabled comics here match (source={}, comic={}); nothing to promote", allSources ? "ALL" : sourceFilter, comicId);
            return new PromotionResult(true, 0, 0, 0, 0, 0, 0);
        }

        // One comic: ask only for it. One source: only that source
        ComicItem only = candidates.size() == 1 ? candidates.values().iterator().next() : null;
        String manifestSource = only != null ? only.getSource() : allSources ? null : sourceFilter;
        String manifestIdentifier = only != null ? only.getSourceIdentifier() : null;
        PromotionManifest manifest = fetchManifest(from, to, manifestSource, manifestIdentifier);

        int comics = 0;
        int promoted = 0;
        int alreadyHere = 0;
        int duplicates = 0;
        int failed = 0;
        int notHere = 0;
        for (PromotionManifest.Comic theirs : manifest.comics() == null ? List.<PromotionManifest.Comic>of() : manifest.comics()) {
            ComicItem comic = candidates.get(key(theirs.source(), theirs.sourceIdentifier()));
            if (comic == null) {
                log.debug("{} ({}/{}) has no enabled match here; skipped", theirs.name(), theirs.source(), theirs.sourceIdentifier());
                notHere++;
                continue;
            }
            comics++;
            ComicIdentifier id = ComicIdentifier.from(comic);
            LocalDate oldestSaved = null;
            LocalDate newestSaved = null;
            try (var _ = MDC.putCloseable(LogContext.COMIC, comic.getName())) {
                for (LocalDate date : theirs.dates() == null ? List.<LocalDate>of() : theirs.dates()) {
                    if (date.isBefore(from) || date.isAfter(to)) {
                        continue;
                    }
                    try (var _ = MDC.putCloseable(LogContext.DATE, date.toString())) {
                        if (storageFacade.comicStripExists(id, date)) {
                            log.debug("{} {} is already here", comic.getName(), date);
                            alreadyHere++;
                            continue;
                        }
                        Optional<FetchedStrip> strip = fetchStrip(comic, date);
                        if (strip.isEmpty()) {
                            failed++;
                            continue;
                        }
                        SaveResult result = storageFacade.saveComicStripWithResult(id, date,
                                ComicSaveData.builder().imageData(strip.get().imageData()).transcript(strip.get().transcript()).build());
                        if (result.wasSaved()) {
                            log.debug("Promoted {} {}", comic.getName(), date);
                            promoted++;
                            oldestSaved = oldestSaved == null || date.isBefore(oldestSaved) ? date : oldestSaved;
                            newestSaved = newestSaved == null || date.isAfter(newestSaved) ? date : newestSaved;
                        } else if (result.getOutcome() == SaveResult.Outcome.DUPLICATE_SKIPPED) {
                            duplicates++;
                        } else {
                            // The storage facade logged the cause
                            log.debug("Couldn't save promoted strip {} {}: {}", comic.getName(), date, result.getMessage());
                            failed++;
                        }
                    }
                }
            }
            if (newestSaved != null) {
                updateDateRange(comic.getId(), oldestSaved, newestSaved);
            }
        }

        log.info("Promoted {} strips from {} for {} to {} (comics={} already-here={} duplicate={} failed={} not-here={}) in {}ms",
                promoted, sourceUrl, from, to, comics, alreadyHere, duplicates, failed, notHere, System.currentTimeMillis() - start);
        return new PromotionResult(true, comics, promoted, alreadyHere, duplicates, failed, notHere);
    }

    /**
     * Widens the comic's oldest and newest dates to cover the promoted strips, reading the comic again so a concurrent change isn't lost.
     */
    private void updateDateRange(int comicId, LocalDate oldestSaved, LocalDate newestSaved) {
        managementFacade.getComic(comicId).ifPresent(current -> {
            LocalDate oldest = current.getOldest() == null || oldestSaved.isBefore(current.getOldest()) ? oldestSaved : current.getOldest();
            LocalDate newest = current.getNewest() == null || newestSaved.isAfter(current.getNewest()) ? newestSaved : current.getNewest();
            if (!oldest.equals(current.getOldest()) || !newest.equals(current.getNewest())) {
                managementFacade.updateComic(comicId, current.toBuilder().oldest(oldest).newest(newest).build());
            }
        });
    }

    private PromotionManifest fetchManifest(LocalDate from, LocalDate to, String source, String sourceIdentifier) {
        StringBuilder url = new StringBuilder(sourceUrl).append("/api/v1/promotion/manifest?from=").append(from).append("&to=").append(to);
        if (source != null) {
            url.append("&source=").append(URLEncoder.encode(source, StandardCharsets.UTF_8));
        }
        if (sourceIdentifier != null) {
            url.append("&sourceIdentifier=").append(URLEncoder.encode(sourceIdentifier, StandardCharsets.UTF_8));
        }
        HttpResponse<String> response;
        try {
            response = http.send(request(url.toString()), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new PromotionException("Can't read the promotion manifest from " + sourceUrl + ": " + e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PromotionException("Interrupted while reading the promotion manifest from " + sourceUrl);
        }
        int status = response.statusCode();
        if (status == 401 || status == 403) {
            throw new PromotionException(sourceUrl + " rejected the promotion token (HTTP " + status + "); check comics.promotion.token on both instances");
        }
        if (status != 200) {
            throw new PromotionException(sourceUrl + " returned HTTP " + status + " for the promotion manifest");
        }
        try {
            PromotionManifest manifest = gson.fromJson(response.body(), PromotionManifest.class);
            if (manifest == null) {
                throw new PromotionException(sourceUrl + " returned an empty promotion manifest");
            }
            return manifest;
        } catch (JsonParseException e) {
            throw new PromotionException(sourceUrl + " returned a promotion manifest that can't be read: " + e.getMessage());
        }
    }

    /**
     * Fetches one strip, or logs why it couldn't (one WARN) and returns empty. Throws {@link PromotionException} when interrupted.
     */
    private Optional<FetchedStrip> fetchStrip(ComicItem comic, LocalDate date) {
        String url = sourceUrl + "/api/v1/promotion/strips/" + pathSegment(comic.getSource()) + "/" + pathSegment(comic.getSourceIdentifier()) + "/" + date;
        try {
            HttpResponse<byte[]> response = http.send(request(url), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                log.warn("Couldn't promote {} {}: {} returned HTTP {}", comic.getName(), date, sourceUrl, response.statusCode());
                return Optional.empty();
            }
            String transcript = response.headers().firstValue(TRANSCRIPT_HEADER)
                    .map(value -> URLDecoder.decode(value, StandardCharsets.UTF_8))
                    .orElse(null);
            return Optional.of(new FetchedStrip(response.body(), transcript));
        } catch (IOException e) {
            log.warn("Couldn't promote {} {}: {}", comic.getName(), date, e.toString());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PromotionException("Interrupted while promoting " + comic.getName() + " " + date);
        }
    }

    private HttpRequest request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header(TOKEN_HEADER, token)
                .GET()
                .build();
    }

    private static String pathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String key(String source, String sourceIdentifier) {
        return source + "/" + sourceIdentifier;
    }

    private record FetchedStrip(byte[] imageData, String transcript) {
    }
}
