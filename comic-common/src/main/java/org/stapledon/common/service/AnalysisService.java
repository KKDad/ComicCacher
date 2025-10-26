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
     * @param sourceUrl optional source URL from which the image was downloaded
     */
    ImageMetadata analyzeImage(File imageFile, ImageValidationResult validation, String sourceUrl);

    /**
     * Analyzes image data in memory and creates comprehensive metadata.
     *
     * @param filePath path where the image will be/is stored
     * @param sourceUrl optional source URL from which the image was downloaded
     */
    ImageMetadata analyzeImage(byte[] imageData, String filePath, ImageValidationResult validation, String sourceUrl);

    /**
     * Detects the color mode of an image by sampling pixels.
     */
    ImageMetadata.ColorMode detectColorMode(byte[] imageData);
}
