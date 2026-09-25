package org.stapledon.engine.downloader;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ComicDownloadResult.FailureKind;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.infrastructure.web.UserAgentService;
import org.stapledon.common.service.ValidationService;

/**
 * Abstract base class for strip-number-based (indexed) comic downloader strategies.
 * Provides the template methods for downloadLatestStrip and downloadStrip that handle
 * validation and error handling.
 */
@Slf4j
@ToString(callSuper = true)
public abstract class AbstractIndexedDownloaderStrategy extends AbstractComicDownloaderStrategy
        implements IndexedComicDownloaderStrategy {

    /**
     * Creates a new indexed downloader strategy for the specified source.
     */
    protected AbstractIndexedDownloaderStrategy(String source,
            InspectorService webInspector,
            ValidationService imageValidationService,
            UserAgentService userAgentService,
            SourceThrottleService throttleService) {
        super(source, webInspector, imageValidationService, userAgentService, throttleService);
    }

    /**
     * Data returned by concrete strategies after fetching an indexed strip.
     * Contains the raw image data and metadata discovered from the page.
     */
    protected record IndexedStripData(byte[] imageData, LocalDate actualDate,
            int stripNumber, String transcript) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComicDownloadResult downloadLatestStrip(ComicItem comic) {
        try {
            throttleService.await(getSource());
            IndexedStripData data = fetchLatestStrip(comic);
            return buildSuccessResult(comic, data);
        } catch (Exception e) {
            return failureFor(comic, "latest strip", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComicDownloadResult downloadStrip(ComicItem comic, int stripNumber) {
        try {
            throttleService.await(getSource());
            IndexedStripData data = fetchStrip(comic, stripNumber);
            return buildSuccessResult(comic, data);
        } catch (Exception e) {
            return failureFor(comic, "strip #" + stripNumber, e);
        }
    }

    /**
     * Turns a failed fetch into a result: a 429 backs the whole source off (no retry here; callers decide), 404/410 means the strip does not
     * exist, any other HTTP status is logged without a stack trace, and anything else is logged with one.
     */
    private ComicDownloadResult failureFor(ComicItem comic, String what, Exception e) {
        ComicDownloadRequest request = buildRequest(comic, LocalDate.now());
        int status = httpStatus(e);
        if (status == RateLimitedException.HTTP_TOO_MANY_REQUESTS) {
            Optional<Duration> retryAfter = e instanceof RateLimitedException rateLimited ? rateLimited.getRetryAfter() : Optional.empty();
            Duration backoff = throttleService.backOff(getSource(), 1, retryAfter);
            String errorMessage = String.format("Rate limited (HTTP 429) downloading %s for %s (source backing off %ds): %s",
                    what, comic.getName(), backoff.toSeconds(), e.getMessage());
            log.warn(errorMessage);
            return ComicDownloadResult.failure(request, errorMessage, FailureKind.RATE_LIMITED, status);
        }
        if (isNotFoundStatus(status)) {
            String errorMessage = String.format("%s for %s not found at source (HTTP %d)", capitalize(what), comic.getName(), status);
            log.warn(errorMessage);
            return ComicDownloadResult.failure(request, errorMessage, FailureKind.UNAVAILABLE, status);
        }
        String errorMessage = String.format("Error downloading %s for %s: %s", what, comic.getName(), e.getMessage());
        if (status > 0) {
            log.warn("{} (HTTP {})", errorMessage, status);
            return ComicDownloadResult.failure(request, errorMessage, FailureKind.ERROR, status);
        }
        log.error(errorMessage, e);
        return ComicDownloadResult.failure(request, errorMessage, FailureKind.ERROR);
    }

    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private ComicDownloadResult buildSuccessResult(ComicItem comic, IndexedStripData data) {
        // Validate image integrity using shared helper
        ImageValidationResult validation = validateImage(data.imageData(),
                comic.getName(), "strip #" + data.stripNumber());
        if (validation == null || !validation.isValid()) {
            String detail = validation != null ? validation.getErrorMessage() : "empty data";
            ComicDownloadRequest request = buildRequest(comic, data.actualDate());
            return ComicDownloadResult.failure(request,
                    String.format("Invalid image for strip #%d: %s", data.stripNumber(), detail), FailureKind.UNAVAILABLE);
        }

        ComicDownloadRequest request = buildRequest(comic, data.actualDate());
        return ComicDownloadResult.successWithMetadata(request, data.imageData(),
                data.actualDate(), data.stripNumber(), data.transcript());
    }

    /**
     * Builds a download request from a comic and date.
     */
    protected ComicDownloadRequest buildRequest(ComicItem comic, LocalDate date) {
        if (date == null) {
            log.warn("actualDate is null for comic '{}', defaulting to today", comic.getName());
        }
        return ComicDownloadRequest.builder()
                .comicId(comic.getId())
                .comicName(comic.getName())
                .source(getSource())
                .sourceIdentifier(comic.getSourceIdentifier())
                .date(date != null ? date : LocalDate.now())
                .build();
    }

    /**
     * Fetches the latest strip from the source.
     * Concrete strategies implement this to handle source-specific page fetching and parsing.
     *
     * @param comic The comic to fetch the latest strip for
     * @return The fetched strip data including image, date, strip number, and transcript
     * @throws Exception If an error occurs during fetching
     */
    protected abstract IndexedStripData fetchLatestStrip(ComicItem comic) throws Exception;

    /**
     * Fetches a specific strip by number from the source.
     * Concrete strategies implement this to handle source-specific page fetching and parsing.
     *
     * @param comic The comic to fetch a strip for
     * @param stripNumber The strip number to fetch
     * @return The fetched strip data including image, date, strip number, and transcript
     * @throws Exception If an error occurs during fetching
     */
    protected abstract IndexedStripData fetchStrip(ComicItem comic, int stripNumber) throws Exception;
}
