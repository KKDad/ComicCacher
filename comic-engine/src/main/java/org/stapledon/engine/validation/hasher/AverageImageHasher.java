package org.stapledon.engine.validation.hasher;

import org.springframework.stereotype.Component;
import org.stapledon.common.service.ImageHasher;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Average Hash (aHash) implementation for duplicate image detection.
 * Fast perceptual hash that detects visually similar images even if bytes differ.
 *
 * Algorithm:
 * 1. Resize image to 8x8 grayscale
 * 2. Calculate average pixel value
 * 3. For each pixel: set bit to 1 if above average, 0 if below
 * 4. Convert resulting 64 bits to hex string
 *
 * Good for catching re-encoded duplicates and minor variations.
 */
@Slf4j
@ToString
@Component("averageImageHasher")
public class AverageImageHasher implements ImageHasher {

    private static final int HASH_SIZE = 8;

    @Override
    public String calculateHash(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            log.warn("Cannot calculate average hash for null or empty image data");
            return null;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageData)) {
            BufferedImage image = ImageIO.read(bis);
            if (image == null) {
                log.warn("ImageIO could not decode image for average hashing");
                return null;
            }

            // Resize to 8x8 and convert to grayscale
            BufferedImage resized = resizeAndGrayscale(image, HASH_SIZE, HASH_SIZE);

            // Calculate average pixel value
            int sum = 0;
            for (int y = 0; y < HASH_SIZE; y++) {
                for (int x = 0; x < HASH_SIZE; x++) {
                    sum += resized.getRGB(x, y) & 0xFF;
                }
            }
            int average = sum / (HASH_SIZE * HASH_SIZE);

            // Build hash based on pixels above/below average
            long hash = 0;
            for (int y = 0; y < HASH_SIZE; y++) {
                for (int x = 0; x < HASH_SIZE; x++) {
                    int pixel = resized.getRGB(x, y) & 0xFF;
                    if (pixel > average) {
                        hash |= 1L << (y * HASH_SIZE + x);
                    }
                }
            }

            return String.format("%016x", hash);

        } catch (IOException e) {
            log.error("Failed to calculate average hash: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Resizes an image and converts it to grayscale.
     *
     * @param original The original image
     * @param width The target width
     * @param height The target height
     * @return The resized grayscale image
     */
    private BufferedImage resizeAndGrayscale(BufferedImage original, int width, int height) {
        BufferedImage grayscale = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = grayscale.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return grayscale;
    }
}
