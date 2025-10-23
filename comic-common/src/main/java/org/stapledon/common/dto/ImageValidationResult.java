package org.stapledon.common.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object representing the result of an image validation operation.
 * Contains validation status, format information, dimensions, and error details if validation failed.
 */
@Data
@Builder
public class ImageValidationResult {
    /**
     * Flag indicating whether the image validation was successful.
     */
    private final boolean valid;

    /**
     * Error message describing why validation failed (null if successful).
     */
    private final String errorMessage;

    /**
     * The detected format of the image (PNG, JPEG, GIF, WEBP, or UNKNOWN).
     */
    private final ImageFormat format;

    /**
     * Width of the image in pixels.
     */
    private final int width;

    /**
     * Height of the image in pixels.
     */
    private final int height;

    /**
     * Size of the image data in bytes.
     */
    private final long sizeInBytes;

    /**
     * Factory method to create a successful validation result.
     *
     * @param format The detected image format
     * @param width Width of the image in pixels
     * @param height Height of the image in pixels
     * @param sizeInBytes Size of the image data in bytes
     * @return A successful ImageValidationResult
     */
    public static ImageValidationResult success(ImageFormat format, int width, int height, long sizeInBytes) {
        return ImageValidationResult.builder()
                .valid(true)
                .format(format)
                .width(width)
                .height(height)
                .sizeInBytes(sizeInBytes)
                .build();
    }

    /**
     * Factory method to create a failed validation result.
     *
     * @param errorMessage Description of why validation failed
     * @return A failed ImageValidationResult
     */
    public static ImageValidationResult failure(String errorMessage) {
        return ImageValidationResult.builder()
                .valid(false)
                .errorMessage(errorMessage)
                .format(ImageFormat.UNKNOWN)
                .width(0)
                .height(0)
                .sizeInBytes(0)
                .build();
    }
}
