package org.stapledon.common.service;

import org.stapledon.common.dto.ImageMetadata;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.dto.StripText;

import java.io.File;

/**
 * Service for analyzing images to extract metadata.
 * Performs color mode detection and combines with validation results.
 */
public interface AnalysisService {
    /**
     * Analyzes an image file and creates comprehensive metadata.
     *
     * @param comicId   ID of the comic
     * @param comicName Name of the comic
     * @param sourceUrl optional source URL from which the image was downloaded
     */
    ImageMetadata analyzeImage(int comicId, String comicName, File imageFile, ImageValidationResult validation,
                               String sourceUrl);

    /**
     * Analyzes image data in memory and creates comprehensive metadata, including the strip's transcript and alt text.
     *
     * @param comicId    ID of the comic
     * @param comicName  Name of the comic
     * @param filePath   path where the image will be/is stored
     * @param sourceUrl  optional source URL from which the image was downloaded
     * @param text       the transcript and alt text the source gave with the strip (null for none)
     */
    ImageMetadata analyzeImage(int comicId, String comicName, byte[] imageData, String filePath,
                                ImageValidationResult validation, String sourceUrl, StripText text);

    /**
     * Analyzes image data in memory and creates comprehensive metadata.
     * <p>
     * Convenience overload that delegates to the text-aware version with no text.
     * </p>
     */
    default ImageMetadata analyzeImage(int comicId, String comicName, byte[] imageData, String filePath,
                               ImageValidationResult validation, String sourceUrl) {
        return analyzeImage(comicId, comicName, imageData, filePath, validation, sourceUrl, StripText.NONE);
    }

    /**
     * Detects the color mode of an image by sampling pixels.
     */
    ImageMetadata.ColorMode detectColorMode(byte[] imageData);
}
