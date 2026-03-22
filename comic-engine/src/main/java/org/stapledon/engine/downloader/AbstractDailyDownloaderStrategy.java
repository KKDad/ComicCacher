package org.stapledon.engine.downloader;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.service.ValidationService;

/**
 * Abstract base class for date-based comic downloader strategies.
 * Provides the downloadComic template method that handles validation and error handling.
 */
@Slf4j
@ToString(callSuper = true)
public abstract class AbstractDailyDownloaderStrategy extends AbstractComicDownloaderStrategy
        implements DailyComicDownloaderStrategy {

    /**
     * Creates a new daily downloader strategy for the specified source.
     *
     * @param source The source identifier (e.g., "gocomics", "comicskingdom")
     * @param webInspector The web inspector to use for HTTP requests
     * @param imageValidationService The service for validating downloaded images
     */
    protected AbstractDailyDownloaderStrategy(String source,
            InspectorService webInspector,
            ValidationService imageValidationService) {
        super(source, webInspector, imageValidationService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComicDownloadResult downloadComic(ComicDownloadRequest request) {
        try {
            log.info("Downloading comic {} for date {} from {}",
                    request.getComicName(), request.getDate(), getSource());

            byte[] imageData = downloadComicImage(request);
            if (imageData == null || imageData.length == 0) {
                return ComicDownloadResult.failure(request, "Downloaded image data is empty");
            }

            // Validate image integrity
            ImageValidationResult validation = imageValidationService.validate(imageData);
            if (!validation.isValid()) {
                String error = String.format("Invalid image downloaded: %s", validation.getErrorMessage());
                log.error("Validation failed for {} on {}: {}",
                        request.getComicName(), request.getDate(), error);
                return ComicDownloadResult.failure(request, error);
            }

            log.debug("Validated {} image for {} on {}: {}x{} ({} bytes)",
                    validation.getFormat(), request.getComicName(), request.getDate(),
                    validation.getWidth(), validation.getHeight(), validation.getSizeInBytes());

            return ComicDownloadResult.success(request, imageData);
        } catch (Exception e) {
            String errorMessage = String.format("Error downloading comic %s for date %s: %s",
                    request.getComicName(), request.getDate(), e.getMessage());
            log.error(errorMessage, e);
            return ComicDownloadResult.failure(request, errorMessage);
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
