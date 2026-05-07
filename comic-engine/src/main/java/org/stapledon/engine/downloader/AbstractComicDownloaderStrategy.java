package org.stapledon.engine.downloader;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;

import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.infrastructure.web.UserAgentService;
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
    protected final UserAgentService userAgentService;
    protected final SourceThrottleService throttleService;

    /**
     * Creates a new downloader strategy for the specified source.
     */
    protected AbstractComicDownloaderStrategy(String source,
            InspectorService webInspector,
            ValidationService imageValidationService,
            UserAgentService userAgentService,
            SourceThrottleService throttleService) {
        this.source = source;
        this.webInspector = webInspector;
        this.imageValidationService = imageValidationService;
        this.userAgentService = userAgentService;
        this.throttleService = throttleService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<byte[]> downloadAvatar(int comicId, String comicName, String sourceIdentifier) {
        try {
            throttleService.await(source);
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
     */
    protected ImageValidationResult validateImage(byte[] imageData, String comicName, String context) {
        if (imageData == null || imageData.length == 0) {
            log.error("Downloaded image data is empty for {} ({})", comicName, context);
            return null;
        }

        ImageValidationResult validation = imageValidationService.validate(imageData);
        if (!validation.isValid()) {
            log.error("Validation failed for {} ({}): {}", comicName, context, validation.getErrorMessage());
            return validation;
        }

        log.debug("Validated {} image for {} ({}): {}x{} ({} bytes)",
                validation.getFormat(), comicName, context,
                validation.getWidth(), validation.getHeight(), validation.getSizeInBytes());

        return validation;
    }

    /**
     * Downloads binary image data from a URL with proper timeout and User-Agent.
     * All strategies should use this instead of raw {@code URL.openStream()}.
     */
    protected byte[] downloadImageData(String imageUrl) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(imageUrl).toURL().openConnection();
        conn.setRequestProperty("User-Agent", userAgentService.getUserAgent(source));
        conn.setConnectTimeout(DownloaderConstants.DEFAULT_TIMEOUT);
        conn.setReadTimeout(DownloaderConstants.DEFAULT_TIMEOUT);
        try (InputStream in = conn.getInputStream()) {
            return in.readAllBytes();
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Downloads the avatar image for a comic from the source.
     * This method must be implemented by concrete subclasses to handle source-specific logic.
     */
    protected abstract byte[] downloadAvatarImage(int comicId, String comicName, String sourceIdentifier) throws Exception;
}
