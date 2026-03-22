package org.stapledon.engine.downloader;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.service.ValidationService;

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
     * Validates downloaded image data and returns the validation result.
     * Utility method for subclasses to reuse validation logic.
     *
     * @param imageData The image data to validate
     * @param comicName The comic name (for logging)
     * @param context The context string for logging (e.g., date or strip number)
     * @return The validation result, or null if image data is empty
     */
    protected ImageValidationResult validateImage(byte[] imageData, String comicName, String context) {
        if (imageData == null || imageData.length == 0) {
            log.error("Downloaded image data is empty for {} ({})", comicName, context);
            return null;
        }

        ImageValidationResult validation = imageValidationService.validate(imageData);
        if (!validation.isValid()) {
            log.error("Validation failed for {} ({}): {}", comicName, context, validation.getErrorMessage());
            return null;
        }

        log.debug("Validated {} image for {} ({}): {}x{} ({} bytes)",
                validation.getFormat(), comicName, context,
                validation.getWidth(), validation.getHeight(), validation.getSizeInBytes());

        return validation;
    }

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
