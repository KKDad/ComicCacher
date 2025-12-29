package org.stapledon.engine.downloader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.service.ValidationService;

import java.io.InputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Strategy implementation for downloading comics from GoComics.
 */
@Slf4j
@ToString
@Component
public class GoComicsDownloaderStrategy extends AbstractComicDownloaderStrategy {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36";
    private static final int TIMEOUT = 5 * 1000;
    private static final String SOURCE_IDENTIFIER = "gocomics";

    /**
     * Creates a new GoComics downloader strategy.
     *
     * @param webInspector           The web inspector to use for HTTP requests
     * @param imageValidationService The service for validating downloaded images
     */
    public GoComicsDownloaderStrategy(InspectorService webInspector,
            ValidationService imageValidationService) {
        super(SOURCE_IDENTIFIER, webInspector, imageValidationService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[] downloadComicImage(ComicDownloadRequest request) throws Exception {
        String url = generateSiteURL(request);
        log.debug("Fetching {}", url);

        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("DNT", "1")
                .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                .timeout(TIMEOUT)
                .get();

        // Extract image URL from Open Graph metadata
        String imageUrl = extractImageFromOpenGraph(doc);

        if (imageUrl == null) {
            log.error("No Open Graph image found for URL: {}", url);
            return null;
        }

        log.debug("Found image via Open Graph metadata: {}", imageUrl);
        URL imgUrl = java.net.URI.create(imageUrl).toURL();
        try (InputStream in = imgUrl.openStream()) {
            return in.readAllBytes();
        }
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

        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("DNT", "1")
                .header("Accept", "text/html,application/xhtml+xml,application/xml")
                .timeout(TIMEOUT)
                .get();

        // Try to find badge image in HTML using different potential CSS classes
        Element badgeImage = doc.select("img.Badge_badge__image__Y3HaD, img[src*=badge], img[src*=avatar]").first();
        if (badgeImage == null) {
            log.error("No avatar image found for comic {}", comicName);
            return null;
        }

        URL imageUrl = java.net.URI.create(badgeImage.attr("abs:src")).toURL();
        try (InputStream in = imageUrl.openStream()) {
            return in.readAllBytes();
        }
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
            if (src.tagName().equals("img")) {
                // Check for main comic image indicators
                Element parent = src.parent();
                if (parent != null) {
                    // Look for the picture element that contains the main strip
                    if (parent.tagName().equals("picture") &&
                            parent.parent() != null &&
                            (parent.parent().className().contains("ComicImage") ||
                                    parent.parent().className().contains("comic__image") ||
                                    parent.parent().className().contains("item__image"))) {
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
                if (src.tagName().equals("img") &&
                        (src.attr("abs:src").contains("assets.amuniversal.com") ||
                                src.attr("abs:src").contains("featureassets.gocomics.com"))) {
                    elements.add(src);
                    break; // Take only the first matching image
                }
            }
        }

        // Second try: Look for images with certain classes or in specific containers
        if (elements.isEmpty()) {
            for (Element src : media) {
                if (src.tagName().equals("img")) {
                    // Check for images in containers with specific class names
                    if (src.parent() != null &&
                            (src.parent().className().contains("comic") ||
                                    src.parent().className().contains("ShowComicViewer"))) {
                        elements.add(src);
                    }
                    // Check for images that are large enough to likely be the comic
                    else if (src.hasAttr("width") && src.hasAttr("height")) {
                        try {
                            int width = Integer.parseInt(src.attr("width"));
                            int height = Integer.parseInt(src.attr("height"));
                            if (width > 400 && height > 200) {
                                elements.add(src);
                            }
                        } catch (NumberFormatException ignored) {
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
                } catch (NumberFormatException ignored) {
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