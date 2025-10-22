package org.stapledon.engine.downloader;

import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.infrastructure.web.WebInspector;

import java.util.Optional;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for comic downloader strategies.
 * Provides common functionality and error handling for all downloaders.
 */
@Slf4j
public abstract class AbstractComicDownloaderStrategy implements ComicDownloaderStrategy {

    @Getter
    private final String source;
    protected final WebInspector webInspector;

    /**
     * Creates a new downloader strategy for the specified source.
     *
     * @param source The source identifier (e.g., "gocomics", "comicskingdom")
     * @param webInspector The web inspector to use for HTTP requests
     */
    protected AbstractComicDownloaderStrategy(String source, WebInspector webInspector) {
        this.source = source;
        this.webInspector = webInspector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComicDownloadResult downloadComic(ComicDownloadRequest request) {
        try {
            log.debug("Downloading comic {} for date {} from {}", 
                      request.getComicName(), request.getDate(), source);
            
            byte[] imageData = downloadComicImage(request);
            if (imageData == null || imageData.length == 0) {
                return ComicDownloadResult.failure(request, "Downloaded image data is empty");
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
     * {@inheritDoc}
     */
    @Override
    public Optional<byte[]> downloadAvatar(int comicId, String comicName, String sourceIdentifier) {
        try {
            log.debug("Downloading avatar for comic {} from {}", comicName, source);
            return Optional.ofNullable(downloadAvatarImage(comicId, comicName, sourceIdentifier));
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