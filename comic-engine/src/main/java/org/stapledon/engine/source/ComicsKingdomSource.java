package org.stapledon.engine.source;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.parser.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.infrastructure.web.UserAgentService;
import org.stapledon.engine.downloader.BrowserFetcher;
import org.stapledon.engine.downloader.ComicDownloaderStrategy;
import org.stapledon.engine.downloader.ComicsKingdomDownloaderStrategy;
import org.stapledon.engine.downloader.SourceThrottleService;

/**
 * Comics Kingdom (King Features). The site is a Next.js front end over a public WordPress API, which lists every feature ({@code ck_feature}) with its
 * byline, badge image and oldest strip, so the catalog carries start dates for free.
 */
@Slf4j
@Component
public class ComicsKingdomSource implements ComicSource {

    static final String ID = "comicskingdom";
    static final String API_URL = "https://wp.comicskingdom.com/wp-json/wp/v2/ck_feature";

    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 20;
    private static final int PAGE_MAX_BYTES = 8 * 1024 * 1024;
    private static final int TIMEOUT_MS = 30 * 1000;
    private static final String FIELDS = "slug,title,meta.ck_byline_on_app,ck_oldest_comic,_links,_embedded";

    private final ComicsKingdomDownloaderStrategy downloader;
    private final BrowserFetcher fetcher;
    private final SourceThrottleService throttle;
    private final String apiUrl;

    @Autowired
    public ComicsKingdomSource(ComicsKingdomDownloaderStrategy downloader, UserAgentService userAgentService, SourceThrottleService throttle) {
        this(downloader, new BrowserFetcher(userAgentService), throttle, API_URL);
    }

    ComicsKingdomSource(ComicsKingdomDownloaderStrategy downloader, BrowserFetcher fetcher, SourceThrottleService throttle, String apiUrl) {
        this.downloader = downloader;
        this.fetcher = fetcher;
        this.throttle = throttle;
        this.apiUrl = apiUrl;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Comics Kingdom";
    }

    @Override
    public ComicDownloaderStrategy downloader() {
        return downloader;
    }

    @Override
    public String identifierFor(ComicItem comic) {
        if (comic.getSourceIdentifier() != null && !comic.getSourceIdentifier().isBlank()) {
            return comic.getSourceIdentifier().toLowerCase(Locale.ROOT);
        }
        return comic.getName() == null ? "" : comic.getName().replace(' ', '-').toLowerCase(Locale.ROOT);
    }

    @Override
    public String comicPageUrl(String identifier) {
        return "https://comicskingdom.com/" + identifier;
    }

    @Override
    public Set<String> imageHosts() {
        return Set.of("wp.comicskingdom.com", "api.kingdigital.com");
    }

    @Override
    public Optional<SourceCatalog> catalog() {
        return Optional.of(new SourceCatalog() {
            @Override
            public String catalogUrl() {
                return API_URL;
            }

            @Override
            public List<SourceCatalogEntry> fetch() throws IOException {
                Map<String, SourceCatalogEntry> entries = new LinkedHashMap<>();
                for (int page = 1; page <= MAX_PAGES; page++) {
                    String url = apiUrl + "?per_page=" + PAGE_SIZE + "&page=" + page + "&_embed=wp:featuredmedia&_fields=" + FIELDS;
                    String body = throttle.withRetries(ID, () -> fetcher.fetchJson(ID, url, TIMEOUT_MS, PAGE_MAX_BYTES));
                    JsonArray features = parseArray(body, url);
                    parseFeatures(features).forEach(entry -> entries.putIfAbsent(entry.identifier(), entry));
                    if (features.size() < PAGE_SIZE) {
                        break;
                    }
                }
                if (entries.isEmpty()) {
                    throw new IOException("No comics found at " + apiUrl + "; the API may have changed");
                }
                return new ArrayList<>(entries.values());
            }
        });
    }

    @Override
    public Optional<StartDetector> startDetector() {
        return Optional.of(comic -> {
            String url = apiUrl + "?slug=" + identifierFor(comic) + "&_fields=" + FIELDS;
            String body = throttle.withRetries(ID, () -> fetcher.fetchJson(ID, url, TIMEOUT_MS, PAGE_MAX_BYTES));
            return parseFeatures(parseArray(body, url)).stream()
                    .findFirst()
                    .map(SourceCatalogEntry::startDate)
                    .map(StartInfo::ofDate);
        });
    }

    private static JsonArray parseArray(String body, String url) throws IOException {
        try {
            JsonElement json = JsonParser.parseString(body);
            if (!json.isJsonArray()) {
                throw new IOException("Expected a JSON array from " + url);
            }
            return json.getAsJsonArray();
        } catch (JsonParseException e) {
            throw new IOException("Unreadable JSON from " + url + ": " + e.getMessage(), e);
        }
    }

    /**
     * Reads {@code ck_feature} objects: slug, title (HTML-escaped), byline, featured image and oldest strip.
     */
    static List<SourceCatalogEntry> parseFeatures(JsonArray features) {
        List<SourceCatalogEntry> entries = new ArrayList<>();
        for (JsonElement element : features) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject feature = element.getAsJsonObject();
            String identifier = text(feature, "slug").orElse(null);
            if (!ComicSource.isValidIdentifier(identifier)) {
                log.debug("Skipping Comics Kingdom feature with an unexpected slug: {}", identifier);
                continue;
            }
            String name = object(feature, "title").flatMap(t -> text(t, "rendered")).map(t -> Parser.unescapeEntities(t, false).trim()).orElse("");
            if (name.isEmpty()) {
                continue;
            }
            String author = object(feature, "meta").flatMap(m -> text(m, "ck_byline_on_app")).map(ComicsKingdomSource::stripBy).orElse(null);
            String image = object(feature, "_embedded")
                    .map(e -> e.get("wp:featuredmedia"))
                    .filter(JsonElement::isJsonArray)
                    .map(JsonElement::getAsJsonArray)
                    .filter(a -> !a.isEmpty() && a.get(0).isJsonObject())
                    .flatMap(a -> text(a.get(0).getAsJsonObject(), "source_url"))
                    .orElse(null);
            LocalDate start = object(feature, "ck_oldest_comic").flatMap(o -> text(o, "date")).flatMap(ComicsKingdomSource::date).orElse(null);
            entries.add(new SourceCatalogEntry(identifier, name, author, image, start));
        }
        return entries;
    }

    private static String stripBy(String byline) {
        String trimmed = byline.trim();
        String author = trimmed.regionMatches(true, 0, "By ", 0, 3) ? trimmed.substring(3).trim() : trimmed;
        return author.isEmpty() ? null : author;
    }

    private static Optional<LocalDate> date(String value) {
        try {
            return Optional.of(LocalDate.parse(value));
        } catch (DateTimeParseException _) {
            return Optional.empty();
        }
    }

    private static Optional<JsonObject> object(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        return element != null && element.isJsonObject() ? Optional.of(element.getAsJsonObject()) : Optional.empty();
    }

    private static Optional<String> text(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        return element != null && element.isJsonPrimitive() ? Optional.of(element.getAsString()) : Optional.empty();
    }
}
