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
 * Difference Hash (dHash) implementation for duplicate image detection.
 * Fast perceptual hash that's more robust than aHash to slight modifications.
 *
 * Algorithm:
 * 1. Resize image to 9x8 grayscale (one extra column)
 * 2. For each row, compare adjacent pixels
 * 3. Set bit to 1 if left pixel > right pixel, 0 otherwise
 * 4. Convert resulting 64 bits to hex string
 *
 * Best balance of speed and duplicate detection for comics.
 * Recommended default for comic duplicate detection.
 */
@Slf4j
@ToString
@Component("differenceImageHasher")
public class DifferenceImageHasher implements ImageHasher {

    private static final int HASH_SIZE = 8;

    @Override
    public String calculateHash(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            log.warn("Cannot calculate difference hash for null or empty image data");
            return null;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageData)) {
            BufferedImage image = ImageIO.read(bis);
            if (image == null) {
                log.warn("ImageIO could not decode image for difference hashing");
                return null;
            }

            // Resize to 9x8 (need one extra column for comparison)
            BufferedImage resized = resizeAndGrayscale(image, HASH_SIZE + 1, HASH_SIZE);

            // Build hash based on horizontal gradients
            long hash = 0;
            for (int y = 0; y < HASH_SIZE; y++) {
                for (int x = 0; x < HASH_SIZE; x++) {
                    int leftPixel = resized.getRGB(x, y) & 0xFF;
                    int rightPixel = resized.getRGB(x + 1, y) & 0xFF;
                    if (leftPixel > rightPixel) {
                        hash |= 1L << (y * HASH_SIZE + x);
                    }
                }
            }

            return String.format("%016x", hash);

        } catch (IOException e) {
            log.error("Failed to calculate difference hash: {}", e.getMessage(), e);
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
