package org.stapledon.engine.downloader;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.service.ValidationService;

/**
 * Strategy implementation for downloading comics from Freefall (freefall.purrsia.com).
 * Freefall uses sequential strip numbers rather than date-based URLs.
 */
@Slf4j
@ToString(callSuper = true)
@Component
public class FreefallDownloaderStrategy extends AbstractComicDownloaderStrategy
        implements IndexedComicDownloaderStrategy {

    static final String SOURCE_IDENTIFIER = "freefall";
    static final String BASE_URL = "http://freefall.purrsia.com";
    private static final String AVATAR_URL = BASE_URL + "/fflogo.gif";
    private static final int TIMEOUT = 10 * 1000;

    private static final Pattern TITLE_PATTERN = Pattern.compile("Freefall\\s+(\\d+)(?:\\s+(.+))?");
    private static final DateTimeFormatter TITLE_DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    /**
     * Creates a new Freefall downloader strategy.
     */
    public FreefallDownloaderStrategy(InspectorService webInspector,
            ValidationService imageValidationService) {
        super(SOURCE_IDENTIFIER, webInspector, imageValidationService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComicDownloadResult downloadLatestStrip(ComicItem comic) {
        try {
            String url = BASE_URL + "/default.htm";
            log.info("Fetching latest Freefall strip from {}", url);

            Document doc = Jsoup.connect(url).timeout(TIMEOUT).get();

            return downloadStripFromDocument(comic, doc, url);
        } catch (Exception e) {
            String errorMessage = String.format("Error downloading latest Freefall strip: %s", e.getMessage());
            log.error(errorMessage, e);
            ComicDownloadRequest request = buildRequest(comic, LocalDate.now());
            return ComicDownloadResult.failure(request, errorMessage);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComicDownloadResult downloadStrip(ComicItem comic, int stripNumber) {
        try {
            String url = buildStripPageUrl(stripNumber);
            log.info("Fetching Freefall strip #{} from {}", stripNumber, url);

            Document doc;
            try {
                doc = Jsoup.connect(url).timeout(TIMEOUT).get();
            } catch (org.jsoup.HttpStatusException e) {
                // Try grayscale version for older strips
                String grayUrl = buildGrayscaleStripPageUrl(stripNumber);
                log.debug("Color page not found, trying grayscale: {}", grayUrl);
                doc = Jsoup.connect(grayUrl).timeout(TIMEOUT).get();
            }

            return downloadStripFromDocument(comic, doc, url);
        } catch (Exception e) {
            String errorMessage = String.format("Error downloading Freefall strip #%d: %s",
                    stripNumber, e.getMessage());
            log.error(errorMessage, e);
            ComicDownloadRequest request = buildRequest(comic, LocalDate.now());
            return ComicDownloadResult.failure(request, errorMessage);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[] downloadAvatarImage(int comicId, String comicName, String sourceIdentifier) throws Exception {
        log.debug("Downloading Freefall avatar from {}", AVATAR_URL);
        URL imgUrl = java.net.URI.create(AVATAR_URL).toURL();
        try (InputStream in = imgUrl.openStream()) {
            return in.readAllBytes();
        }
    }

    /**
     * Processes a fetched Freefall page document: parses title, extracts image, parses transcript.
     */
    private ComicDownloadResult downloadStripFromDocument(ComicItem comic, Document doc, String pageUrl) throws Exception {
        // Parse title to get strip number and date
        String title = doc.title();
        TitleParseResult titleResult = parseTitleTag(title);

        if (titleResult == null) {
            ComicDownloadRequest request = buildRequest(comic, LocalDate.now());
            return ComicDownloadResult.failure(request, "Could not parse Freefall title: " + title);
        }

        // Determine the actual date
        LocalDate actualDate = titleResult.date();
        if (actualDate == null) {
            actualDate = parseDateFromComment(doc);
        }

        // Extract and download the image
        String imageUrl = extractImageUrl(doc, titleResult.stripNumber());
        if (imageUrl == null) {
            ComicDownloadRequest request = buildRequest(comic, actualDate);
            return ComicDownloadResult.failure(request, "Could not find image for strip #" + titleResult.stripNumber());
        }

        // Make URL absolute if needed
        if (!imageUrl.startsWith("http")) {
            imageUrl = BASE_URL + "/" + imageUrl;
        }

        log.debug("Downloading image from {}", imageUrl);
        URL imgUrl = java.net.URI.create(imageUrl).toURL();
        byte[] imageData;
        try (InputStream in = imgUrl.openStream()) {
            imageData = in.readAllBytes();
        }

        // Validate the image
        ImageValidationResult validation = validateImage(imageData, comic.getName(),
                "strip #" + titleResult.stripNumber());
        if (validation == null) {
            ComicDownloadRequest request = buildRequest(comic, actualDate);
            return ComicDownloadResult.failure(request,
                    "Invalid image for strip #" + titleResult.stripNumber());
        }

        // Parse transcript
        String transcript = parseTranscript(doc);

        ComicDownloadRequest request = buildRequest(comic, actualDate);
        return ComicDownloadResult.successWithMetadata(request, imageData, actualDate,
                titleResult.stripNumber(), transcript);
    }

    /**
     * Parses the Freefall title tag to extract strip number and optional date.
     * Title format: "Freefall 04350 March 18, 2026" (modern) or "Freefall 00001" (old)
     */
    TitleParseResult parseTitleTag(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }

        Matcher matcher = TITLE_PATTERN.matcher(title.trim());
        if (!matcher.matches()) {
            return null;
        }

        int stripNumber = Integer.parseInt(matcher.group(1));
        LocalDate date = null;

        if (matcher.group(2) != null) {
            try {
                date = LocalDate.parse(matcher.group(2).trim(), TITLE_DATE_FORMAT);
            } catch (DateTimeParseException e) {
                log.debug("Could not parse date from title: {}", matcher.group(2));
            }
        }

        return new TitleParseResult(stripNumber, date);
    }

    /**
     * For old strips, extracts the date from an HTML comment like {@code <!- April 9, 1998 ->}.
     */
    LocalDate parseDateFromComment(Document doc) {
        return findDateInCommentNodes(doc.body());
    }

    /**
     * Recursively walks the DOM tree to find Comment nodes containing parseable dates.
     */
    private LocalDate findDateInCommentNodes(Node parent) {
        if (parent == null) {
            return null;
        }

        for (Node node : parent.childNodes()) {
            if (node instanceof org.jsoup.nodes.Comment comment) {
                String commentText = comment.getData().trim();
                // Strip leading/trailing dashes that Jsoup may add
                commentText = commentText.replaceAll("^-+\\s*", "").replaceAll("\\s*-+$", "");
                try {
                    return LocalDate.parse(commentText.trim(), TITLE_DATE_FORMAT);
                } catch (DateTimeParseException e) {
                    // Not a date comment, continue
                }
            } else if (node instanceof Element) {
                LocalDate found = findDateInCommentNodes(node);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    /**
     * Extracts transcript text from the page.
     * Freefall transcripts appear after a {@code <FONT SIZE=+1>TRANSCRIPT</FONT>} heading.
     */
    String parseTranscript(Document doc) {
        // Look for the transcript section
        for (Element font : doc.select("font")) {
            if ("TRANSCRIPT".equalsIgnoreCase(font.text().trim())) {
                // The font element and transcript text are siblings inside the same parent
                Element parent = font.parent();
                if (parent == null) {
                    continue;
                }

                StringBuilder transcript = new StringBuilder();
                boolean foundTranscript = false;

                for (Node node : parent.childNodes()) {
                    // Skip nodes until we find the TRANSCRIPT font element
                    if (!foundTranscript) {
                        if (node instanceof Element el
                                && "font".equalsIgnoreCase(el.tagName())
                                && "TRANSCRIPT".equalsIgnoreCase(el.text().trim())) {
                            foundTranscript = true;
                        }
                        continue;
                    }

                    // Collect text from text nodes after the transcript heading
                    if (node instanceof TextNode textNode) {
                        String text = textNode.text().trim();
                        if (!text.isEmpty()) {
                            if (!transcript.isEmpty()) {
                                transcript.append("\n");
                            }
                            transcript.append(text);
                        }
                    } else if (node instanceof Element element
                            && !"br".equalsIgnoreCase(element.tagName())) {
                        String text = element.text().trim();
                        if (!text.isEmpty()) {
                            if (!transcript.isEmpty()) {
                                transcript.append("\n");
                            }
                            transcript.append(text);
                        }
                    }
                }

                String result = transcript.toString().trim();
                return result.isEmpty() ? null : result;
            }
        }

        return null;
    }

    /**
     * Builds the URL for a color strip page.
     * Formula: /ff{folder}/fc{NNNNN}.htm
     */
    String buildStripPageUrl(int stripNumber) {
        int folder = calculateFolderNumber(stripNumber);
        String padded = String.format("%05d", stripNumber);
        return String.format("%s/ff%d/fc%s.htm", BASE_URL, folder, padded);
    }

    /**
     * Builds the URL for a grayscale strip page (for older strips).
     * Formula: /ff{folder}/fv{NNNNN}.htm
     */
    private String buildGrayscaleStripPageUrl(int stripNumber) {
        int folder = calculateFolderNumber(stripNumber);
        String padded = String.format("%05d", stripNumber);
        return String.format("%s/ff%d/fv%s.htm", BASE_URL, folder, padded);
    }

    /**
     * Calculates the folder number for a given strip number.
     * Formula: ((stripNumber - 1) / 100 + 1) * 100
     */
    int calculateFolderNumber(int stripNumber) {
        return ((stripNumber - 1) / 100 + 1) * 100;
    }

    /**
     * Extracts the image URL from the document for the given strip number.
     * Looks for an img tag whose src contains the padded strip number.
     */
    String extractImageUrl(Document doc, int stripNumber) {
        String padded = String.format("%05d", stripNumber);

        // Try color version first (fc prefix, .png)
        Element img = doc.selectFirst("img[src*=" + padded + "]");
        if (img != null) {
            return img.attr("src");
        }

        // Try without padding
        String unpadded = String.valueOf(stripNumber);
        img = doc.selectFirst("img[src*=" + unpadded + "]");
        if (img != null) {
            return img.attr("src");
        }

        return null;
    }

    private ComicDownloadRequest buildRequest(ComicItem comic, LocalDate date) {
        return ComicDownloadRequest.builder()
                .comicId(comic.getId())
                .comicName(comic.getName())
                .source(SOURCE_IDENTIFIER)
                .sourceIdentifier(comic.getSourceIdentifier())
                .date(date != null ? date : LocalDate.now())
                .build();
    }

    /**
     * Result of parsing a Freefall title tag.
     */
    record TitleParseResult(int stripNumber, LocalDate date) {
    }
}
