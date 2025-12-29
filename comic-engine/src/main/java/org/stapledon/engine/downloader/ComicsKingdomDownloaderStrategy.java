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
 * Strategy implementation for downloading comics from Comics Kingdom.
 */
@Slf4j
@ToString
@Component
public class ComicsKingdomDownloaderStrategy extends AbstractComicDownloaderStrategy {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36";
    private static final int TIMEOUT = 5 * 1000;
    private static final String SOURCE_IDENTIFIER = "comicskingdom";
    private static final String ABOUT_SITE_STRING = "https://comicskingdom.com/%s/about";
    private static final String BASE_URL = "https://comicskingdom.com/";

    /**
     * Creates a new Comics Kingdom downloader strategy.
     *
     * @param webInspector           The web inspector to use for HTTP requests
     * @param imageValidationService The service for validating downloaded images
     */
    public ComicsKingdomDownloaderStrategy(InspectorService webInspector,
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

        Elements media = doc.select("meta");
        Elements imageElements = pickImages(media);

        if (imageElements == null || imageElements.isEmpty()) {
            log.error("No images were selected from the media");
            log.error("Site: {}", url);
            webInspector.dumpMedia(media);
            return null;
        }

        Element imageElement = imageElements.first();
        URL imageUrl = java.net.URI.create(imageElement.attr("content")).toURL();

        try (InputStream in = imageUrl.openStream()) {
            return in.readAllBytes();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[] downloadAvatarImage(int comicId, String comicName, String sourceIdentifier) throws Exception {
        String url = String.format(ABOUT_SITE_STRING,
                sourceIdentifier != null ? sourceIdentifier : comicName.replace(' ', '-'));
        log.debug("Fetching avatar from {}", url);

        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("DNT", "1")
                .header("Accept", "text/html,application/xhtml+xml,application/xml")
                .timeout(TIMEOUT)
                .get();

        Element featureAvatars = doc.select("img[src^=https://api.kingdigital.com/img/features/]").last();
        if (featureAvatars == null) {
            log.error("Unable to determine site avatar for comic {}", comicName);
            return null;
        }

        URL imageUrl = java.net.URI.create(featureAvatars.attr("abs:src")).toURL();
        try (InputStream in = imageUrl.openStream()) {
            return in.readAllBytes();
        }
    }

    /**
     * Generates the URL for a specific comic and date.
     */
    private String generateSiteURL(ComicDownloadRequest request) {
        String sourceIdentifier = request.getSourceIdentifier();
        String dateString = request.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Use either the source identifier or the comic name as the URL path
        String urlPath = sourceIdentifier != null && !sourceIdentifier.isEmpty() ? sourceIdentifier
                : request.getComicName().replace(' ', '-');

        return String.format("%s/%s/%s", BASE_URL, urlPath, dateString);
    }

    /**
     * Determines which links represent the comic image that we should cache.
     *
     * @param media list of image links to choose from
     * @return filtered list of elements containing only the comic images
     */
    private Elements pickImages(Elements media) {
        var elements = new Elements();

        for (Element src : media) {
            if (src.tagName().equals("meta") && src.attr("property").contains("og:image")) {
                elements.add(src);
            }
        }

        webInspector.dumpMedia(elements);

        // We get back 2-3 images. The 2nd image is the hi-res version - we'll select
        // it.
        if (elements.size() > 1) {
            var e = new Elements();
            e.add(elements.get(1));
            return e;
        }

        return elements;
    }
}