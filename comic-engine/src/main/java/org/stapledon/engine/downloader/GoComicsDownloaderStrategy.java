package org.stapledon.engine.downloader;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.brotli.dec.BrotliInputStream;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
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
    private static final Pattern CHROME_MAJOR_VERSION = Pattern.compile("Chrome/(\\d+)\\.");

    /**
     * Creates a new GoComics downloader strategy.
     */
    public GoComicsDownloaderStrategy(InspectorService webInspector,
            ValidationService imageValidationService,
            UserAgentService userAgentService,
            SourceThrottleService throttleService) {
        super(SOURCE_IDENTIFIER, webInspector, imageValidationService, userAgentService, throttleService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[] downloadComicImage(ComicDownloadRequest request) throws Exception {
        String url = generateSiteURL(request);
        log.debug("Fetching {}", url);

        Document doc = fetchDocument(url);

        // Extract image URL from Open Graph metadata
        String imageUrl = extractImageFromOpenGraph(doc);

        if (imageUrl == null) {
            log.warn("No Open Graph image found for {} on {} at {}", request.getComicName(), request.getDate(), url);
            return null;
        }

        log.debug("Found image via Open Graph metadata: {}", imageUrl);
        return downloadImageData(imageUrl);
    }

    /**
     * Extracts the comic image URL from Open Graph metadata tags.
     *
     * @param doc the parsed HTML document
     * @return the image URL from og:image meta tag, or null if not found
     */
    private String extractImageFromOpenGraph(Document doc) {
        Element ogImage = doc.selectFirst("meta[property=og:image]");
        if (ogImage != null && ogImage.hasAttr("content")) {
            return ogImage.attr("content");
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[] downloadAvatarImage(int comicId, String comicName, String sourceIdentifier) throws Exception {
        String comicNameParsed = comicName.replace(" ", "");
        String url = String.format("https://www.gocomics.com/%s/about", sourceIdentifier);
        log.debug("Fetching avatar from {}", url);

        Document doc = fetchDocument(url);

        // Try to find badge image in HTML using different potential CSS classes
        Element badgeImage = doc.select("img.Badge_badge__image__Y3HaD, img[src*=badge], img[src*=avatar]").first();
        if (badgeImage == null) {
            log.warn("No avatar image found for comic {} at {}", comicName, url);
            return null;
        }

        return downloadImageData(badgeImage.attr("abs:src"));
    }

    // GoComics serves Content-Encoding: br; Jsoup only auto-decompresses gzip, so we wrap the body stream manually when needed.
    // Headers mirror a desktop Chrome navigation. Chrome also advertises zstd, which we can't decode, so it is left out of Accept-Encoding.
    private Document fetchDocument(String url) throws IOException {
        long start = System.nanoTime();
        String userAgent = userAgentService.getUserAgent(SOURCE_IDENTIFIER);
        Connection connection = Jsoup.connect(url)
                .userAgent(userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Accept-Encoding", "gzip, deflate, br");
        chromeClientHints(userAgent).ifPresent(hint -> connection
                .header("Sec-Ch-Ua", hint)
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "\"Windows\""));
        Connection.Response response = connection
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .header("Upgrade-Insecure-Requests", "1")
                .timeout(TIMEOUT)
                .ignoreHttpErrors(true)
                .execute();
        log.debug("GET {} [{}] -> HTTP {} in {}ms", url, SOURCE_IDENTIFIER, response.statusCode(), (System.nanoTime() - start) / 1_000_000);

        if (response.statusCode() == RateLimitedException.HTTP_TOO_MANY_REQUESTS) {
            throw RateLimitedException.of(url, response.header("Retry-After"));
        }
        if (response.statusCode() >= 400) {
            throw new HttpStatusException("HTTP error fetching URL", response.statusCode(), url);
        }

        InputStream stream = response.bodyStream();
        if ("br".equalsIgnoreCase(response.header("Content-Encoding"))) {
            stream = new BrotliInputStream(stream);
        }
        try (InputStream body = stream) {
            return Jsoup.parse(body, response.charset(), response.url().toExternalForm());
        }
    }

    /**
     * Builds the {@code Sec-Ch-Ua} value real Chrome would send alongside {@code userAgent}, so the client hints never disagree with the UA string.
     * Empty when the UA isn't a Chrome UA (e.g. a Firefox per-source override), since other browsers don't send client hints.
     */
    static Optional<String> chromeClientHints(String userAgent) {
        if (userAgent == null) {
            return Optional.empty();
        }
        Matcher m = CHROME_MAJOR_VERSION.matcher(userAgent);
        if (!m.find()) {
            return Optional.empty();
        }
        String major = m.group(1);
        return Optional.of(String.format("\"Chromium\";v=\"%1$s\", \"Google Chrome\";v=\"%1$s\", \"Not?A_Brand\";v=\"99\"", major));
    }

    /**
     * Generates the URL for a specific comic and date.
     */
    private String generateSiteURL(ComicDownloadRequest request) {
        String comicNameParsed = request.getComicName().replace(" ", "");
        String sourceIdentifier = request.getSourceIdentifier();
        String dateString = request.getDate().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        // Use either the source identifier or the comic name as the URL path
        String urlPath = sourceIdentifier != null && !sourceIdentifier.isEmpty() ? sourceIdentifier : comicNameParsed;

        return String.format("https://www.gocomics.com/%s/%s/", urlPath, dateString).toLowerCase();
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
