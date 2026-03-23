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
                return ComicDownloadResult.failure(request,
                        String.format("Downloaded image data is empty for %s on %s",
                                request.getComicName(), request.getDate()));
            }

            // Validate image integrity using shared helper
            ImageValidationResult validation = validateImage(imageData,
                    request.getComicName(), request.getDate().toString());
            if (validation == null || !validation.isValid()) {
                String detail = validation != null ? validation.getErrorMessage() : "unknown";
                return ComicDownloadResult.failure(request,
                        String.format("Invalid image for %s on %s: %s",
                                request.getComicName(), request.getDate(), detail));
            }

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
