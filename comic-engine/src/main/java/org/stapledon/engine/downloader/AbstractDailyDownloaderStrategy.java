package org.stapledon.engine.downloader;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;

import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ComicDownloadResult.FailureKind;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.infrastructure.web.UserAgentService;
import org.stapledon.common.service.ValidationService;

/**
 * Abstract base class for date-based comic downloader strategies.
 * Provides the downloadComic template method that handles throttling, HTTP 429 retries, validation and error handling.
 */
@Slf4j
@ToString(callSuper = true)
public abstract class AbstractDailyDownloaderStrategy extends AbstractComicDownloaderStrategy
        implements DailyComicDownloaderStrategy {

    /**
     * Creates a new daily downloader strategy for the specified source.
     */
    protected AbstractDailyDownloaderStrategy(String source,
            InspectorService webInspector,
            ValidationService imageValidationService,
            UserAgentService userAgentService,
            SourceThrottleService throttleService) {
        super(source, webInspector, imageValidationService, userAgentService, throttleService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComicDownloadResult downloadComic(ComicDownloadRequest request) {
        try {
            byte[] imageData;
            int maxAttempts = throttleService.maxAttempts(getSource());
            for (int attempt = 1; ; attempt++) {
                throttleService.await(getSource());
                log.info("Downloading comic {} for date {} from {}",
                        request.getComicName(), request.getDate(), getSource());
                try {
                    imageData = downloadComicImage(request);
                    break;
                } catch (RateLimitedException e) {
                    if (request.isFailFastOnRateLimit()) {
                        // Still slow the whole source down, but let the caller decide when to try again
                        Duration backoff = throttleService.backOff(getSource(), attempt, e.getRetryAfter());
                        String errorMessage = String.format("Rate limited (HTTP 429) downloading comic %s for date %s; not retrying (source backing off %ds): %s",
                                request.getComicName(), request.getDate(), backoff.toSeconds(), e.getMessage());
                        log.warn(errorMessage);
                        return ComicDownloadResult.failure(request, errorMessage, FailureKind.RATE_LIMITED, RateLimitedException.HTTP_TOO_MANY_REQUESTS);
                    }
                    if (attempt >= maxAttempts) {
                        String errorMessage = String.format("Rate limited (HTTP 429) downloading comic %s for date %s after %d attempt(s): %s",
                                request.getComicName(), request.getDate(), attempt, e.getMessage());
                        log.error(errorMessage);
                        return ComicDownloadResult.failure(request, errorMessage, FailureKind.RATE_LIMITED, RateLimitedException.HTTP_TOO_MANY_REQUESTS);
                    }
                    Duration backoff = throttleService.backOff(getSource(), attempt, e.getRetryAfter());
                    log.warn("Rate limited (HTTP 429) on {} for {} {}, attempt {}/{}; Retry-After={}; backing off {}s",
                            e.getUrl(), request.getComicName(), request.getDate(), attempt, maxAttempts,
                            e.getRetryAfter().map(d -> d.toSeconds() + "s").orElse("none"),
                            backoff.toSeconds());
                }
            }

            if (imageData == null || imageData.length == 0) {
                String errorMessage = String.format("Downloaded image data is empty for %s on %s", request.getComicName(), request.getDate());
                log.warn(errorMessage);
                return ComicDownloadResult.failure(request, errorMessage, FailureKind.UNAVAILABLE);
            }

            // Validate image integrity using shared helper
            ImageValidationResult validation = validateImage(imageData,
                    request.getComicName(), request.getDate().toString());
            if (validation == null || !validation.isValid()) {
                String detail = validation != null ? validation.getErrorMessage() : "unknown";
                return ComicDownloadResult.failure(request,
                        String.format("Invalid image for %s on %s: %s",
                                request.getComicName(), request.getDate(), detail),
                        FailureKind.UNAVAILABLE);
            }

            return ComicDownloadResult.success(request, imageData);
        } catch (Exception e) {
            int status = httpStatus(e);
            if (status == RateLimitedException.HTTP_TOO_MANY_REQUESTS) {
                // A 429 raised as a plain HTTP error (e.g. by Jsoup's get()) rather than a RateLimitedException: back off, don't retry
                Duration backoff = throttleService.backOff(getSource(), 1, Optional.empty());
                String errorMessage = String.format("Rate limited (HTTP 429) downloading comic %s for date %s (source backing off %ds): %s",
                        request.getComicName(), request.getDate(), backoff.toSeconds(), e.getMessage());
                log.warn(errorMessage);
                return ComicDownloadResult.failure(request, errorMessage, FailureKind.RATE_LIMITED, status);
            }
            if (isNotFoundStatus(status)) {
                String errorMessage = String.format("Comic %s for date %s not found at source (HTTP %d)",
                        request.getComicName(), request.getDate(), status);
                log.warn(errorMessage);
                return ComicDownloadResult.failure(request, errorMessage, FailureKind.UNAVAILABLE, status);
            }
            Integer knownStatus = status > 0 ? status : null;
            String errorMessage = String.format("Error downloading comic %s for date %s%s: %s",
                    request.getComicName(), request.getDate(), knownStatus != null ? " (HTTP " + knownStatus + ")" : "", e.getMessage());
            if (knownStatus != null) {
                // The source answered with an error status: the message says everything, a stack trace would not help
                log.warn(errorMessage);
            } else {
                log.error(errorMessage, e);
            }
            return ComicDownloadResult.failure(request, errorMessage, FailureKind.ERROR, knownStatus);
        }
    }

    /**
     * Downloads the comic image from the source.
     * This method must be implemented by concrete subclasses to handle source-specific logic.
     *
     * @param request The download request containing comic details and date
     * @return The downloaded image data
     * @throws Exception If an error occurs during download
     */
    protected abstract byte[] downloadComicImage(ComicDownloadRequest request) throws Exception;
}
