package org.stapledon.engine.downloader;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.infrastructure.web.UserAgentService;
import org.stapledon.common.service.ValidationService;

/**
 * Strategy implementation for downloading comics from GoComics.
 */
@Slf4j
@ToString
@Component
public class GoComicsDownloaderStrategy extends AbstractDailyDownloaderStrategy {

    private static final int TIMEOUT = 5 * 1000;
    private static final String SOURCE_IDENTIFIER = "gocomics";
    private static final DateTimeFormatter PATH_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** A strip object in the RSC payload: its image URL, then its date (both come before any nested object). */
    private static final Pattern COMIC_OBJECT = Pattern.compile("\"comic\":\\{[^{}]*?\"url\":\"([^\"]+)\"[^{}]*?\"date\":\"(\\d{4}-\\d{2}-\\d{2})T");

    private final BrowserFetcher browserFetcher;

    /**
     * Creates a new GoComics downloader strategy.
     */
    public GoComicsDownloaderStrategy(InspectorService webInspector,
            ValidationService imageValidationService,
            UserAgentService userAgentService,
            SourceThrottleService throttleService) {
        super(SOURCE_IDENTIFIER, webInspector, imageValidationService, userAgentService, throttleService);
        this.browserFetcher = new BrowserFetcher(userAgentService);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the strip from the date page's React Server Components payload, which the site's own router fetches when a reader clicks through to
     * a date. GoComics' firewall has refused full page loads of strip pages while still serving these, so the HTML page isn't fetched at all.
     */
    @Override
    protected byte[] downloadComicImage(ComicDownloadRequest request) throws Exception {
        String slug = comicSlug(request);
        String url = String.format("https://www.gocomics.com/%s/%s", slug, request.getDate().format(PATH_DATE));
        log.debug("Fetching page data for {}", url);

        String flight = browserFetcher.fetchNextJsFlight(SOURCE_IDENTIFIER, url, "/" + slug, TIMEOUT, BrowserFetcher.DEFAULT_MAX_BODY_BYTES);
        Optional<String> imageUrl = stripImageUrl(flight, request.getDate());
        if (imageUrl.isEmpty()) {
            log.warn("No strip for {} on {} in the page data from {}", request.getComicName(), request.getDate(), url);
            return null;
        }

        log.debug("Found strip image {}", imageUrl.get());
        return downloadImageData(imageUrl.get());
    }

    /**
     * Finds the strip for {@code date} in a date page's RSC payload. The payload holds several {@code "comic":{...}} objects (the page's strip
     * plus related strips from other dates), so the one whose {@code date} matches is taken; none matching means the source has no strip that day.
     */
    static Optional<String> stripImageUrl(String flight, LocalDate date) {
        Matcher m = COMIC_OBJECT.matcher(flight);
        while (m.find()) {
            if (m.group(2).equals(date.toString())) {
                return Optional.of(m.group(1));
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[] downloadAvatarImage(int comicId, String comicName, String sourceIdentifier) throws Exception {
        String url = String.format("https://www.gocomics.com/%s/about", sourceIdentifier);
        log.debug("Fetching avatar from {}", url);

        Document doc = fetchDocument(url);

        // Try to find badge image in HTML using different potential CSS classes
        Element badgeImage = doc.selectFirst("img.Badge_badge__image__Y3HaD, img[src*=badge], img[src*=avatar]");
        if (badgeImage == null) {
            log.warn("No avatar image found for comic {} at {}", comicName, url);
            return null;
        }

        return downloadImageData(badgeImage.attr("abs:src"));
    }

    // Headers mirror a desktop Chrome navigation, which Cloudflare expects (see BrowserFetcher)
    private Document fetchDocument(String url) throws IOException {
        return browserFetcher.fetchDocument(SOURCE_IDENTIFIER, url, TIMEOUT, BrowserFetcher.DEFAULT_MAX_BODY_BYTES);
    }

    /**
     * The comic's path on gocomics.com: its source identifier, or else its name without spaces, lower-cased.
     */
    private static String comicSlug(ComicDownloadRequest request) {
        String sourceIdentifier = request.getSourceIdentifier();
        String slug = sourceIdentifier != null && !sourceIdentifier.isEmpty() ? sourceIdentifier : request.getComicName().replace(" ", "");
        return slug.toLowerCase(Locale.ROOT);
    }

    /**
     * Determines which links represent the comic image that we should cache.
     *
     * @param media list of image links to choose from
     * @return filtered list of elements containing only the comic images
     */
    private Elements pickImages(Elements media) {
        var elements = new Elements();

        // First try: Look for the main comic image using specific selectors
        // GoComics uses a specific class or data attribute for the main strip
        for (Element src : media) {
            if ("img".equals(src.tagName())) {
                // Check for main comic image indicators
                Element parent = src.parent();
                if (parent != null) {
                    // Look for the picture element that contains the main strip
                    if ("picture".equals(parent.tagName())
                            && parent.parent() != null
                            && (parent.parent().className().contains("ComicImage")
                            || parent.parent().className().contains("comic__image")
                            || parent.parent().className().contains("item__image"))) {
                        elements.add(src);
                        break; // Found the main comic, stop looking
                    }
                }
            }
        }

        // Second try: Look for the FIRST image from GoComics domains
        // The main comic strip is typically the first one on the page
        if (elements.isEmpty()) {
            for (Element src : media) {
                if ("img".equals(src.tagName())
                        && (src.attr("abs:src").contains("assets.amuniversal.com")
                        || src.attr("abs:src").contains("featureassets.gocomics.com"))) {
                    elements.add(src);
                    break; // Take only the first matching image
                }
            }
        }

        // Second try: Look for images with certain classes or in specific containers
        if (elements.isEmpty()) {
            for (Element src : media) {
                if ("img".equals(src.tagName())) {
                    // Check for images in containers with specific class names
                    if (src.parent() != null
                            && (src.parent().className().contains("comic")
                            || src.parent().className().contains("ShowComicViewer"))) {
                        elements.add(src);
                    } else if (src.hasAttr("width") && src.hasAttr("height")) {
                        // Check for images that are large enough to likely be the comic
                        try {
                            int width = Integer.parseInt(src.attr("width"));
                            int height = Integer.parseInt(src.attr("height"));
                            if (width > 400 && height > 200) {
                                elements.add(src);
                            }
                        } catch (NumberFormatException _) {
                            // If we can't parse the dimensions, just ignore this element
                        }
                    }
                }
            }
        }

        log.debug("Found {} potential comic images", elements.size());
        webInspector.dumpMedia(elements);

        // If we have multiple images, try to find the highest resolution one
        if (elements.size() > 1) {
            // Try to find the image with the largest dimensions
            Element largest = elements.first();
            int maxSize = 0;

            for (Element img : elements) {
                try {
                    if (img.hasAttr("width") && img.hasAttr("height")) {
                        int width = Integer.parseInt(img.attr("width"));
                        int height = Integer.parseInt(img.attr("height"));
                        int size = width * height;

                        if (size > maxSize) {
                            maxSize = size;
                            largest = img;
                        }
                    }
                } catch (NumberFormatException _) {
                    // Continue to next element if we can't parse dimensions
                }
            }

            var e = new Elements();
            e.add(largest);
            return e;
        }

        return elements;
    }
}
