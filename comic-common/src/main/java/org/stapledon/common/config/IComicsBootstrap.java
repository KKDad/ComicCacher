package org.stapledon.common.config;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

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

    /**
     * Gets the days of the week this comic publishes.
     * If null or empty, the comic publishes daily.
     *
     * @return List of days this comic publishes, or null/empty for daily publication
     */
    default List<DayOfWeek> getPublicationDays() {
        return null; // Default: publish daily
    }

    /**
     * Gets whether this comic is actively publishing new strips.
     * Inactive comics will not have new downloads attempted but remain visible in the UI.
     *
     * @return true if active (default), false if inactive/discontinued
     */
    default Boolean getActive() {
        return true; // Default: active
    }
}