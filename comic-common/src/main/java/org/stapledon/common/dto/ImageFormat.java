package org.stapledon.common.dto;

/**
 * Enum representing supported image formats for comic strips and avatars.
 * Used by the image validation service to identify the format of downloaded images.
 */
public enum ImageFormat {
    /**
     * Graphics Interchange Format
     */
    GIF,

    /**
     * Joint Photographic Experts Group format
     */
    JPEG,

    /**
     * Portable Network Graphics format
     */
    PNG,

    /**
     * Tagged Image File Format
     */
    TIFF,

    /**
     * Bitmap Image File format
     */
    BMP,

    /**
     * WebP format (requires TwelveMonkeys ImageIO plugin)
     */
    WEBP,

    /**
     * Unknown or unsupported format
     */
    UNKNOWN
}
