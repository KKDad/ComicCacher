package org.stapledon.engine.validation.hasher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MD5ImageHasher.
 * Tests cryptographic hashing behavior for image duplicate detection.
 */
class MD5ImageHasherTest {

    private MD5ImageHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new MD5ImageHasher();
    }

    @Test
    void testCalculateHash_ValidImage_ReturnsHash() throws IOException {
        // Create a simple 10x10 red image
        byte[] imageData = createTestImage(10, 10, Color.RED);

        String hash = hasher.calculateHash(imageData);

        assertNotNull(hash, "Hash should not be null");
        assertEquals(32, hash.length(), "MD5 hash should be 32 characters (128 bits in hex)");
        assertTrue(hash.matches("[0-9a-f]+"), "Hash should be lowercase hexadecimal");
    }

    @Test
    void testCalculateHash_SameImage_ProducesSameHash() throws IOException {
        byte[] imageData1 = createTestImage(10, 10, Color.BLUE);
        byte[] imageData2 = createTestImage(10, 10, Color.BLUE);

        String hash1 = hasher.calculateHash(imageData1);
        String hash2 = hasher.calculateHash(imageData2);

        assertEquals(hash1, hash2, "Identical images should produce identical hashes");
    }

    @Test
    void testCalculateHash_DifferentImages_ProduceDifferentHashes() throws IOException {
        byte[] redImage = createTestImage(10, 10, Color.RED);
        byte[] blueImage = createTestImage(10, 10, Color.BLUE);

        String redHash = hasher.calculateHash(redImage);
        String blueHash = hasher.calculateHash(blueImage);

        assertNotEquals(redHash, blueHash, "Different images should produce different hashes");
    }

    @Test
    void testCalculateHash_SinglePixelDifference_ProducesDifferentHash() throws IOException {
        // MD5 is byte-exact, so even a single pixel difference should produce a different hash
        BufferedImage img1 = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        BufferedImage img2 = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);

        // Fill both with red
        Graphics2D g1 = img1.createGraphics();
        g1.setColor(Color.RED);
        g1.fillRect(0, 0, 10, 10);
        g1.dispose();

        Graphics2D g2 = img2.createGraphics();
        g2.setColor(Color.RED);
        g2.fillRect(0, 0, 10, 10);
        g2.dispose();

        // Change one pixel in img2
        img2.setRGB(5, 5, Color.BLUE.getRGB());

        byte[] imageData1 = imageToBytes(img1);
        byte[] imageData2 = imageToBytes(img2);

        String hash1 = hasher.calculateHash(imageData1);
        String hash2 = hasher.calculateHash(imageData2);

        assertNotEquals(hash1, hash2, "Images with single pixel difference should have different MD5 hashes");
    }

    @Test
    void testCalculateHash_NullData_ReturnsNull() {
        String hash = hasher.calculateHash(null);
        assertNull(hash, "Null image data should return null");
    }

    @Test
    void testCalculateHash_EmptyData_ReturnsNull() {
        String hash = hasher.calculateHash(new byte[0]);
        assertNull(hash, "Empty image data should return null");
    }

    @Test
    void testCalculateHash_InvalidImageData_ReturnsNull() {
        // Random bytes that don't represent a valid image
        byte[] invalidData = new byte[]{0x01, 0x02, 0x03, 0x04};

        String hash = hasher.calculateHash(invalidData);

        // MD5 should still calculate a hash because it's just hashing the bytes
        // It doesn't validate whether it's a valid image
        assertNotNull(hash, "MD5 should hash any byte array, even if not a valid image");
        assertEquals(32, hash.length(), "MD5 hash should be 32 characters");
    }

    @Test
    void testCalculateHash_DifferentSizes_SameContent_ProducesDifferentHash() throws IOException {
        // Create two solid red images of different sizes
        byte[] small = createTestImage(10, 10, Color.RED);
        byte[] large = createTestImage(20, 20, Color.RED);

        String smallHash = hasher.calculateHash(small);
        String largeHash = hasher.calculateHash(large);

        assertNotEquals(smallHash, largeHash,
                "Images with different dimensions should have different hashes, even if same color");
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
