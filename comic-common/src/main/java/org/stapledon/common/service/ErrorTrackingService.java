package org.stapledon.common.service;

import org.stapledon.common.dto.ComicErrorRecord;

import java.util.List;

/**
 * Service interface for tracking comic retrieval errors.
 * Maintains a rolling history of recent errors per comic for troubleshooting.
 */
public interface ErrorTrackingService {
    /**
     * Records an error for a specific comic.
     * Maintains only the last N errors per comic (configurable, default 5).
     *
     * @param error The error record to save
     */
    void recordError(ComicErrorRecord error);

    /**
     * Clears all error records for a specific comic.
     * Called when a comic is successfully retrieved.
     *
     * @param comicName The name of the comic
     */
    void clearErrors(String comicName);

    /**
     * Gets all error records for a specific comic.
     *
     * @param comicName The name of the comic
     * @return List of error records for the comic, ordered by timestamp (newest first)
     */
    List<ComicErrorRecord> getErrors(String comicName);

    /**
     * Gets all error records across all comics.
     *
     * @return Map of comic name to list of error records
     */
    java.util.Map<String, List<ComicErrorRecord>> getAllErrors();

    /**
     * Gets the count of comics that have errors.
     *
     * @return Number of comics with at least one error
     */
    int getComicErrorCount();

    /**
     * Clears errors older than the specified number of hours.
     * This prevents error accumulation over multiple days/weeks.
     *
     * @param hoursToKeep Number of hours of errors to retain (e.g., 48 for last 2 days)
     */
    void clearOldErrors(int hoursToKeep);
}
