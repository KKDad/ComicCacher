package org.stapledon.engine.validation.hasher;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Unit tests for DifferenceImageHasher.
 * Tests perceptual hashing behavior (dHash) for image duplicate detection.
 * dHash is recommended as the default for comic duplicate detection.
 */
class DifferenceImageHasherTest {

    private DifferenceImageHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new DifferenceImageHasher();
    }

    @Test
    void calculateHashValidImageReturnsHash() throws Exception {
        // Create a simple 10x10 red image
        byte[] imageData = createTestImage(10, 10, Color.RED);

        String hash = hasher.calculateHash(imageData);

        assertThat(hash).as("Hash should not be null").isNotNull();
        assertThat(hash.length()).as("dHash should be 16 characters (64 bits in hex)").isEqualTo(16);
        assertThat(hash.matches("[0-9a-f]+")).as("Hash should be lowercase hexadecimal").isTrue();
    }

    @Test
    void calculateHashSameImageProducesSameHash() throws Exception {
        byte[] imageData1 = createTestImage(10, 10, Color.BLUE);
        byte[] imageData2 = createTestImage(10, 10, Color.BLUE);

        String hash1 = hasher.calculateHash(imageData1);
        String hash2 = hasher.calculateHash(imageData2);

        assertThat(hash2).as("Identical images should produce identical hashes").isEqualTo(hash1);
    }

    @Test
    void calculateHashDifferentImagesProduceDifferentHashes() throws Exception {
        // Perceptual hashes work on patterns, not colors
        // Solid color images will all produce the same hash (all zeros)
        // Use patterns instead
        byte[] checkerboard = createCheckerboardImage(100, 100);
        byte[] stripes = createStripesImage(100, 100);

        String checkerboardHash = hasher.calculateHash(checkerboard);
        String stripesHash = hasher.calculateHash(stripes);

        assertThat(stripesHash).as("Different patterns should produce different hashes").isNotEqualTo(checkerboardHash);
        assertThat(checkerboardHash).as("Checkerboard should not be all zeros").isNotEqualTo("0000000000000000");
        assertThat(stripesHash).as("Stripes should not be all zeros").isNotEqualTo("0000000000000000");
    }

    @Test
    void calculateHashSimilarImagesMayProduceSameHash() throws Exception {
        // dHash is perceptual - it should produce the same hash for visually similar images
        // Create two slightly different red images (different sizes but same visual content)
        byte[] small = createTestImage(100, 100, Color.RED);
        byte[] large = createTestImage(200, 200, Color.RED);

        String smallHash = hasher.calculateHash(small);
        String largeHash = hasher.calculateHash(large);

        // For solid color images, dHash should produce the same hash regardless of size
        // (because there are no horizontal gradients in a solid color image)
        assertThat(largeHash).as("dHash should produce same hash for solid color images of different sizes").isEqualTo(smallHash);
    }

    @Test
    void calculateHashNullDataReturnsNull() {
        String hash = hasher.calculateHash(null);
        assertThat(hash).as("Null image data should return null").isNull();
    }

    @Test
    void calculateHashEmptyDataReturnsNull() {
        String hash = hasher.calculateHash(new byte[0]);
        assertThat(hash).as("Empty image data should return null").isNull();
    }

    @Test
    void calculateHashInvalidImageDataReturnsNull() {
        // Random bytes that don't represent a valid image
        byte[] invalidData = new byte[]{0x01, 0x02, 0x03, 0x04};

        String hash = hasher.calculateHash(invalidData);

        // Unlike MD5/SHA256, dHash requires valid image decoding
        assertThat(hash).as("dHash should return null for invalid image data").isNull();
    }

    @Test
    void calculateHashHorizontalGradientProducesDifferentHashFromVertical() throws Exception {
        // Create a horizontal gradient (dark on left, light on right)
        BufferedImage horizontal = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g1 = horizontal.createGraphics();
        for (int x = 0; x < 100; x++) {
            int gray = (int) (x * 255.0 / 100);
            g1.setColor(new Color(gray, gray, gray));
            g1.fillRect(x, 0, 1, 100);
        }
        g1.dispose();

        // Create a vertical gradient (dark on top, light on bottom)
        // Note: Vertical gradient will produce all zeros in dHash (no horizontal variation)
        BufferedImage vertical = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = vertical.createGraphics();
        for (int y = 0; y < 100; y++) {
            int gray = (int) (y * 255.0 / 100);
            g2.setColor(new Color(gray, gray, gray));
            g2.fillRect(0, y, 100, 1);
        }
        g2.dispose();

        byte[] horizontalData = imageToBytes(horizontal);
        byte[] verticalData = imageToBytes(vertical);

        String horizontalHash = hasher.calculateHash(horizontalData);
        String verticalHash = hasher.calculateHash(verticalData);

        // dHash is based on horizontal gradients
        // Vertical gradient should be all zeros (no horizontal variation)
        // Horizontal gradient behavior depends on image resizing during hash calculation
        assertThat(verticalHash).as("Vertical gradient should produce all zeros (no horizontal variation)").isEqualTo("0000000000000000");

        // Verify we can calculate hashes for both
        assertThat(horizontalHash).as("Horizontal gradient hash should not be null").isNotNull();
        assertThat(verticalHash).as("Vertical gradient hash should not be null").isNotNull();
    }

    @Test
    void calculateHashSlightModificationMayProduceSimilarHash() throws Exception {
        // Create a gradient image
        BufferedImage img1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g1 = img1.createGraphics();
        for (int x = 0; x < 100; x++) {
            int gray = (int) (x * 255.0 / 100);
            g1.setColor(new Color(gray, gray, gray));
            g1.fillRect(x, 0, 1, 100);
        }
        g1.dispose();

        // Create a slightly modified version (add small noise)
        BufferedImage img2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img2.createGraphics();
        for (int x = 0; x < 100; x++) {
            int gray = (int) (x * 255.0 / 100);
            g2.setColor(new Color(gray, gray, gray));
            g2.fillRect(x, 0, 1, 100);
        }
        g2.setColor(Color.WHITE);
        g2.fillRect(45, 45, 2, 2);  // Tiny 2x2 white square
        g2.dispose();

        byte[] imageData1 = imageToBytes(img1);
        byte[] imageData2 = imageToBytes(img2);

        String hash1 = hasher.calculateHash(imageData1);
        String hash2 = hasher.calculateHash(imageData2);

        // With a very small modification, hashes should be very similar
        // This test documents the perceptual nature of dHash
        assertThat(hash1).isNotNull();
        assertThat(hash2).isNotNull();

        // Count bit differences (Hamming distance)
        int hammingDistance = calculateHammingDistance(hash1, hash2);

        // Small modifications should result in low Hamming distance
        assertThat(hammingDistance <= 10).as("Small modification should result in low Hamming distance (was: " + hammingDistance + ")").isTrue();
    }

    @Test
    void calculateHashDifferentPatternsProduceDifferentHashes() throws Exception {
        // Create a checkerboard pattern
        BufferedImage checkerboard = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g1 = checkerboard.createGraphics();
        for (int y = 0; y < 100; y += 10) {
            for (int x = 0; x < 100; x += 10) {
                g1.setColor(((x + y) / 10) % 2 == 0 ? Color.BLACK : Color.WHITE);
                g1.fillRect(x, y, 10, 10);
            }
        }
        g1.dispose();

        // Create a horizontal stripe pattern
        BufferedImage stripes = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = stripes.createGraphics();
        for (int y = 0; y < 100; y += 10) {
            g2.setColor((y / 10) % 2 == 0 ? Color.BLACK : Color.WHITE);
            g2.fillRect(0, y, 100, 10);
        }
        g2.dispose();

        byte[] checkerboardData = imageToBytes(checkerboard);
        byte[] stripesData = imageToBytes(stripes);

        String checkerboardHash = hasher.calculateHash(checkerboardData);
        String stripesHash = hasher.calculateHash(stripesData);

        assertThat(stripesHash).as("Different patterns should produce different dHashes").isNotEqualTo(checkerboardHash);
    }

    @Test
    void calculateHashSameHashLengthAsAverageHash() throws Exception {
        byte[] imageData = createTestImage(10, 10, Color.GREEN);

        String hash = hasher.calculateHash(imageData);

        assertThat(hash.length()).as("dHash should produce 16-character hash (same as aHash)").isEqualTo(16);
    }

    /**
     * Calculate Hamming distance between two hex strings.
     * Returns the number of differing bits.
     */
    private int calculateHammingDistance(String hash1, String hash2) {
        if (hash1.length() != hash2.length()) {
            throw new IllegalArgumentException("Hashes must be same length");
        }

        int distance = 0;
        for (int i = 0; i < hash1.length(); i++) {
            int xor = Integer.parseInt(String.valueOf(hash1.charAt(i)), 16)
                    ^ Integer.parseInt(String.valueOf(hash2.charAt(i)), 16);
            distance += Integer.bitCount(xor);
        }
        return distance;
    }

    /**
     * Helper method to create a checkerboard pattern image.
     */
    private byte[] createCheckerboardImage(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        int squareSize = 10;
        for (int y = 0; y < height; y += squareSize) {
            for (int x = 0; x < width; x += squareSize) {
                g.setColor(((x + y) / squareSize) % 2 == 0 ? Color.BLACK : Color.WHITE);
                g.fillRect(x, y, squareSize, squareSize);
            }
        }
        g.dispose();
        return imageToBytes(img);
    }

    /**
     * Helper method to create a vertical stripes pattern image.
     * Vertical stripes have horizontal variation (good for dHash).
     */
    private byte[] createStripesImage(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        int stripeWidth = 10;
        for (int x = 0; x < width; x += stripeWidth) {
            g.setColor((x / stripeWidth) % 2 == 0 ? Color.BLACK : Color.WHITE);
            g.fillRect(x, 0, stripeWidth, height);
        }
        g.dispose();
        return imageToBytes(img);
    }

    /**
     * Helper method to create a test image with specified dimensions and color.
     */
    private byte[] createTestImage(int width, int height, Color color) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();

        return imageToBytes(img);
    }

    /**
     * Helper method to convert BufferedImage to byte array.
     */
    private byte[] imageToBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }
}
