package org.stapledon.common.service;

import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.dto.SaveResult;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Facade interface for all comic storage related operations.
 * This facade abstracts all filesystem operations related to comic storage,
 * creating a clean separation between storage concerns and business logic.
 */
public interface ComicStorageFacade {
    // Basic file operations

    /**
     * Saves a comic strip image with detailed result information.
     * <p>
     * This method provides comprehensive feedback about the save operation including
     * validation failures, duplicate detection, and I/O errors.
     * </p>
     *
     * @param comic The comic identifier
     * @param date The publication date
     * @param imageData The image data to save
     * @return A SaveResult containing the outcome and any relevant details
     */
    SaveResult saveComicStripWithResult(ComicIdentifier comic, LocalDate date, byte[] imageData);

    /**
     * Saves a comic strip image with transcript text.
     * <p>
     * This overload passes transcript text through to the metadata pipeline.
     * </p>
     *
     * @param comic The comic identifier
     * @param date The publication date
     * @param imageData The image data to save
     * @param transcript The transcript text extracted from the comic page (nullable)
     * @return A SaveResult containing the outcome and any relevant details
     */
    default SaveResult saveComicStripWithResult(ComicIdentifier comic, LocalDate date, byte[] imageData,
                                                 String transcript) {
        return saveComicStripWithResult(comic, date, imageData);
    }

    /**
     * Saves a comic strip image (legacy method).
     * <p>
     * This method provides backward compatibility by wrapping the new saveComicStripWithResult method.
     * Consider using saveComicStripWithResult for more detailed outcome information.
     * </p>
     *
     * @param comic The comic identifier
     * @param date The publication date
     * @param imageData The image data to save
     * @return true if the operation was successful (saved or duplicate properly detected), false otherwise
     */
    default boolean saveComicStrip(ComicIdentifier comic, LocalDate date, byte[] imageData) {
        return saveComicStripWithResult(comic, date, imageData).isSuccess();
    }

    boolean saveAvatar(ComicIdentifier comic, byte[] imageData);

    Optional<ImageDto> getComicStrip(ComicIdentifier comic, LocalDate date);

    Optional<ImageDto> getAvatar(ComicIdentifier comic);

    // Navigation operations
    Optional<LocalDate> getNextDateWithComic(ComicIdentifier comic, LocalDate fromDate);

    Optional<LocalDate> getPreviousDateWithComic(ComicIdentifier comic, LocalDate fromDate);

    Optional<LocalDate> getNewestDateWithComic(ComicIdentifier comic);

    Optional<LocalDate> getOldestDateWithComic(ComicIdentifier comic);

    // Date index operations

    /**
     * Gets all available dates for a comic, sorted in ascending order.
     */
    List<LocalDate> getAvailableDates(ComicIdentifier comic);

    // Management operations
    boolean comicStripExists(ComicIdentifier comic, LocalDate date);

    boolean deleteComic(ComicIdentifier comic);

    boolean purgeOldImages(ComicIdentifier comic, int daysToKeep);

    Path getCacheRoot();

    String getComicCacheRoot(ComicIdentifier comic);

    // Cache statistics
    List<String> getYearsWithContent(ComicIdentifier comic);

    long getStorageSize(ComicIdentifier comic);
}