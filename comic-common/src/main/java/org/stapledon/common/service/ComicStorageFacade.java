package org.stapledon.common.service;

import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.ImageDto;

import java.io.File;
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
    boolean saveComicStrip(ComicIdentifier comic, LocalDate date, byte[] imageData);

    boolean saveAvatar(ComicIdentifier comic, byte[] imageData);

    Optional<ImageDto> getComicStrip(ComicIdentifier comic, LocalDate date);

    Optional<ImageDto> getAvatar(ComicIdentifier comic);

    // Navigation operations
    Optional<LocalDate> getNextDateWithComic(ComicIdentifier comic, LocalDate fromDate);

    Optional<LocalDate> getPreviousDateWithComic(ComicIdentifier comic, LocalDate fromDate);

    Optional<LocalDate> getNewestDateWithComic(ComicIdentifier comic);

    Optional<LocalDate> getOldestDateWithComic(ComicIdentifier comic);

    // Management operations
    boolean comicStripExists(ComicIdentifier comic, LocalDate date);

    boolean deleteComic(ComicIdentifier comic);

    boolean purgeOldImages(ComicIdentifier comic, int daysToKeep);

    File getCacheRoot();

    String getComicCacheRoot(ComicIdentifier comic);

    // Cache statistics
    List<String> getYearsWithContent(ComicIdentifier comic);

    long getStorageSize(ComicIdentifier comic);
}