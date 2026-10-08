package org.stapledon.engine.downloader;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;

import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.infrastructure.web.UserAgentService;
import org.stapledon.common.service.ValidationService;

/**
 * Strategy implementation for downloading xkcd (xkcd.com). Strips are numbered, and the site's JSON API
 * ({@code /info.0.json} for the latest, {@code /{n}/info.0.json} for one strip) gives each strip's date, image and alt text.
 */
@Slf4j
@ToString(callSuper = true)
@Component
public class XkcdDownloaderStrategy extends AbstractIndexedDownloaderStrategy {

    static final String SOURCE_IDENTIFIER = "xkcd";
    static final String BASE_URL = "https://xkcd.com";

    private static final Gson GSON = new Gson();

    /**
     * Creates a new xkcd downloader strategy.
     */
    public XkcdDownloaderStrategy(InspectorService webInspector,
            ValidationService imageValidationService,
            UserAgentService userAgentService,
            SourceThrottleService throttleService,
            Clock clock) {
        super(SOURCE_IDENTIFIER, webInspector, imageValidationService, userAgentService, throttleService, clock);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IndexedStripData fetchLatestStrip(ComicItem comic) throws Exception {
        return fetchStripFromJson(BASE_URL + "/info.0.json");
    }

    /**
     * {@inheritDoc}
     * Strip 404 doesn't exist (the API answers HTTP 404, so it is reported as unavailable).
     */
    @Override
    protected IndexedStripData fetchStrip(ComicItem comic, int stripNumber) throws Exception {
        return fetchStripFromJson(BASE_URL + "/" + stripNumber + "/info.0.json");
    }

    /**
     * {@inheritDoc}
     * The logo's file name is a content hash, so it is read from the home page rather than hard-coded.
     */
    @Override
    protected byte[] downloadAvatarImage(int comicId, String comicName, String sourceIdentifier) throws Exception {
        Document home = getPage(Jsoup.connect(BASE_URL + "/")
                .userAgent(userAgentService.getUserAgent(SOURCE_IDENTIFIER)).timeout(DownloaderConstants.DEFAULT_TIMEOUT));
        Element logo = home.selectFirst("img[alt*=logo]");
        if (logo == null) {
            throw new IllegalStateException("No logo on the xkcd home page");
        }
        String logoUrl = logo.absUrl("src");
        log.debug("Downloading xkcd avatar from {}", logoUrl);
        return downloadImageData(logoUrl);
    }

    private IndexedStripData fetchStripFromJson(String url) throws IOException, StripUnavailableException {
        XkcdStrip strip = parseStrip(fetchJson(url));
        if (!strip.hasImage()) {
            // Interactive strips (#1608 Hoverboard, #1663 Garden) have no image to keep
            throw new StripUnavailableException(String.format("xkcd #%d (%s) has no image, likely an interactive strip", strip.num(), strip.safeTitle()));
        }
        log.debug("Downloading xkcd #{} image from {}", strip.num(), strip.img());
        return new IndexedStripData(downloadImageData(strip.img()), strip.date(), strip.num(), strip.alt());
    }

    /**
     * GETs a JSON document. Non-2xx answers throw Jsoup's {@link HttpStatusException}, so the base class classifies them.
     */
    String fetchJson(String url) throws IOException {
        long start = System.nanoTime();
        try {
            String body = Jsoup.connect(url)
                    .userAgent(userAgentService.getUserAgent(SOURCE_IDENTIFIER))
                    .timeout(DownloaderConstants.DEFAULT_TIMEOUT)
                    .ignoreContentType(true)
                    .execute()
                    .body();
            log.debug("GET {} [{}] -> OK in {}ms", url, SOURCE_IDENTIFIER, (System.nanoTime() - start) / 1_000_000);
            return body;
        } catch (HttpStatusException e) {
            log.debug("GET {} [{}] -> HTTP {} in {}ms", url, SOURCE_IDENTIFIER, e.getStatusCode(), (System.nanoTime() - start) / 1_000_000);
            throw e;
        }
    }

    /**
     * Parses one strip's {@code info.0.json}.
     *
     * @throws IllegalStateException if the JSON is malformed or lacks the strip number or date
     */
    XkcdStrip parseStrip(String json) {
        XkcdJson parsed;
        try {
            parsed = GSON.fromJson(json, XkcdJson.class);
        } catch (JsonParseException e) {
            throw new IllegalStateException("Could not parse xkcd JSON: " + e.getMessage(), e);
        }
        if (parsed == null || parsed.num() <= 0) {
            throw new IllegalStateException("xkcd JSON has no strip number");
        }
        LocalDate date;
        try {
            date = LocalDate.of(Integer.parseInt(parsed.year()), Integer.parseInt(parsed.month()), Integer.parseInt(parsed.day()));
        } catch (NumberFormatException | DateTimeException e) {
            throw new IllegalStateException(String.format("xkcd #%d has no valid date (%s-%s-%s)", parsed.num(), parsed.year(), parsed.month(), parsed.day()), e);
        }
        String alt = parsed.alt() == null || parsed.alt().isBlank() ? null : parsed.alt().trim();
        return new XkcdStrip(parsed.num(), date, parsed.img(), parsed.safeTitle(), alt);
    }

    /**
     * The fields of {@code info.0.json} the strategy reads.
     */
    private record XkcdJson(int num, String year, String month, String day, String img,
            @SerializedName("safe_title") String safeTitle, String alt) {
    }

    /**
     * One parsed strip. The alt text (the hover caption) is kept as the transcript.
     */
    record XkcdStrip(int num, LocalDate date, String img, String safeTitle, String alt) {

        /** Interactive strips give the bare image directory ({@code https://imgs.xkcd.com/comics/}) instead of a file. */
        boolean hasImage() {
            return img != null && !img.isBlank() && !img.endsWith("/");
        }
    }
}
