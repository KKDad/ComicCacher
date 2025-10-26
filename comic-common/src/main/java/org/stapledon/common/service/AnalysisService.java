package org.stapledon.common.service;

import org.stapledon.common.dto.ImageMetadata;
import org.stapledon.common.dto.ImageValidationResult;

import java.io.File;

/**
 * Service for analyzing images to extract metadata.
 * Performs color mode detection and combines with validation results.
 */
public interface AnalysisService {
    /**
     * Analyzes an image file and creates comprehensive metadata.
     *
     * @param imageFile The image file to analyze
     * @param validation The validation result from ValidationService
     * @param sourceUrl The source URL from which the image was downloaded (optional)
     * @return ImageMetadata containing all analyzed properties
     */
    ImageMetadata analyzeImage(File imageFile, ImageValidationResult validation, String sourceUrl);

    /**
     * Analyzes image data in memory and creates comprehensive metadata.
     *
     * @param imageData The image data bytes
     * @param filePath The path where the image will be/is stored
     * @param validation The validation result from ValidationService
     * @param sourceUrl The source URL from which the image was downloaded (optional)
     * @return ImageMetadata containing all analyzed properties
     */
    ImageMetadata analyzeImage(byte[] imageData, String filePath, ImageValidationResult validation, String sourceUrl);

    /**
     * Detects the color mode of an image by sampling pixels.
     *
     * @param imageData The image data bytes
     * @return The detected color mode (GRAYSCALE, COLOR, or UNKNOWN)
     */
    ImageMetadata.ColorMode detectColorMode(byte[] imageData);
}
