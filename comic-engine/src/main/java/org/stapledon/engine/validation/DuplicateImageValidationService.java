package org.stapledon.engine.validation;

import org.springframework.stereotype.Component;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.DuplicateValidationResult;
import org.stapledon.common.dto.ImageHashRecord;
import org.stapledon.common.service.DuplicateValidationService;
import org.stapledon.common.service.ImageHasher;

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

    private final DuplicateHashCacheService hashCacheService;
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

        // Check if this hash already exists for this comic/year (with automatic backfill/rebuild)
        Optional<ImageHashRecord> existingRecord = hashCacheService.findByHash(comicId, comicName, year, hash);

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

        // No duplicate found - validation passed
        log.debug("Image for {} on {} is unique (hash: {})", comicName, date, hash);
        return DuplicateValidationResult.unique(hash);
    }
}
