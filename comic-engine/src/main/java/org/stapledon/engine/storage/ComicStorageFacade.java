package org.stapledon.engine.storage;

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
    boolean saveComicStrip(int comicId, String comicName, LocalDate date, byte[] imageData);
    boolean saveAvatar(int comicId, String comicName, byte[] imageData);
    Optional<ImageDto> getComicStrip(int comicId, String comicName, LocalDate date);
    Optional<ImageDto> getAvatar(int comicId, String comicName);
    
    // Navigation operations
    Optional<LocalDate> getNextDateWithComic(int comicId, String comicName, LocalDate fromDate);
    Optional<LocalDate> getPreviousDateWithComic(int comicId, String comicName, LocalDate fromDate);
    Optional<LocalDate> getNewestDateWithComic(int comicId, String comicName);
    Optional<LocalDate> getOldestDateWithComic(int comicId, String comicName);
    
    // Management operations
    boolean comicStripExists(int comicId, String comicName, LocalDate date);
    boolean deleteComic(int comicId, String comicName);
    boolean purgeOldImages(int comicId, String comicName, int daysToKeep);
    File getCacheRoot();
    String getComicCacheRoot(int comicId, String comicName);
    
    // Cache statistics
    List<String> getYearsWithContent(int comicId, String comicName);
    long getStorageSize(int comicId, String comicName);
}