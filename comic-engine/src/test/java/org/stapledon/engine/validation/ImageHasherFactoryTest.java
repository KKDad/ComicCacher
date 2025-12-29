package org.stapledon.engine.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.HashAlgorithm;
import org.stapledon.common.service.ImageHasher;
import org.stapledon.engine.validation.hasher.AverageImageHasher;
import org.stapledon.engine.validation.hasher.DifferenceImageHasher;
import org.stapledon.engine.validation.hasher.MD5ImageHasher;
import org.stapledon.engine.validation.hasher.SHA256ImageHasher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ImageHasherFactory.
 * Tests Spring bean selection based on HashAlgorithm configuration.
 */
@ExtendWith(MockitoExtension.class)
class ImageHasherFactoryTest {

    @Mock
    private CacheProperties cacheProperties;

    private MD5ImageHasher md5ImageHasher;
    private SHA256ImageHasher sha256ImageHasher;
    private AverageImageHasher averageImageHasher;
    private DifferenceImageHasher differenceImageHasher;

    private ImageHasherFactory factory;

    @BeforeEach
    void setUp() {
        // Create real hasher instances
        md5ImageHasher = new MD5ImageHasher();
        sha256ImageHasher = new SHA256ImageHasher();
        averageImageHasher = new AverageImageHasher();
        differenceImageHasher = new DifferenceImageHasher();

        // Create factory with mocked properties and real hasher instances
        factory = new ImageHasherFactory(
                cacheProperties,
                md5ImageHasher,
                sha256ImageHasher,
                averageImageHasher,
                differenceImageHasher
        );
    }

    @Test
    void testGetImageHasher_MD5_ReturnsMD5Hasher() {
        // Given
        when(cacheProperties.getHashAlgorithm()).thenReturn(HashAlgorithm.MD5);

        // When
        ImageHasher hasher = factory.getImageHasher();

        // Then
        assertThat(hasher).as("Hasher should not be null").isNotNull();
        assertThat(hasher).as("Should return MD5ImageHasher instance").isSameAs(md5ImageHasher);
        assertThat(hasher instanceof MD5ImageHasher).as("Should be an instance of MD5ImageHasher").isTrue();
    }

    @Test
    void testGetImageHasher_SHA256_ReturnsSHA256Hasher() {
        // Given
        when(cacheProperties.getHashAlgorithm()).thenReturn(HashAlgorithm.SHA256);

        // When
        ImageHasher hasher = factory.getImageHasher();

        // Then
        assertThat(hasher).as("Hasher should not be null").isNotNull();
        assertThat(hasher).as("Should return SHA256ImageHasher instance").isSameAs(sha256ImageHasher);
        assertThat(hasher instanceof SHA256ImageHasher).as("Should be an instance of SHA256ImageHasher").isTrue();
    }

    @Test
    void testGetImageHasher_AverageHash_ReturnsAverageHasher() {
        // Given
        when(cacheProperties.getHashAlgorithm()).thenReturn(HashAlgorithm.AVERAGE_HASH);

        // When
        ImageHasher hasher = factory.getImageHasher();

        // Then
        assertThat(hasher).as("Hasher should not be null").isNotNull();
        assertThat(hasher).as("Should return AverageImageHasher instance").isSameAs(averageImageHasher);
        assertThat(hasher instanceof AverageImageHasher).as("Should be an instance of AverageImageHasher").isTrue();
    }

    @Test
    void testGetImageHasher_DifferenceHash_ReturnsDifferenceHasher() {
        // Given
        when(cacheProperties.getHashAlgorithm()).thenReturn(HashAlgorithm.DIFFERENCE_HASH);

        // When
        ImageHasher hasher = factory.getImageHasher();

        // Then
        assertThat(hasher).as("Hasher should not be null").isNotNull();
        assertThat(hasher).as("Should return DifferenceImageHasher instance").isSameAs(differenceImageHasher);
        assertThat(hasher instanceof DifferenceImageHasher).as("Should be an instance of DifferenceImageHasher").isTrue();
    }

    @Test
    void testGetImageHasher_CalledMultipleTimes_ReturnsSameInstance() {
        // Given
        when(cacheProperties.getHashAlgorithm()).thenReturn(HashAlgorithm.MD5);

        // When
        ImageHasher hasher1 = factory.getImageHasher();
        ImageHasher hasher2 = factory.getImageHasher();

        // Then
        assertThat(hasher2).as("Multiple calls should return the same hasher instance").isSameAs(hasher1);
    }

    @Test
    void testGetImageHasher_DifferentAlgorithms_ReturnsDifferentInstances() {
        // Given & When & Then
        when(cacheProperties.getHashAlgorithm()).thenReturn(HashAlgorithm.MD5);
        ImageHasher md5 = factory.getImageHasher();

        when(cacheProperties.getHashAlgorithm()).thenReturn(HashAlgorithm.SHA256);
        ImageHasher sha256 = factory.getImageHasher();

        assertNotSame(md5, sha256, "Different algorithms should return different hasher instances");
    }

    @Test
    void testGetImageHasher_AllAlgorithms_AreDistinct() {
        // Verify all four hasher implementations are distinct instances
        assertNotSame(md5ImageHasher, sha256ImageHasher, "MD5 and SHA256 hashers should be different");
        assertNotSame(md5ImageHasher, averageImageHasher, "MD5 and Average hashers should be different");
        assertNotSame(md5ImageHasher, differenceImageHasher, "MD5 and Difference hashers should be different");
        assertNotSame(sha256ImageHasher, averageImageHasher, "SHA256 and Average hashers should be different");
        assertNotSame(sha256ImageHasher, differenceImageHasher, "SHA256 and Difference hashers should be different");
        assertNotSame(averageImageHasher, differenceImageHasher, "Average and Difference hashers should be different");
    }

    @Test
    void testGetImageHasher_PerceptualHashers_HaveSameHashLength() {
        // Both perceptual hashers (aHash and dHash) should produce 16-character hashes
        byte[] testData = new byte[]{0x01, 0x02, 0x03, 0x04};

        // Create a simple test image
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.RED);
        g.fillRect(0, 0, 10, 10);
        g.dispose();

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try {
            javax.imageio.ImageIO.write(img, "PNG", baos);
            byte[] imageData = baos.toByteArray();

            String avgHash = averageImageHasher.calculateHash(imageData);
            String diffHash = differenceImageHasher.calculateHash(imageData);

            assertThat(avgHash).as("Average hash should not be null").isNotNull();
            assertThat(diffHash).as("Difference hash should not be null").isNotNull();
            assertThat(avgHash.length()).as("Average hash should be 16 characters").isEqualTo(16);
            assertThat(diffHash.length()).as("Difference hash should be 16 characters").isEqualTo(16);
            assertThat(diffHash.length()).as("Both perceptual hashers should produce same-length hashes").isEqualTo(avgHash.length());
        } catch (java.io.IOException e) {
            fail("", "Failed to create test image: " + e.getMessage());
        }
    }

    @Test
    void testGetImageHasher_CryptographicHashers_HaveDifferentLengths() {
        // MD5 produces 32-char hash, SHA256 produces 64-char hash
        byte[] testData = "test data".getBytes();

        String md5Hash = md5ImageHasher.calculateHash(testData);
        String sha256Hash = sha256ImageHasher.calculateHash(testData);

        assertThat(md5Hash).as("MD5 hash should not be null").isNotNull();
        assertThat(sha256Hash).as("SHA256 hash should not be null").isNotNull();
        assertThat(md5Hash.length()).as("MD5 hash should be 32 characters").isEqualTo(32);
        assertThat(sha256Hash.length()).as("SHA256 hash should be 64 characters").isEqualTo(64);
        assertThat(sha256Hash.length()).as("Cryptographic hashers should produce different-length hashes").isNotEqualTo(md5Hash.length());
    }
}
