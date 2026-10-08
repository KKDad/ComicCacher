package org.stapledon.engine.source;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.infrastructure.web.UserAgentService;
import org.stapledon.engine.downloader.BrowserFetcher;
import org.stapledon.engine.downloader.ComicDownloaderStrategy;
import org.stapledon.engine.downloader.GoComicsDownloaderStrategy;
import org.stapledon.engine.downloader.RateLimitedException;
import org.stapledon.engine.downloader.SourceThrottleService;

/**
 * GoComics (Andrews McMeel). The catalog is the A–Z page: one anchor per comic, each carrying a JSON-LD {@code ImageObject} with the title, author and
 * badge image. A strip page embeds the comic's first strip date ({@code "firstDate"}) in its Next.js data, and an about page its description and
 * categories. The strip page is read as its RSC payload, as for downloads: GoComics' firewall refuses full page loads of strip pages from our IP.
 */
@Slf4j
@Component
public class GoComicsSource implements ComicSource {

    static final String ID = "gocomics";
    static final String BASE_URL = "https://www.gocomics.com";
    static final String CATALOG_PATH = "/comics/a-to-z";

    /** The A–Z page is about 16 MB of HTML. */
    private static final int CATALOG_MAX_BYTES = 64 * 1024 * 1024;
    private static final int CATALOG_TIMEOUT_MS = 60 * 1000;
    private static final Pattern FIRST_DATE = Pattern.compile("firstDate\\\\?\"\\s*:\\s*\\\\?\"(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern CATEGORIES = Pattern.compile("comic\\\\?\"\\s*:\\s*\\{\\s*\\\\?\"categories\\\\?\"\\s*:\\s*\\[(.*?)]");
    private static final Pattern CATEGORY_NAME = Pattern.compile("\\\\?\"name\\\\?\"\\s*:\\s*\\\\?\"([^\"\\\\]+)");

    private final GoComicsDownloaderStrategy downloader;
    private final BrowserFetcher fetcher;
    private final SourceThrottleService throttle;
    private final String baseUrl;

    @Autowired
    public GoComicsSource(GoComicsDownloaderStrategy downloader, UserAgentService userAgentService, SourceThrottleService throttle) {
        this(downloader, new BrowserFetcher(userAgentService), throttle, BASE_URL);
    }

    GoComicsSource(GoComicsDownloaderStrategy downloader, BrowserFetcher fetcher, SourceThrottleService throttle, String baseUrl) {
        this.downloader = downloader;
        this.fetcher = fetcher;
        this.throttle = throttle;
        this.baseUrl = baseUrl;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "GoComics";
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
        return comic.getName() == null ? "" : comic.getName().replace(" ", "").toLowerCase(Locale.ROOT);
    }

    /**
     * The comic's page with the {@code _rsc} parameter the site's router sends. GoComics' firewall answers the plain page with 403 from our network
     * (#443), but serves it with that parameter.
     */
    @Override
    public String comicPageUrl(String identifier) {
        String path = "/" + identifier;
        return BASE_URL + path + "?_rsc=" + BrowserFetcher.nextJsCacheBuster(path);
    }

    @Override
    public Set<String> imageHosts() {
        return Set.of("gocomicscmsassets.gocomics.com", "featureassets.gocomics.com", "assets.amuniversal.com");
    }

    @Override
    public Optional<SourceCatalog> catalog() {
        return Optional.of(new SourceCatalog() {
            @Override
            public String catalogUrl() {
                return BASE_URL + CATALOG_PATH;
            }

            @Override
            public List<SourceCatalogEntry> fetch() throws IOException {
                String url = baseUrl + CATALOG_PATH;
                Document page = throttle.withRetries(ID, () -> fetcher.fetchDocument(ID, url, CATALOG_TIMEOUT_MS, CATALOG_MAX_BYTES));
                List<SourceCatalogEntry> entries = parseCatalog(page);
                if (entries.isEmpty()) {
                    throw new IOException("No comics found on " + url + "; the page layout may have changed");
                }
                return entries;
            }
        });
    }

    /**
     * Reads the comic page's RSC payload, as the site's router does when a reader clicks through from the home page. A comic GoComics no longer
     * has comes back as its not-found page, with no {@code firstDate}.
     */
    @Override
    public Optional<StartDetector> startDetector() {
        return Optional.of(comic -> {
            String url = baseUrl + "/" + identifierFor(comic);
            String flight = throttle.withRetries(ID, () -> fetcher.fetchNextJsFlight(ID, url, "/", BrowserFetcher.DEFAULT_TIMEOUT_MS,
                    BrowserFetcher.DEFAULT_MAX_BODY_BYTES));
            return parseFirstDate(flight).map(StartInfo::ofDate);
        });
    }

    /**
     * Reads the comic's about page. Fails fast on HTTP 429: backs the whole source off once and rethrows, so a background run stops instead of waiting
     * through retries.
     */
    @Override
    public Optional<DetailsFetcher> detailsFetcher() {
        return Optional.of(identifier -> {
            String url = baseUrl + "/" + identifier + "/about";
            throttle.await(ID);
            try {
                Document page = fetcher.fetchDocument(ID, url, BrowserFetcher.DEFAULT_TIMEOUT_MS, BrowserFetcher.DEFAULT_MAX_BODY_BYTES);
                return parseDetails(page);
            } catch (RateLimitedException e) {
                throttle.backOff(ID, 1, e.getRetryAfter());
                throw e;
            }
        });
    }

    /**
     * The about page's description (its {@code ComicSeries} JSON-LD) and categories (the {@code "comic":{"categories":[…]}} in its Next.js data).
     */
    static Optional<CatalogDetails> parseDetails(Document page) {
        String description = null;
        for (Element ld : page.select("script[type=application/ld+json]")) {
            try {
                JsonElement json = JsonParser.parseString(ld.data());
                if (json.isJsonObject() && text(json.getAsJsonObject().get("@type")).filter("ComicSeries"::equals).isPresent()) {
                    description = text(json.getAsJsonObject().get("description")).map(String::trim).filter(d -> !d.isEmpty()).orElse(null);
                    break;
                }
            } catch (JsonParseException | IllegalStateException e) {
                log.debug("Skipping unreadable JSON-LD on a GoComics about page: {}", e.toString());
            }
        }
        List<String> tags = new ArrayList<>();
        Matcher categories = CATEGORIES.matcher(page.outerHtml());
        if (categories.find()) {
            Matcher name = CATEGORY_NAME.matcher(categories.group(1));
            while (name.find()) {
                tags.add(name.group(1).trim());
            }
        }
        return description == null && tags.isEmpty() ? Optional.empty() : Optional.of(new CatalogDetails(description, tags));
    }

    /**
     * Reads the A–Z page: anchors with the page's analytics location that carry a JSON-LD image object. Other links (subscribe, navigation) have none.
     */
    static List<SourceCatalogEntry> parseCatalog(Document page) {
        Map<String, SourceCatalogEntry> entries = new LinkedHashMap<>();
        for (Element link : page.select("a[data-analytics-location=" + CATALOG_PATH + "][href]")) {
            Element ld = link.selectFirst("script[type=application/ld+json]");
            String href = link.attr("href");
            if (ld == null || !href.startsWith("/")) {
                continue;
            }
            String identifier = href.substring(1);
            if (!ComicSource.isValidIdentifier(identifier)) {
                log.debug("Skipping GoComics catalog link with an unexpected path: {}", href);
                continue;
            }
            try {
                JsonObject json = JsonParser.parseString(ld.data()).getAsJsonObject();
                String name = text(json.get("name")).orElse(link.attr("data-analytics-label")).trim();
                if (name.isEmpty()) {
                    continue;
                }
                String author = Optional.ofNullable(json.get("author"))
                        .filter(JsonElement::isJsonObject)
                        .flatMap(a -> text(a.getAsJsonObject().get("name")))
                        .orElse(null);
                String image = text(json.get("contentUrl")).or(() -> text(json.get("url"))).orElse(null);
                entries.putIfAbsent(identifier, new SourceCatalogEntry(identifier, name, author, image, null));
            } catch (JsonParseException | IllegalStateException e) {
                log.debug("Skipping GoComics catalog entry {} with unreadable JSON-LD: {}", identifier, e.toString());
            }
        }
        return new ArrayList<>(entries.values());
    }

    /**
     * The {@code firstDate} a GoComics strip page embeds in its Next.js flight data (escaped inside the HTML page, plain in the RSC payload).
     */
    static Optional<LocalDate> parseFirstDate(String html) {
        Matcher m = FIRST_DATE.matcher(html);
        return m.find() ? Optional.of(LocalDate.parse(m.group(1))) : Optional.empty();
    }

    private static Optional<String> text(JsonElement element) {
        return element != null && element.isJsonPrimitive() ? Optional.of(element.getAsString()) : Optional.empty();
    }
}
