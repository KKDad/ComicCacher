package org.stapledon.engine.downloader;

import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ComicItem;

/**
 * Strategy interface for downloading strip-number-based (indexed) comics.
 * Implementations handle sources where strips are identified by sequential numbers
 * and the actual publication date is discovered from the page HTML
 * (e.g., Freefall, XKCD).
 */
public interface IndexedComicDownloaderStrategy extends ComicDownloaderStrategy {

    /**
     * Downloads the latest (most recent) strip for the given comic.
     * The strategy discovers the strip number and actual date from the source.
     *
     * @param comic The comic item to download the latest strip for
     * @return The download result with actualDate and stripNumber set
     */
    ComicDownloadResult downloadLatestStrip(ComicItem comic);

    /**
     * Downloads a specific strip by its number.
     * The strategy discovers the actual date from the page HTML.
     *
     * @param comic The comic item to download
     * @param stripNumber The strip number to download
     * @return The download result with actualDate and stripNumber set
     */
    ComicDownloadResult downloadStrip(ComicItem comic, int stripNumber);
}
