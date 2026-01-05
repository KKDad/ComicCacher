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
 * Unit tests for AverageImageHasher.
 * Tests perceptual hashing behavior (aHash) for image duplicate detection.
 */
class AverageImageHasherTest {

    private AverageImageHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new AverageImageHasher();
    }

    @Test
    void testCalculateHash_ValidImage_ReturnsHash() throws IOException {
        // Create a simple 10x10 red image
        byte[] imageData = createTestImage(10, 10, Color.RED);

        String hash = hasher.calculateHash(imageData);

        assertThat(hash).as("Hash should not be null").isNotNull();
        assertThat(hash.length()).as("aHash should be 16 characters (64 bits in hex)").isEqualTo(16);
        assertThat(hash.matches("[0-9a-f]+")).as("Hash should be lowercase hexadecimal").isTrue();
    }

    @Test
    void testCalculateHash_SameImage_ProducesSameHash() throws IOException {
        byte[] imageData1 = createTestImage(10, 10, Color.BLUE);
        byte[] imageData2 = createTestImage(10, 10, Color.BLUE);

        String hash1 = hasher.calculateHash(imageData1);
        String hash2 = hasher.calculateHash(imageData2);

        assertThat(hash2).as("Identical images should produce identical hashes").isEqualTo(hash1);
    }

    @Test
    void testCalculateHash_DifferentImages_ProduceDifferentHashes() throws IOException {
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
    void testCalculateHash_SimilarImages_MayProduceSameHash() throws IOException {
        // aHash is perceptual - it should produce the same hash for visually similar images
        // Create two slightly different red images (different sizes but same visual content)
        byte[] small = createTestImage(100, 100, Color.RED);
        byte[] large = createTestImage(200, 200, Color.RED);

        String smallHash = hasher.calculateHash(small);
        String largeHash = hasher.calculateHash(large);

        // For solid color images, aHash should produce the same hash regardless of size
        assertThat(largeHash).as("aHash should produce same hash for solid color images of different sizes").isEqualTo(smallHash);
    }

    @Test
    void testCalculateHash_SlightVariation_MayProduceSimilarHash() throws IOException {
        // Create a base image
        BufferedImage img1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g1 = img1.createGraphics();
        g1.setColor(Color.RED);
        g1.fillRect(0, 0, 100, 100);
        g1.dispose();

        // Create a slightly modified version (add a small white rectangle)
        BufferedImage img2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img2.createGraphics();
        g2.setColor(Color.RED);
        g2.fillRect(0, 0, 100, 100);
        g2.setColor(Color.WHITE);
        g2.fillRect(45, 45, 10, 10);  // Small 10x10 white square in center
        g2.dispose();

        byte[] imageData1 = imageToBytes(img1);
        byte[] imageData2 = imageToBytes(img2);

        String hash1 = hasher.calculateHash(imageData1);
        String hash2 = hasher.calculateHash(imageData2);

        // With a small modification, hashes might be the same or very similar
        // This test documents the perceptual nature of aHash
        assertThat(hash1).isNotNull();
        assertThat(hash2).isNotNull();
    }

    @Test
    void testCalculateHash_NullData_ReturnsNull() {
        String hash = hasher.calculateHash(null);
        assertThat(hash).as("Null image data should return null").isNull();
    }

    @Test
    void testCalculateHash_EmptyData_ReturnsNull() {
        String hash = hasher.calculateHash(new byte[0]);
        assertThat(hash).as("Empty image data should return null").isNull();
    }

    @Test
    void testCalculateHash_InvalidImageData_ReturnsNull() {
        // Random bytes that don't represent a valid image
        byte[] invalidData = new byte[]{0x01, 0x02, 0x03, 0x04};

        String hash = hasher.calculateHash(invalidData);

        // Unlike MD5/SHA256, aHash requires valid image decoding
        assertThat(hash).as("aHash should return null for invalid image data").isNull();
    }

    @Test
    void testCalculateHash_DifferentPatterns_ProduceDifferentHashes() throws IOException {
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

        assertThat(stripesHash).as("Different patterns should produce different aHashes").isNotEqualTo(checkerboardHash);
    }

    @Test
    void testCalculateHash_ShorterThanCryptographicHashes() throws IOException {
        byte[] imageData = createTestImage(10, 10, Color.GREEN);

        String hash = hasher.calculateHash(imageData);

        assertThat(hash.length()).as("aHash should produce 16-character hash").isEqualTo(16);
        assertThat(hash.length() < 32).as("aHash should be shorter than MD5 (32 chars)").isTrue();
        assertThat(hash.length() < 64).as("aHash should be shorter than SHA-256 (64 chars)").isTrue();
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
     * Helper method to create a horizontal stripes pattern image.
     */
    private byte[] createStripesImage(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        int stripeHeight = 10;
        for (int y = 0; y < height; y += stripeHeight) {
            g.setColor((y / stripeHeight) % 2 == 0 ? Color.BLACK : Color.WHITE);
            g.fillRect(0, y, width, stripeHeight);
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
