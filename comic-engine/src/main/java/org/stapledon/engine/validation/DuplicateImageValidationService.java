package org.stapledon.engine.validation;

import org.springframework.stereotype.Component;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.DuplicateValidationResult;
import org.stapledon.common.dto.ImageHashRecord;
import org.stapledon.common.service.DuplicateValidationService;
import org.stapledon.common.service.ImageHasher;
import org.stapledon.engine.storage.DuplicateImageHashRepository;

import java.time.LocalDate;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of DuplicateValidationService that detects duplicate images at download time.
 * Prevents saving the same comic strip multiple times within the same year by comparing image hashes.
 * Allows re-downloading the same date (which overwrites the existing file).
 */
@Slf4j
@ToString
@Component
@RequiredArgsConstructor
public class DuplicateImageValidationService implements DuplicateValidationService {

    private final DuplicateImageHashRepository hashRepository;
    private final ImageHasherFactory imageHasherFactory;
    private final CacheProperties cacheProperties;

    @Override
    public DuplicateValidationResult validateNoDuplicate(int comicId,
                                                         String comicName,
                                                         LocalDate date,
                                                         byte[] imageData) {
        // Skip validation if duplicate detection is disabled
        if (!cacheProperties.isDuplicateDetectionEnabled()) {
            log.debug("Duplicate detection is disabled, skipping validation");
            return DuplicateValidationResult.unique("disabled");
        }

        // Calculate hash of the incoming image using the configured algorithm
        ImageHasher imageHasher = imageHasherFactory.getImageHasher();
        String hash = imageHasher.calculateHash(imageData);
        if (hash == null) {
            log.warn("Failed to calculate hash for image, skipping duplicate detection for {} on {}",
                    comicName, date);
            return DuplicateValidationResult.unique("hash-failed");
        }

        int year = date.getYear();

        // Check if this hash already exists for this comic/year
        Optional<ImageHashRecord> existingRecord = hashRepository.findByHash(comicId, comicName, year, hash);

        if (existingRecord.isPresent()) {
            ImageHashRecord existing = existingRecord.get();

            // Allow re-downloading the same date (overwrite scenario)
            if (existing.getDate().equals(date)) {
                log.debug("Hash {} matches existing image for same date {} - allowing overwrite",
                        hash, date);
                return DuplicateValidationResult.unique(hash);
            }

            // Found a duplicate on a different date
            log.warn("Duplicate image detected for {} on {}. Duplicate of {} (hash: {})",
                    comicName, date, existing.getDate(), hash);

            return DuplicateValidationResult.duplicate(
                    hash,
                    existing.getDate(),
                    existing.getFilePath()
            );
        }

        // No duplicate found - add this hash to the repository
        String filePath = buildFilePath(comicId, comicName, date);
        ImageHashRecord newRecord = ImageHashRecord.builder()
                .hash(hash)
                .date(date)
                .filePath(filePath)
                .algorithm(cacheProperties.getHashAlgorithm())
                .build();

        hashRepository.addHash(comicId, comicName, year, newRecord);

        log.debug("Image for {} on {} is unique (hash: {})", comicName, date, hash);
        return DuplicateValidationResult.unique(hash);
    }

    /**
     * Builds the expected file path for a comic strip.
     *
     * @param comicId The comic ID
     * @param comicName The comic name
     * @param date The date
     * @return The file path
     */
    private String buildFilePath(int comicId, String comicName, LocalDate date) {
        String comicNameParsed = getComicNameParsed(comicId, comicName);
        String year = String.valueOf(date.getYear());
        String filename = date.toString(); // yyyy-MM-dd format
        String cacheRoot = cacheProperties.getLocation();

        return String.format("%s/%s/%s/%s.png", cacheRoot, comicNameParsed, year, filename);
    }

    /**
     * Gets a directory name for a comic - uses the comic name if available,
     * otherwise falls back to the comic ID.
     *
     * @param comicId The comic ID
     * @param comicName The comic name (can be null)
     * @return A string to use as the directory name
     */
    private String getComicNameParsed(int comicId, String comicName) {
        if (comicName == null) {
            return "comic_" + comicId;
        }
        return comicName.replace(" ", "");
    }
}
