package org.stapledon.engine.management;

import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.util.Direction;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Facade for managing comic operations. This is the highest-level facade in the application,
 * acting as the central coordinator between all other facades.
 * It consolidates functionality from ComicsService, UpdateService, and StartupReconciler interfaces.
 */
public interface ManagementFacade {

    /**
     * Gets a list of all available comics.
     */
    List<ComicItem> getAllComics();
    
    /**
     * Gets a specific comic by its ID.
     */
    Optional<ComicItem> getComic(int comicId);
    
    /**
     * Gets a specific comic by its name.
     */
    Optional<ComicItem> getComicByName(String comicName);
    
    /**
     * Creates a new comic with the provided details.
     * Returns the created comic if successful, empty otherwise.
     */
    Optional<ComicItem> createComic(ComicItem comicItem);
    
    /**
     * Updates an existing comic with the provided details.
     * Returns the updated comic if successful, empty otherwise.
     */
    Optional<ComicItem> updateComic(int comicId, ComicItem comicItem);
    
    /**
     * Deletes a comic by its ID.
     * Returns true if the comic was successfully deleted, false otherwise.
     */
    boolean deleteComic(int comicId);
    
    /**
     * Gets a comic strip for the specified comic in the given direction.
     * Uses the most recent date available if no specific date is provided.
     * Returns a ComicNavigationResult with image data if found, or boundary information if not.
     */
    ComicNavigationResult getComicStrip(int comicId, Direction direction);

    /**
     * Gets a comic strip for the specified comic in the given direction,
     * starting from the specified date.
     * Returns a ComicNavigationResult with image data if found, or boundary information if not.
     */
    ComicNavigationResult getComicStrip(int comicId, Direction direction, LocalDate from);
    
    /**
     * Gets a comic strip for the specified comic on the exact date requested.
     */
    Optional<ImageDto> getComicStripOnDate(int comicId, LocalDate date);
    
    /**
     * Gets a comic strip for the specified comic by name on the exact date requested.
     */
    Optional<ImageDto> getComicStripOnDate(String comicName, LocalDate date);
    
    /**
     * Gets the avatar image for the specified comic.
     */
    Optional<ImageDto> getAvatar(int comicId);
    
    /**
     * Gets the avatar image for the specified comic by name.
     */
    Optional<ImageDto> getAvatar(String comicName);
    
    /**
     * Updates all comics by downloading the latest strips.
     * Returns true if all comics were successfully updated, false otherwise.
     */
    boolean updateAllComics();

    /**
     * Updates all comics by downloading strips for the specified date.
     * Downloads and saves each comic strip to disk.
     *
     * @param date The date for which to download comics
     * @return List of download results for each comic
     */
    List<org.stapledon.common.dto.ComicDownloadResult> updateComicsForDate(LocalDate date);

    /**
     * Updates a specific comic by downloading its latest strip.
     * Returns true if the comic was successfully updated, false otherwise.
     */
    boolean updateComic(int comicId);

    /**
     * Updates a specific comic by downloading its latest strip.
     * Returns true if the comic was successfully updated, false otherwise.
     */
    boolean updateComic(String comicName);

    /**
     * Refreshes the comic list from storage and configuration.
     * This ensures that the in-memory comic list matches the persisted state.
     */
    void refreshComicList();
    
    /**
     * Purges old comic images that are older than the specified number of days.
     * Returns true if the purge was successful, false otherwise.
     */
    boolean purgeOldImages(int daysToKeep);
    
    /**
     * Gets the newest date with a comic strip for the specified comic.
     */
    Optional<LocalDate> getNewestDateWithComic(int comicId);
    
    /**
     * Gets the oldest date with a comic strip for the specified comic.
     */
    Optional<LocalDate> getOldestDateWithComic(int comicId);
    
    /**
     * Gets retrieval records for a specific comic
     */
    List<ComicRetrievalRecord> getRetrievalRecords(String comicName, int limit);
    
    /**
     * Gets retrieval records for a specific comic on a specific date
     */
    List<ComicRetrievalRecord> getRetrievalRecordsForDate(String comicName, LocalDate date);
    
    /**
     * Gets filtered retrieval records
     */
    List<ComicRetrievalRecord> getFilteredRetrievalRecords(
            String comicName, 
            ComicRetrievalStatus status, 
            LocalDate fromDate, 
            LocalDate toDate, 
            int limit);
    
    /**
     * Gets retrieval summary statistics
     */
    Map<String, Object> getRetrievalSummary(LocalDate fromDate, LocalDate toDate);
    
    /**
     * Purges old retrieval records
     */
    int purgeOldRetrievalRecords(int daysToKeep);
}