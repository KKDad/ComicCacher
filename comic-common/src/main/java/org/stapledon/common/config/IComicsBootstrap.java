package org.stapledon.common.config;

import java.time.LocalDate;

/**
 * Interface for comic bootstrap configurations.
 * Provides methods to retrieve information needed for comic downloading and processing.
 *
 * Note: IDailyComic is not imported here to avoid circular dependencies.
 * Implementations will use the concrete downloader type.
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
     * Returns Object to avoid importing IDailyComic and creating circular dependencies.
     *
     * @return The downloader implementation (will be cast to IDailyComic by caller)
     */
    Object getDownloader();

    /**
     * Gets the source identifier for the comic (e.g., "gocomics", "comicskingdom").
     *
     * @return The source identifier
     */
    String getSource();

    /**
     * Gets the source-specific identifier for the comic (e.g., "calvinandhobbes", "beetle-bailey").
     *
     * @return The source-specific identifier
     */
    String getSourceIdentifier();
}