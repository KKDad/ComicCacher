package org.stapledon.engine.validation;

import org.springframework.stereotype.Service;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.HashAlgorithm;
import org.stapledon.common.dto.ImageHashRecord;
import org.stapledon.common.service.ImageHasher;
import org.stapledon.engine.storage.DuplicateImageHashRepository;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing duplicate hash cache, including backfilling existing images
 * and rebuilding cache when hash algorithm changes.
 */
@Slf4j
@ToString
@Service
@RequiredArgsConstructor
public class DuplicateHashCacheService {

    private final DuplicateImageHashRepository hashRepository;
    private final ImageHasherFactory imageHasherFactory;
    private final CacheProperties cacheProperties;

    /**
     * Loads hashes for a comic/year, with automatic backfill and algorithm change detection.
     *
     * @param comicId The comic ID
     * @param comicName The comic name
     * @param year The year
     * @return Map of hash value to ImageHashRecord
     */
    public Map<String, ImageHashRecord> loadHashesWithBackfill(int comicId, String comicName, int year) {
        Map<String, ImageHashRecord> hashes = hashRepository.loadHashes(comicId, comicName, year);

        // Check if cache needs rebuilding (algorithm changed or empty)
        boolean needsRebuild = false;
        String rebuildReason = null;

        if (hashes.isEmpty()) {
            // Check if there are existing images to backfill
            File yearDir = hashRepository.getYearDirectory(comicId, comicName, year);
            if (yearDir.exists() && yearDir.isDirectory()) {
                File[] imageFiles = yearDir.listFiles((dir, name) -> name.endsWith(".png"));
                if (imageFiles != null && imageFiles.length > 0) {
                    needsRebuild = true;
                    rebuildReason = "empty cache with existing images";
                    log.info("No existing hash file for comic {} year {}, found {} images to backfill",
                            comicName, year, imageFiles.length);
                }
            }
        } else {
            // Check if algorithm changed
            HashAlgorithm currentAlgorithm = cacheProperties.getHashAlgorithm();
            Optional<HashAlgorithm> existingAlgorithm = hashes.values().stream()
                    .map(ImageHashRecord::getAlgorithm)
                    .filter(Objects::nonNull)
                    .findFirst();

            if (existingAlgorithm.isPresent() && !existingAlgorithm.get().equals(currentAlgorithm)) {
                needsRebuild = true;
                rebuildReason = String.format("algorithm changed from %s to %s",
                        existingAlgorithm.get(), currentAlgorithm);
                log.warn("Hash algorithm changed from {} to {} for {} year {}, rebuilding cache",
                        existingAlgorithm.get(), currentAlgorithm, comicName, year);
            }
        }

        if (needsRebuild) {
            log.info("Rebuilding hash cache for {} year {}: {}", comicName, year, rebuildReason);
            hashes = backfillExistingImages(comicId, comicName, year);
            hashRepository.replaceHashes(comicId, comicName, year, hashes);
        }

        return hashes;
    }

    /**
     * Backfills hash records from existing image files in the directory.
     *
     * @param comicId The comic ID
     * @param comicName The comic name
     * @param year The year
     * @return Map of hash value to ImageHashRecord
     */
    private Map<String, ImageHashRecord> backfillExistingImages(int comicId, String comicName, int year) {
        Map<String, ImageHashRecord> hashes = new ConcurrentHashMap<>();
        File yearDir = hashRepository.getYearDirectory(comicId, comicName, year);

        if (!yearDir.exists() || !yearDir.isDirectory()) {
            log.debug("No year directory exists for {} year {}", comicName, year);
            return hashes;
        }

        File[] imageFiles = yearDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (imageFiles == null || imageFiles.length == 0) {
            log.debug("No image files found in {} for backfill", yearDir.getAbsolutePath());
            return hashes;
        }

        log.info("Backfilling {} existing images for {} year {}", imageFiles.length, comicName, year);

        ImageHasher hasher = imageHasherFactory.getImageHasher();
        int backfilled = 0;
        int failed = 0;

        for (File imageFile : imageFiles) {
            try {
                // Extract date from filename (yyyy-MM-dd.png)
                String filename = imageFile.getName();
                String dateStr = filename.substring(0, filename.lastIndexOf('.'));
                LocalDate date = LocalDate.parse(dateStr);

                // Read image and calculate hash
                byte[] imageData = Files.readAllBytes(imageFile.toPath());
                String hash = hasher.calculateHash(imageData);

                if (hash != null) {
                    ImageHashRecord record = ImageHashRecord.builder()
                            .hash(hash)
                            .date(date)
                            .filePath(imageFile.getAbsolutePath())
                            .algorithm(cacheProperties.getHashAlgorithm())
                            .build();

                    hashes.put(hash, record);
                    backfilled++;
                    log.debug("Backfilled hash for {} on {}: {}", comicName, date, hash);
                } else {
                    log.warn("Failed to calculate hash for {}", imageFile.getName());
                    failed++;
                }
            } catch (Exception e) {
                log.warn("Failed to backfill hash for {}: {}", imageFile.getName(), e.getMessage());
                failed++;
            }
        }

        log.info("Backfilled {} hashes for {} year {} ({} successful, {} failed)",
                backfilled, comicName, year, backfilled, failed);

        return hashes;
    }

    /**
     * Finds an existing hash record for a comic/year.
     * Ensures cache is loaded with backfill before searching.
     *
     * @param comicId The comic ID
     * @param comicName The comic name
     * @param year The year
     * @param hash The hash value to find
     * @return Optional containing the matching record, or empty if not found
     */
    public Optional<ImageHashRecord> findByHash(int comicId, String comicName, int year, String hash) {
        // Ensure cache is loaded with backfill
        loadHashesWithBackfill(comicId, comicName, year);
        return hashRepository.findByHash(comicId, comicName, year, hash);
    }

    /**
     * Adds a new hash record to the cache.
     *
     * @param comicId The comic ID
     * @param comicName The comic name
     * @param year The year
     * @param record The hash record to add
     */
    public void addHash(int comicId, String comicName, int year, ImageHashRecord record) {
        hashRepository.addHash(comicId, comicName, year, record);
    }

    /**
     * Adds an image to the hash cache after it has been saved to disk.
     * Calculates hash from image data and creates a hash record.
     *
     * @param comicId The comic ID
     * @param comicName The comic name
     * @param date The date of the comic
     * @param imageData The raw image data
     * @param filePath The file path where the image was saved
     */
    public void addImageToCache(int comicId, String comicName, LocalDate date,
                                byte[] imageData, String filePath) {
        ImageHasher hasher = imageHasherFactory.getImageHasher();
        String hash = hasher.calculateHash(imageData);

        if (hash != null) {
            ImageHashRecord record = ImageHashRecord.builder()
                    .hash(hash)
                    .date(date)
                    .filePath(filePath)
                    .algorithm(cacheProperties.getHashAlgorithm())
                    .build();

            int year = date.getYear();
            hashRepository.addHash(comicId, comicName, year, record);
            log.info("Added hash record for {} on {} to cache (hash: {})", comicName, date, hash);
        } else {
            log.warn("Failed to calculate hash for {} on {}, not adding to cache", comicName, date);
        }
    }
}
