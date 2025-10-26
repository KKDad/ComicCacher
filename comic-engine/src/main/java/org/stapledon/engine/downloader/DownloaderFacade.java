package org.stapledon.engine.downloader;

import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Facade interface for downloading comic images from various sources.
 * This facade unifies the comic downloading operations previously scattered
 * across multiple components, providing a single point of entry for all
 * comic downloading operations.
 */
public interface DownloaderFacade {

    /**
     * Downloads a comic image for the specified request.
     *
     * @param request The download request containing comic details and date
     * @return The download result containing the image data if successful
     */
    ComicDownloadResult downloadComic(ComicDownloadRequest request);

    /**
     * Downloads a comic avatar image for the specified comic.
     *
     * @param comicId The unique identifier of the comic
     * @param comicName The name of the comic
     * @param source The source where the comic can be downloaded from
     * @param sourceIdentifier The URL or identifier used to locate the comic at the source
     * @return The image data if successful, empty otherwise
     */
    Optional<byte[]> downloadAvatar(int comicId, String comicName, String source, String sourceIdentifier);

    /**
     * Downloads the latest comic strip for all comics in the provided configuration.
     *
     * @param config The comic configuration containing all comic information
     * @return List of download results for each comic
     */
    List<ComicDownloadResult> downloadLatestComics(ComicConfig config);

    /**
     * Downloads the comic strips for all comics in the provided configuration for the specified date.
     *
     * @param config The comic configuration containing all comic information
     * @param date The date for which to download comics
     * @return List of download results for each comic
     */
    List<ComicDownloadResult> downloadComicsForDate(ComicConfig config, LocalDate date);

    /**
     * Registers a new comic downloader strategy for a specific source.
     *
     * @param source The source identifier (e.g., "gocomics", "comicskingdom")
     * @param strategy The downloader strategy implementation
     */
    void registerDownloaderStrategy(String source, ComicDownloaderStrategy strategy);
}