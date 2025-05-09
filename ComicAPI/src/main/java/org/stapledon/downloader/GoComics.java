package org.stapledon.downloader;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector;
import org.stapledon.dto.ComicItem;
import org.stapledon.web.WebInspector;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
public class GoComics extends DailyComic {
    public GoComics(WebInspector inspector) {
        super(inspector, "[src]");
    }


    /**
     * Determines when the latest published image it. Some comics are only available on the web a couple days or
     * a week after they were published in print.
     *
     * @return Mst recent date we can get a api for
     */
    public LocalDate getLastStripOn() {
        return LocalDate.now();
    }

    /**
     * Generate the URL for a specific date.
     */
    @Override
    protected String generateSiteURL() {
        return String.format("https://www.gocomics.com/%s/%s/", this.comicNameParsed, this.currentDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))).toLowerCase();
    }

    /**
     * Link to the About the comic page
     *
     * @return URL where we can get the 'about' information for this strip
     */
    private String generateAboutURL() {
        return String.format("https://www.gocomics.com/%s/about", this.comicNameParsed);
    }

    /**
     * Updates the metadata for a comic strip by fetching information from the GoComics website.
     * <p>
     * The method attempts to extract author information and description from the comic's page,
     * looking for structured data in JSON-LD format. If this fails, it will use a generic
     * approach to try to find the information elsewhere on the page.
     * <p>
     * Due to the website's structure potentially changing over time, this method is designed
     * to gracefully degrade by providing sensible defaults when information can't be extracted.
     *
     * @param comicItem The ComicItem to update with metadata from the web
     */
    public void updateComicMetadata(ComicItem comicItem) {
        try {
            String url = this.generateSiteURL();
            log.info("Getting comic metadata from {}", url);

            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("DNT", "1")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml")
                    .timeout(TIMEOUT)
                    .get();

            // Set default values in case extraction fails
            comicItem.setDescription("A comic strip published on GoComics");
            comicItem.setAuthor("By " + inferAuthorFromComicName(this.comicNameParsed));

            // Try multiple extraction methods in order of preference
            boolean authorFound = extractFromJsonLd(doc, comicItem) ||
                                  extractFromMetaTags(doc, comicItem) ||
                                  extractFromPageTitle(doc, comicItem);

            // Set avatar available regardless
            comicItem.setAvatarAvailable(true);

            // Cache the avatar if we don't already have it
            var avatarCached = new File(String.format("%s/avatar.png", this.cacheLocation()));
            if (!avatarCached.exists()) {
                // Try to find badge image in HTML using different potential CSS classes
                Element badgeImage = doc.select("img.Badge_badge__image__Y3HaD, img[src*=badge], img[src*=avatar]").first();
                if (badgeImage != null) {
                    comicItem.setAvatarAvailable(cacheImage(badgeImage, avatarCached.getAbsolutePath()));
                    log.trace("Avatar has been cached");
                }
            }
        } catch (Exception e) {
            log.error("Error updating comic metadata: {}", e.getMessage());
            // Ensure we always have valid data even after errors
            if (comicItem.getDescription() == null) {
                comicItem.setDescription("A comic strip published on GoComics");
            }
            if (comicItem.getAuthor() == null) {
                comicItem.setAuthor("By " + inferAuthorFromComicName(this.comicNameParsed));
            }
            comicItem.setAvatarAvailable(true);
        }
    }

    /**
     * Attempts to extract comic metadata from JSON-LD structured data in the page
     */
    private boolean extractFromJsonLd(Document doc, ComicItem comicItem) {
        try {
            Elements jsonScripts = doc.select("script[type=application/ld+json]");

            for (Element script : jsonScripts) {
                String json = script.html();

                // Look for author information in JSON-LD
                if (json.contains("\"author\":{") && json.contains("\"name\":")) {
                    // Extract author name
                    int nameStartIndex = json.indexOf("\"name\":\"", json.indexOf("\"author\":{")) + 8;
                    if (nameStartIndex > 8) {
                        int nameEndIndex = json.indexOf("\"", nameStartIndex);
                        if (nameEndIndex > nameStartIndex) {
                            String authorName = json.substring(nameStartIndex, nameEndIndex);
                            comicItem.setAuthor("By " + authorName);

                            // Also try to extract description
                            int descStartIndex = json.indexOf("\"description\":\"") + 14;
                            if (descStartIndex > 14) {
                                int descEndIndex = json.indexOf("\"", descStartIndex);
                                if (descEndIndex > descStartIndex) {
                                    String description = json.substring(descStartIndex, descEndIndex);
                                    comicItem.setDescription(description);
                                }
                            }
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract metadata from JSON-LD: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Attempts to extract comic metadata from meta tags in the page header
     */
    private boolean extractFromMetaTags(Document doc, ComicItem comicItem) {
        try {
            // Try to get author from meta tags
            Element authorMeta = doc.select("meta[name=author], meta[property=og:author]").first();
            if (authorMeta != null) {
                String authorName = authorMeta.attr("content");
                if (authorName != null && !authorName.isEmpty()) {
                    comicItem.setAuthor("By " + authorName);

                    // Also try to get description
                    Element descMeta = doc.select("meta[name=description], meta[property=og:description]").first();
                    if (descMeta != null) {
                        String desc = descMeta.attr("content");
                        if (desc != null && !desc.isEmpty()) {
                            comicItem.setDescription(desc);
                        }
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract metadata from meta tags: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Attempts to extract comic metadata from the page title
     */
    private boolean extractFromPageTitle(Document doc, ComicItem comicItem) {
        try {
            Element titleElement = doc.select("title").first();
            if (titleElement != null) {
                String title = titleElement.text();
                // Try to extract author name from title if it follows patterns like "Comic Name by Author Name"
                if (title.contains(" by ")) {
                    String authorName = title.substring(title.lastIndexOf(" by ") + 4).trim();
                    comicItem.setAuthor("By " + authorName);
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract metadata from page title: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Infers a likely author name based on the comic name
     * Used as a last resort when online extraction fails
     */
    private String inferAuthorFromComicName(String comicName) {
        if (comicName == null) {
            return "Unknown Creator";
        }

        // Format comic name to create a more human-readable default
        String formattedName = comicName.replace("-", " ")
                                       .replaceAll("([a-z])([A-Z])", "$1 $2");

        return "Creator of " + formattedName;
    }

    /**
     * Determines which links represent the comic image that we should cache
     *
     * @param media list of image links to choose from
     */
    @Override
    protected Elements pickImages(Elements media) {
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
