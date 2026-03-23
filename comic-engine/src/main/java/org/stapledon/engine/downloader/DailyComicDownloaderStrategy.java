package org.stapledon.engine.downloader;

import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;

/**
 * Strategy interface for downloading date-based comics.
 * Implementations handle sources where each strip maps directly to a date URL
 * (e.g., GoComics, ComicsKingdom).
 */
public interface DailyComicDownloaderStrategy extends ComicDownloaderStrategy {

    /**
     * Downloads a comic strip based on the provided request.
     *
     * @param request The download request containing comic details and date
     * @return The download result containing the image data if successful
     */
    ComicDownloadResult downloadComic(ComicDownloadRequest request);
}
