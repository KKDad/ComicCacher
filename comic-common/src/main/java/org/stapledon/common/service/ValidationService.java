package org.stapledon.common.service;

import org.stapledon.common.dto.ImageValidationResult;

/**
 * Service interface for validating image data integrity and format.
 * Provides validation for comic strips and avatars to ensure only well-formed
 * images are saved to the cache.
 */
public interface ValidationService {
    /**
     * Validates image data to ensure it is a well-formed image.
     * Checks for null/empty data, readable format, valid dimensions, and size limits.
     *
     * @param imageData The raw image bytes to validate
     * @return ImageValidationResult containing validation status and image metadata
     */
    ImageValidationResult validate(byte[] imageData);

    /**
     * Validates image data and additionally checks that dimensions meet minimum requirements.
     * Useful for ensuring comic strips are large enough to be readable.
     *
     * @param imageData The raw image bytes to validate
     * @param minWidth Minimum required width in pixels
     * @param minHeight Minimum required height in pixels
     * @return ImageValidationResult containing validation status and image metadata
     */
    ImageValidationResult validateWithMinDimensions(byte[] imageData, int minWidth, int minHeight);

    /**
     * Simple boolean check to determine if image data is valid.
     * Convenience method that calls validate() and returns the success status.
     *
     * @param imageData The raw image bytes to validate
     * @return true if the image is valid, false otherwise
     */
    boolean isValidImage(byte[] imageData);
}
