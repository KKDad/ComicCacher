package org.stapledon.engine.downloader;

import java.util.Optional;

/**
 * Base strategy interface for downloading comics from a specific source.
 * Sub-interfaces define the download method signature appropriate for each comic type.
 *
 * @see DailyComicDownloaderStrategy for date-based comics (GoComics, ComicsKingdom)
 * @see IndexedComicDownloaderStrategy for strip-number-based comics (Freefall, XKCD)
 */
public interface ComicDownloaderStrategy {

    /**
     * Returns the source identifier that this strategy handles.
     *
     * @return The source identifier (e.g., "gocomics", "comicskingdom", "freefall")
     */
    String getSource();

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