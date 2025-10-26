package org.stapledon.common.dto;

/**
 * Enumeration of supported hash algorithms for duplicate image detection.
 */
public enum HashAlgorithm {
    /**
     * MD5 cryptographic hash - fast, byte-exact matching only.
     * Good for detecting exact duplicates but won't catch re-encoded images.
     * Recommended default.
     */
    MD5,

    /**
     * SHA-256 cryptographic hash - more secure than MD5, byte-exact matching.
     * Slower than MD5 but more collision-resistant. Also won't catch re-encoded images.
     */
    SHA256,

    /**
     * Average Hash (aHash) - simple perceptual hash using average pixel values.
     * Fast, lightweight, no external dependencies. Detects visually similar images.
     * Good for catching re-encoded duplicates and minor variations.
     */
    AVERAGE_HASH,

    /**
     * Difference Hash (dHash) - perceptual hash using pixel gradients.
     * Fast, lightweight, no external dependencies. More robust than aHash to slight modifications.
     * Best balance of speed and duplicate detection for comics.
     */
    DIFFERENCE_HASH
}
