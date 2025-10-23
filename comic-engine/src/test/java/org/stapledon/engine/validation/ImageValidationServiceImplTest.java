package org.stapledon.engine.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.dto.ImageValidationResult;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ImageValidationServiceImpl.
 * Tests validation of various image formats, corrupted images, and edge cases.
 */
class ImageValidationServiceImplTest {

    private ImageValidationServiceImpl imageValidationService;

    @BeforeEach
    void setUp() {
        imageValidationService = new ImageValidationServiceImpl();
    }

    /**
     * Helper method to create a test image in the specified format.
     *
     * @param width Width in pixels
     * @param height Height in pixels
     * @param format Image format (e.g., "PNG", "JPEG", "GIF")
     * @return The image as a byte array
     * @throws IOException if image creation fails
     */
    private byte[] createTestImage(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    @Test
    void testValidPngImage() throws IOException {
        byte[] imageData = createTestImage(800, 600, "PNG");

        ImageValidationResult result = imageValidationService.validate(imageData);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFormat()).isEqualTo(ImageFormat.PNG);
        assertThat(result.getWidth()).isEqualTo(800);
        assertThat(result.getHeight()).isEqualTo(600);
        assertThat(result.getSizeInBytes()).isEqualTo(imageData.length);
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void testValidJpegImage() throws IOException {
        byte[] imageData = createTestImage(640, 480, "JPEG");

        ImageValidationResult result = imageValidationService.validate(imageData);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFormat()).isEqualTo(ImageFormat.JPEG);
        assertThat(result.getWidth()).isEqualTo(640);
        assertThat(result.getHeight()).isEqualTo(480);
        assertThat(result.getSizeInBytes()).isEqualTo(imageData.length);
    }

    @Test
    void testValidGifImage() throws IOException {
        byte[] imageData = createTestImage(400, 300, "GIF");

        ImageValidationResult result = imageValidationService.validate(imageData);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFormat()).isEqualTo(ImageFormat.GIF);
        assertThat(result.getWidth()).isEqualTo(400);
        assertThat(result.getHeight()).isEqualTo(300);
    }

    @Test
    void testSmallValidImage() throws IOException {
        // Test minimum viable image (1x1 pixel)
        byte[] imageData = createTestImage(1, 1, "PNG");

        ImageValidationResult result = imageValidationService.validate(imageData);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getWidth()).isEqualTo(1);
        assertThat(result.getHeight()).isEqualTo(1);
    }

    @Test
    void testNullImageData() {
        ImageValidationResult result = imageValidationService.validate(null);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("null or empty");
        assertThat(result.getFormat()).isEqualTo(ImageFormat.UNKNOWN);
    }

    @Test
    void testEmptyImageData() {
        byte[] emptyData = new byte[0];

        ImageValidationResult result = imageValidationService.validate(emptyData);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("null or empty");
    }

    @Test
    void testCorruptedImage() {
        // Create a valid PNG and then truncate it
        byte[] validImage;
        try {
            validImage = createTestImage(100, 100, "PNG");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Truncate to first 50 bytes - this should be invalid
        byte[] corruptedImage = Arrays.copyOf(validImage, 50);

        ImageValidationResult result = imageValidationService.validate(corruptedImage);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isNotNull();
    }

    @Test
    void testHtmlContentAsImage() {
        // Simulate receiving HTML error page instead of image
        String htmlContent = "<html><body><h1>404 Not Found</h1></body></html>";
        byte[] htmlBytes = htmlContent.getBytes();

        ImageValidationResult result = imageValidationService.validate(htmlBytes);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isNotNull();
    }

    @Test
    void testRandomBinaryData() {
        // Create random binary data that isn't a valid image
        byte[] randomData = new byte[1000];
        for (int i = 0; i < randomData.length; i++) {
            randomData[i] = (byte) (Math.random() * 256);
        }

        ImageValidationResult result = imageValidationService.validate(randomData);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void testOversizedImage() {
        // Create an image larger than MAX_IMAGE_SIZE (10MB)
        byte[] oversizedData = new byte[11 * 1024 * 1024]; // 11MB

        ImageValidationResult result = imageValidationService.validate(oversizedData);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("exceeds maximum size");
    }

    @Test
    void testValidateWithMinDimensions_Pass() throws IOException {
        byte[] imageData = createTestImage(200, 150, "PNG");

        ImageValidationResult result = imageValidationService.validateWithMinDimensions(
                imageData, 100, 50);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getWidth()).isEqualTo(200);
        assertThat(result.getHeight()).isEqualTo(150);
    }

    @Test
    void testValidateWithMinDimensions_FailWidth() throws IOException {
        byte[] imageData = createTestImage(80, 150, "PNG");

        ImageValidationResult result = imageValidationService.validateWithMinDimensions(
                imageData, 100, 50);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("below minimum");
    }

    @Test
    void testValidateWithMinDimensions_FailHeight() throws IOException {
        byte[] imageData = createTestImage(200, 40, "PNG");

        ImageValidationResult result = imageValidationService.validateWithMinDimensions(
                imageData, 100, 50);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("below minimum");
    }

    @Test
    void testValidateWithMinDimensions_ExactBoundary() throws IOException {
        byte[] imageData = createTestImage(100, 50, "PNG");

        ImageValidationResult result = imageValidationService.validateWithMinDimensions(
                imageData, 100, 50);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testValidateWithMinDimensions_InvalidImage() {
        byte[] invalidData = "not an image".getBytes();

        ImageValidationResult result = imageValidationService.validateWithMinDimensions(
                invalidData, 100, 50);

        assertThat(result.isValid()).isFalse();
        // Should fail at image validation, not dimension check
        assertThat(result.getErrorMessage()).doesNotContain("below minimum");
    }

    @Test
    void testIsValidImage_Valid() throws IOException {
        byte[] imageData = createTestImage(100, 100, "PNG");

        boolean isValid = imageValidationService.isValidImage(imageData);

        assertThat(isValid).isTrue();
    }

    @Test
    void testIsValidImage_Invalid() {
        byte[] invalidData = "not an image".getBytes();

        boolean isValid = imageValidationService.isValidImage(invalidData);

        assertThat(isValid).isFalse();
    }

    @Test
    void testTypicalComicStripDimensions() throws IOException {
        // Test with typical comic strip dimensions
        byte[] imageData = createTestImage(900, 300, "PNG");

        ImageValidationResult result = imageValidationService.validateWithMinDimensions(
                imageData, 100, 50);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getFormat()).isEqualTo(ImageFormat.PNG);
    }

    @Test
    void testLargeValidImage() throws IOException {
        // Test with large but valid image (under 10MB limit)
        byte[] imageData = createTestImage(2000, 2000, "PNG");

        ImageValidationResult result = imageValidationService.validate(imageData);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getWidth()).isEqualTo(2000);
        assertThat(result.getHeight()).isEqualTo(2000);
    }
}
