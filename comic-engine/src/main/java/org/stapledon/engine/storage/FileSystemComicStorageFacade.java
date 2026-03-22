package org.stapledon.engine.storage;

import com.google.common.io.Files;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.DuplicateValidationResult;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.dto.ImageMetadata;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.dto.SaveResult;
import org.stapledon.common.service.AnalysisService;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.common.service.DuplicateValidationService;
import org.stapledon.common.service.ValidationService;
import org.stapledon.common.util.ImageUtils;
import org.stapledon.engine.validation.DuplicateHashCacheService;

/**
 * Implementation of the Comic Storage Facade that abstracts all filesystem
 * operations related to comic storage.
 */
@Slf4j
@ToString
@Component
@RequiredArgsConstructor
public class FileSystemComicStorageFacade implements ComicStorageFacade {

    private static final String COMBINE_PATH = "%s/%s";
    private static final String AVATAR_FILE = "avatar.png";
    private static final int MIN_COMIC_WIDTH = 100;
    private static final int MIN_COMIC_HEIGHT = 50;
    private static final String EXCLUDED_SYNOLOGY_DIR = "@eaDir";

    private final CacheProperties cacheProperties;
    private final ValidationService imageValidationService;
    private final DuplicateValidationService duplicateValidationService;
    private final DuplicateHashCacheService duplicateHashCacheService;
    private final AnalysisService imageAnalysisService;
    private final ImageMetadataRepository imageMetadataRepository;
    private final ComicIndexService comicIndexService;

    @Override
    public SaveResult saveComicStripWithResult(@lombok.NonNull ComicIdentifier comic,
                                               @lombok.NonNull LocalDate date,
                                               @lombok.NonNull byte[] imageData) {
        return saveComicStripWithResult(comic, date, imageData, null);
    }

    @Override
    public SaveResult saveComicStripWithResult(@lombok.NonNull ComicIdentifier comic,
                                               @lombok.NonNull LocalDate date,
                                               @lombok.NonNull byte[] imageData,
                                               String transcript) {

        // Validate image before saving
        ImageValidationResult validation = imageValidationService.validateWithMinDimensions(
                imageData, MIN_COMIC_WIDTH, MIN_COMIC_HEIGHT);

        if (!validation.isValid()) {
            log.error("Refusing to save invalid comic strip for {} on {}: {}",
                    comic.getName(), date, validation.getErrorMessage());
            return SaveResult.validationFailed(validation.getErrorMessage());
        }

        log.info("Saving validated {} image for {} on {}: {}x{} ({} bytes)",
                validation.getFormat(), comic.getName(), date,
                validation.getWidth(), validation.getHeight(), imageData.length);

        // Check for duplicates
        DuplicateValidationResult duplicateCheck = duplicateValidationService.validateNoDuplicate(
                comic.getId(), comic.getName(), date, imageData);

        if (duplicateCheck.isDuplicate()) {
            log.warn("Skipping duplicate image for {} on {}, duplicate of {} (hash: {})",
                    comic.getName(), date, duplicateCheck.getDuplicateDate(), duplicateCheck.getHash());
            return SaveResult.duplicateSkipped(duplicateCheck.getDuplicateDate());
        }

        // Create directory structure if it doesn't exist
        String yearPath = date.format(DateTimeFormatter.ofPattern("yyyy"));
        File directory = new File(
                String.format("%s/%s/%s", getCacheRoot().toAbsolutePath(), comic.getDirectoryName(),
                        yearPath));
        if (!directory.exists() && !directory.mkdirs()) {
            log.error("Failed to create directory: {}", directory.getAbsolutePath());
            return SaveResult.ioError("Failed to create directory: " + directory.getAbsolutePath());
        }

        // Create the file
        String filename = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        File file = new File(String.format("%s/%s.png", directory.getAbsolutePath(), filename));

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(imageData);
            log.info("Saved comic strip to: {}", file.getAbsolutePath());

            // Add to hash cache after successful save (non-critical)
            try {
                duplicateHashCacheService.addImageToCache(comic.getId(), comic.getName(), date, imageData,
                        file.getAbsolutePath());
            } catch (Exception e) {
                log.warn("Failed to update hash cache (non-critical): {}", e.getMessage());
            }

            // Add to the persistent date index - CRITICAL operation
            log.info("Calling comicIndexService.addDateToIndex for comic {} (id={}) on date {}",
                     comic.getName(), comic.getId(), date);
            try {
                comicIndexService.addDateToIndex(comic.getId(), comic.getName(), date);
            } catch (Exception e) {
                // Index update is CRITICAL - if it fails, the file becomes an orphan
                // We should delete the file to maintain consistency
                log.error("Index update failed for {} on {}, rolling back file write", comic.getName(), date, e);
                if (!file.delete()) {
                    log.error("CRITICAL: Failed to delete orphan file: {}", file.getAbsolutePath());
                }
                return SaveResult.ioError("Index update failed: " + e.getMessage());
            }

            // After successfully saving the image, analyze and save metadata (non-critical)
            try {
                ImageMetadata metadata = imageAnalysisService.analyzeImage(comic.getId(), comic.getName(), imageData,
                        file.getAbsolutePath(), validation, null, transcript);
                boolean saved = imageMetadataRepository.saveMetadata(metadata);
                if (saved) {
                    log.debug("Saved metadata for comic strip: {}", file.getAbsolutePath());
                } else {
                    log.error(
                            "Failed to save metadata for comic strip {} on {}: metadata validation failed or incomplete",
                            comic.getName(), date);
                }
            } catch (Exception e) {
                // Log but don't fail the save operation if metadata capture fails (non-critical)
                log.warn("Failed to save metadata (non-critical): {}", e.getMessage());
            }

            return SaveResult.saved();
        } catch (IOException e) {
            log.error("Failed to write comic strip file for {} on {}: {}", comic.getName(), date, e.getMessage());
            return SaveResult.ioError("File write failed: " + e.getMessage());
        }
    }

    @Override
    public boolean saveAvatar(@lombok.NonNull ComicIdentifier comic, @lombok.NonNull byte[] imageData) {

        // Validate avatar image
        ImageValidationResult validation = imageValidationService.validate(imageData);
        if (!validation.isValid()) {
            log.error("Refusing to save invalid avatar for {}: {}",
                    comic.getName(), validation.getErrorMessage());
            return false;
        }

        log.debug("Saving validated {} avatar for {}: {}x{}",
                validation.getFormat(), comic.getName(),
                validation.getWidth(), validation.getHeight());

        // Create directory structure if it doesn't exist
        File directory = new File(String.format(COMBINE_PATH, getCacheRoot().toAbsolutePath(),
                comic.getDirectoryName()));
        if (!directory.exists() && !directory.mkdirs()) {
            log.error("Failed to create directory: {}", directory.getAbsolutePath());
            return false;
        }

        // Create the file
        File file = new File(String.format(COMBINE_PATH, directory.getAbsolutePath(), AVATAR_FILE));

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(imageData);
            return true;
        } catch (IOException e) {
            log.error("Failed to save avatar for {}: {}", comic.getName(), e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<ImageDto> getComicStrip(@lombok.NonNull ComicIdentifier comic, @lombok.NonNull LocalDate date) {

        // Check if the image exists in cache
        if (!comicStripExists(comic, date)) {
            return Optional.empty();
        }

        String yearPath = date.format(DateTimeFormatter.ofPattern("yyyy"));
        String filename = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        File file = new File(String.format("%s/%s/%s/%s.png", getCacheRoot().toAbsolutePath(),
                comic.getDirectoryName(), yearPath, filename));

        try {
            return Optional.of(ImageUtils.getImageDto(file));
        } catch (IOException e) {
            log.error("Failed to read comic strip for {} on {}: {}", comic.getName(), date, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<ImageDto> getAvatar(@lombok.NonNull ComicIdentifier comic) {

        File file = new File(String.format("%s/%s/%s", getCacheRoot().toAbsolutePath(),
                comic.getDirectoryName(), AVATAR_FILE));

        if (!file.exists()) {
            log.error("Avatar not found for {}", comic.getName());
            return Optional.empty();
        }

        try {
            return Optional.of(ImageUtils.getImageDto(file));
        } catch (IOException e) {
            log.error("Failed to read avatar for {}: {}", comic.getName(), e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<LocalDate> getNextDateWithComic(@lombok.NonNull ComicIdentifier comic,
                                                     @lombok.NonNull LocalDate fromDate) {
        return comicIndexService.getNextDate(comic.getId(), comic.getName(), fromDate);
    }

    @Override
    public Optional<LocalDate> getPreviousDateWithComic(@lombok.NonNull ComicIdentifier comic,
                                                         @lombok.NonNull LocalDate fromDate) {
        return comicIndexService.getPreviousDate(comic.getId(), comic.getName(), fromDate);
    }

    @Override
    public Optional<LocalDate> getNewestDateWithComic(@lombok.NonNull ComicIdentifier comic) {
        return comicIndexService.getNewestDate(comic.getId(), comic.getName());
    }

    @Override
    public Optional<LocalDate> getOldestDateWithComic(@lombok.NonNull ComicIdentifier comic) {
        return comicIndexService.getOldestDate(comic.getId(), comic.getName());
    }

    @Override
    public List<LocalDate> getAvailableDates(@lombok.NonNull ComicIdentifier comic) {
        return comicIndexService.getAvailableDates(comic.getId(), comic.getName());
    }

    @Override
    public boolean comicStripExists(@lombok.NonNull ComicIdentifier comic, @lombok.NonNull LocalDate date) {

        String yearPath = date.format(DateTimeFormatter.ofPattern("yyyy"));
        String filename = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        File file = new File(String.format("%s/%s/%s/%s.png", getCacheRoot().toAbsolutePath(),
                comic.getDirectoryName(), yearPath, filename));

        return file.exists();
    }

    @Override
    public boolean deleteComic(@lombok.NonNull ComicIdentifier comic) {

        File directory = new File(String.format(COMBINE_PATH, getCacheRoot().toAbsolutePath(),
                comic.getDirectoryName()));

        if (!directory.exists()) {
            log.warn("Comic directory does not exist for {}", comic.getName());
            return false;
        }

        boolean deleted = deleteDirectory(directory);
        if (deleted) {
            // Invalidate the in-memory index cache
            comicIndexService.invalidateCache(comic.getId());
        }
        return deleted;
    }

    private boolean deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        log.error("Failed to delete file: {}", file.getAbsolutePath());
                    }
                }
            }
        }

        return directory.delete();
    }

    /**
     * Purges comic strip images older than the specified number of days.
     * NOTE: This method is currently not called by any scheduled job or API
     * endpoint. The application is configured to retain all historical comic strips
     * indefinitely. This implementation is preserved for potential future use if
     * retention policies change.
     */
    @Override
    public boolean purgeOldImages(@lombok.NonNull ComicIdentifier comic, int daysToKeep) {

        LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);
        File comicRoot = new File(String.format(COMBINE_PATH, getCacheRoot().toAbsolutePath(),
                comic.getDirectoryName()));

        if (!comicRoot.exists()) {
            log.warn("Comic directory does not exist for {}", comic.getName());
            return false;
        }

        // Get all year directories
        File[] yearDirs = comicRoot.listFiles(File::isDirectory);
        if (yearDirs == null) {
            return true;
        }

        boolean success = true;

        for (File yearDir : yearDirs) {
            if (EXCLUDED_SYNOLOGY_DIR.equals(yearDir.getName())) {
                continue;
            }

            File[] comicFiles = yearDir.listFiles(file -> file.isFile() && file.getName().endsWith(".png"));
            if (comicFiles == null) {
                continue;
            }

            for (File comicFile : comicFiles) {
                try {
                    String filename = Files.getNameWithoutExtension(comicFile.getName());
                    LocalDate comicDate = LocalDate.parse(filename, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                    if (comicDate.isBefore(cutoffDate)) {
                        if (comicFile.delete()) {
                            // Update the index to remove the deleted date
                            comicIndexService.removeDateFromIndex(comic.getId(), comic.getName(), comicDate);
                        } else {
                            log.error("Failed to delete old comic file: {}", comicFile.getAbsolutePath());
                            success = false;
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to parse date from filename: {}", comicFile.getName());
                }
            }

            // Delete year directory if it's empty
            File[] remainingFiles = yearDir.listFiles();
            if (remainingFiles != null && remainingFiles.length == 0) {
                if (!yearDir.delete()) {
                    log.error("Failed to delete empty year directory: {}", yearDir.getAbsolutePath());
                    success = false;
                }
            }
        }

        return success;
    }

    @Override
    public Path getCacheRoot() {
        return Path.of(cacheProperties.getLocation());
    }

    @Override
    public String getComicCacheRoot(@lombok.NonNull ComicIdentifier comic) {
        return String.format(COMBINE_PATH, getCacheRoot().toAbsolutePath(), comic.getDirectoryName());
    }

    @Override
    public List<String> getYearsWithContent(@lombok.NonNull ComicIdentifier comic) {

        File comicRoot = new File(String.format(COMBINE_PATH, getCacheRoot().toAbsolutePath(),
                comic.getDirectoryName()));

        if (!comicRoot.exists()) {
            return new ArrayList<>();
        }

        File[] yearDirs = comicRoot
                .listFiles(file -> file.isDirectory() && !EXCLUDED_SYNOLOGY_DIR.equals(file.getName()));
        if (yearDirs == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(yearDirs)
                .map(File::getName)
                .sorted()
                .toList();
    }

    @Override
    public long getStorageSize(@lombok.NonNull ComicIdentifier comic) {

        File comicRoot = new File(String.format(COMBINE_PATH, getCacheRoot().toAbsolutePath(),
                comic.getDirectoryName()));

        if (!comicRoot.exists()) {
            return 0;
        }

        return calculateDirectorySize(comicRoot);
    }

    private long calculateDirectorySize(File directory) {
        long size = 0;

        File[] files = directory.listFiles();
        if (files == null) {
            return 0;
        }

        for (File file : files) {
            if (file.isFile()) {
                size += file.length();
            } else if (file.isDirectory() && !EXCLUDED_SYNOLOGY_DIR.equals(file.getName())) {
                size += calculateDirectorySize(file);
            }
        }

        return size;
    }
}
