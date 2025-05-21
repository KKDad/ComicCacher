package org.stapledon.core.comic.management;

import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.api.dto.comic.ImageDto;
import org.stapledon.common.util.Direction;
import org.stapledon.core.comic.dto.ComicRetrievalRecord;
import org.stapledon.core.comic.dto.ComicRetrievalStatus;
import org.stapledon.events.CacheMissEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Facade for managing comic operations. This is the highest-level facade in the application,
 * acting as the central coordinator between all other facades.
 * It consolidates functionality from ComicsService, UpdateService, and StartupReconciler interfaces.
 */
public interface ComicManagementFacade {
    
    /**
     * Registers a handler for cache miss events.
     * When a comic is not found in the cache, the handler will be notified.
     */
    void registerCacheMissHandler(Consumer<CacheMissEvent> handler);
    
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
     */
    Optional<ImageDto> getComicStrip(int comicId, Direction direction);
    
    /**
     * Gets a comic strip for the specified comic in the given direction,
     * starting from the specified date.
     */
    Optional<ImageDto> getComicStrip(int comicId, Direction direction, LocalDate from);
    
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
     * Reconciles the comic configuration with the bootstrap configuration.
     * This ensures that all comics defined in the bootstrap are available in the comic configuration.
     * Returns true if the reconciliation was successful, false otherwise.
     */
    boolean reconcileWithBootstrap();
    
    /**
     * Schedules a periodic reconciliation task according to the given cron expression.
     */
    void scheduleReconciliation(String cronExpression);
    
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