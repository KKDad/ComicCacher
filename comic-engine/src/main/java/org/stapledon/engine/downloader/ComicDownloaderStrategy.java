package org.stapledon.engine.downloader;

import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;

import java.util.Optional;

/**
 * Strategy interface for downloading comics from a specific source.
 * Different implementations handle different comic sources like GoComics, ComicsKingdom, etc.
 */
public interface ComicDownloaderStrategy {

    /**
     * Returns the source identifier that this strategy handles.
     *
     * @return The source identifier (e.g., "gocomics", "comicskingdom")
     */
    String getSource();

    /**
     * Downloads a comic strip based on the provided request.
     *
     * @param request The download request containing comic details and date
     * @return The download result containing the image data if successful
     */
    ComicDownloadResult downloadComic(ComicDownloadRequest request);

    /**
     * Downloads the avatar image for a comic.
     *
     * @param comicId The unique identifier of the comic
     * @param comicName The name of the comic
     * @param sourceIdentifier The URL or identifier used to locate the comic at the source
     * @return The avatar image data if successful, empty otherwise
     */
    Optional<byte[]> downloadAvatar(int comicId, String comicName, String sourceIdentifier);
}