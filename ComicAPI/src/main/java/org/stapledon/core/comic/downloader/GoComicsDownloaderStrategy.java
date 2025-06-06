package org.stapledon.core.comic.downloader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.stapledon.core.comic.dto.ComicDownloadRequest;
import org.stapledon.infrastructure.web.WebInspector;

import java.io.InputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;

import lombok.extern.slf4j.Slf4j;

/**
 * Strategy implementation for downloading comics from GoComics.
 */
@Slf4j
@Component
public class GoComicsDownloaderStrategy extends AbstractComicDownloaderStrategy {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36";
    private static final int TIMEOUT = 5 * 1000;
    private static final String SOURCE_IDENTIFIER = "gocomics";

    /**
     * Creates a new GoComics downloader strategy.
     *
     * @param webInspector The web inspector to use for HTTP requests
     */
    public GoComicsDownloaderStrategy(WebInspector webInspector) {
        super(SOURCE_IDENTIFIER, webInspector);
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
        
        Elements media = doc.select("[src]");
        Elements imageElements = pickImages(media);
        
        if (imageElements == null || imageElements.isEmpty()) {
            log.error("No images were selected from the media");
            log.error("Site: {}", url);
            webInspector.dumpMedia(media);
            return null;
        }
        
        Element imageElement = imageElements.first();
        URL imageUrl = new URL(imageElement.attr("abs:src"));
        
        try (InputStream in = imageUrl.openStream()) {
            return in.readAllBytes();
        }
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
        
        URL imageUrl = new URL(badgeImage.attr("abs:src"));
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
        String urlPath = sourceIdentifier != null && !sourceIdentifier.isEmpty() ? 
                sourceIdentifier : comicNameParsed;
                
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

        // First try: Look for images from either the old or new domain patterns
        for (Element src : media) {
            if (src.tagName().equals("img") &&
                (src.attr("abs:src").contains("assets.amuniversal.com") ||
                 src.attr("abs:src").contains("featureassets.gocomics.com"))) {
                elements.add(src);
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