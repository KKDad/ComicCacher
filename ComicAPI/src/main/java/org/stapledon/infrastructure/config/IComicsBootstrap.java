package org.stapledon.infrastructure.config;

import org.stapledon.core.comic.downloader.IDailyComic;

import java.time.LocalDate;

/**
 * Interface for comic bootstrap configurations.
 * Provides methods to retrieve information needed for comic downloading and processing.
 */
public interface IComicsBootstrap {
    /**
     * Gets the name of the comic strip.
     *
     * @return The name of the comic strip
     */
    String stripName();

    /**
     * Gets the start date from which to download comics.
     *
     * @return The start date
     */
    LocalDate startDate();

    /**
     * Gets the downloader for this comic.
     *
     * @return The downloader implementation
     */
    IDailyComic getDownloader();

    /**
     * Gets the source identifier for the comic (e.g., "gocomics", "comicskingdom").
     * Default implementation returns the source based on the downloader class.
     *
     * @return The source identifier
     */
    default String getSource() {
        IDailyComic downloader = getDownloader();
        if (downloader instanceof org.stapledon.core.comic.downloader.GoComics) {
            return "gocomics";
        } else if (downloader instanceof org.stapledon.core.comic.downloader.ComicsKingdom) {
            return "comicskingdom";
        } else {
            return "unknown";
        }
    }

    /**
     * Gets the source-specific identifier for the comic (e.g., "calvinandhobbes", "beetle-bailey").
     * Default implementation returns the strip name with spaces removed for GoComics
     * or with spaces replaced by hyphens for ComicsKingdom.
     *
     * @return The source-specific identifier
     */
    default String getSourceIdentifier() {
        String source = getSource();
        if ("gocomics".equals(source)) {
            return stripName().replace(" ", "").toLowerCase();
        } else if ("comicskingdom".equals(source)) {
            return stripName().replace(" ", "-").toLowerCase();
        } else {
            return stripName().toLowerCase();
        }
    }
}