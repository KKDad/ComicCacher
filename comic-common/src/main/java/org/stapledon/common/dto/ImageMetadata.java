package org.stapledon.common.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/**
 * Metadata about an individual comic image.
 * Stored as a sidecar JSON file alongside each image.
 */
@Data
@Builder
public class ImageMetadata {
    /**
     * Absolute path to the image file
     */
    private final String filePath;

    /**
     * Image format (PNG, JPEG, GIF, WEBP)
     */
    private final ImageFormat format;

    /**
     * Image width in pixels
     */
    private final int width;

    /**
     * Image height in pixels
     */
    private final int height;

    /**
     * File size in bytes
     */
    private final long sizeInBytes;

    /**
     * Color mode detected from the image
     */
    private final ColorMode colorMode;

    /**
     * Percentage of pixels sampled for color detection (0.0 - 100.0)
     */
    private final double samplePercentage;

    /**
     * Timestamp when this metadata was captured
     */
    private final LocalDateTime captureTimestamp;

    /**
     * Source URL from which the image was downloaded (if available)
     */
    private final String sourceUrl;

    /**
     * Enum representing the color mode of an image
     */
    public enum ColorMode {
        /**
         * Image contains only grayscale pixels (no color)
         */
        GRAYSCALE,

        /**
         * Image contains colored pixels
         */
        COLOR,

        /**
         * Could not determine color mode
         */
        UNKNOWN
    }
}
