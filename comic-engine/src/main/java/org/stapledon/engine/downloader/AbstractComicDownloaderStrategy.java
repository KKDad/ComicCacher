package org.stapledon.engine.downloader;

import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.service.ValidationService;

import java.util.Optional;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for comic downloader strategies.
 * Provides common functionality and error handling for all downloaders.
 */
@Slf4j
@ToString
public abstract class AbstractComicDownloaderStrategy implements ComicDownloaderStrategy {

    @Getter
    private final String source;
    protected final InspectorService webInspector;
    protected final ValidationService imageValidationService;

    /**
     * Creates a new downloader strategy for the specified source.
     *
     * @param source The source identifier (e.g., "gocomics", "comicskingdom")
     * @param webInspector The web inspector to use for HTTP requests
     * @param imageValidationService The service for validating downloaded images
     */
    protected AbstractComicDownloaderStrategy(String source,
                                             InspectorService webInspector,
                                             ValidationService imageValidationService) {
        this.source = source;
        this.webInspector = webInspector;
        this.imageValidationService = imageValidationService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComicDownloadResult downloadComic(ComicDownloadRequest request) {
        try {
            log.info("Downloading comic {} for date {} from {}",
                      request.getComicName(), request.getDate(), source);

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
     * {@inheritDoc}
     */
    @Override
    public Optional<byte[]> downloadAvatar(int comicId, String comicName, String sourceIdentifier) {
        try {
            log.debug("Downloading avatar for comic {} from {}", comicName, source);
            byte[] avatarData = downloadAvatarImage(comicId, comicName, sourceIdentifier);

            if (avatarData == null || avatarData.length == 0) {
                return Optional.empty();
            }

            // Validate avatar image
            ImageValidationResult validation = imageValidationService.validate(avatarData);
            if (!validation.isValid()) {
                log.warn("Invalid avatar image for {}: {}", comicName, validation.getErrorMessage());
                return Optional.empty();
            }

            log.debug("Validated {} avatar for {}: {}x{}",
                     validation.getFormat(), comicName,
                     validation.getWidth(), validation.getHeight());

            return Optional.of(avatarData);
        } catch (Exception e) {
            log.error("Error downloading avatar for comic {}: {}", comicName, e.getMessage(), e);
            return Optional.empty();
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

    /**
     * Downloads the avatar image for a comic from the source.
     * This method must be implemented by concrete subclasses to handle source-specific logic.
     *
     * @param comicId The unique identifier of the comic
     * @param comicName The name of the comic
     * @param sourceIdentifier The URL or identifier used to locate the comic at the source
     * @return The avatar image data, or null if not available
     * @throws Exception If an error occurs during download
     */
    protected abstract byte[] downloadAvatarImage(int comicId, String comicName, String sourceIdentifier) throws Exception;
}