package org.stapledon.common.service;

/**
 * Interface for calculating image hashes for duplicate detection.
 * Implementations provide different hashing algorithms (MD5, SHA-256, perceptual hashes, etc.).
 */
public interface ImageHasher {
    /**
     * Calculates a hash for the given image data.
     *
     * @param imageData The raw image bytes
     * @return The calculated hash as a hexadecimal string, or null if hashing fails
     */
    String calculateHash(byte[] imageData);
}
