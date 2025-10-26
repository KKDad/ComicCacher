package org.stapledon.common.service;

import org.stapledon.common.dto.DuplicateValidationResult;

import java.time.LocalDate;

/**
 * Service interface for detecting duplicate images before saving to the comic cache.
 * Prevents storing the same comic strip multiple times across different dates within the same year.
 */
public interface DuplicateValidationService {
    /**
     * Validates that an image is not a duplicate of an existing cached image for the same comic and year.
     * Allows re-downloading the same date (which would overwrite the existing file).
     *
     * @param comicId The comic ID
     * @param comicName The comic name
     * @param date The date this strip would be saved as
     * @param imageData The raw image bytes to validate
     * @return DuplicateValidationResult indicating if duplicate and details of existing image if found
     */
    DuplicateValidationResult validateNoDuplicate(int comicId, String comicName, LocalDate date, byte[] imageData);
}
