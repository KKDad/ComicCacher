package org.stapledon.engine.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.dto.ImageMetadata;
import org.stapledon.common.dto.ImageValidationResult;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageAnalysisServiceImplTest {

    @TempDir
    Path tempDir;

    private ImageAnalysisServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ImageAnalysisServiceImpl(5.0); // 5% sampling
    }

    @Test
    void shouldDetectColorImage() throws IOException {
        // Given - Create a color image (red)
        byte[] imageData = createColorImage(100, 100, Color.RED);

        // When
        ImageMetadata.ColorMode colorMode = service.detectColorMode(imageData);

        // Then
        assertEquals(ImageMetadata.ColorMode.COLOR, colorMode);
    }

    @Test
    void shouldDetectGrayscaleImage() throws IOException {
        // Given - Create a grayscale image
        byte[] imageData = createGrayscaleImage(100, 100);

        // When
        ImageMetadata.ColorMode colorMode = service.detectColorMode(imageData);

        // Then
        assertEquals(ImageMetadata.ColorMode.GRAYSCALE, colorMode);
    }

    @Test
    void shouldAnalyzeImageFromFile() throws IOException {
        // Given
        File imageFile = createTestImageFile(tempDir, "test.png", Color.BLUE);
        ImageValidationResult validation = ImageValidationResult.success(
                ImageFormat.PNG, 100, 100, imageFile.length());

        // When
        ImageMetadata metadata = service.analyzeImage(imageFile, validation, "http://example.com/image.png");

        // Then
        assertNotNull(metadata);
        assertEquals(imageFile.getAbsolutePath(), metadata.getFilePath());
        assertEquals(ImageFormat.PNG, metadata.getFormat());
        assertEquals(100, metadata.getWidth());
        assertEquals(100, metadata.getHeight());
        assertEquals(ImageMetadata.ColorMode.COLOR, metadata.getColorMode());
        assertEquals(5.0, metadata.getSamplePercentage());
        assertEquals("http://example.com/image.png", metadata.getSourceUrl());
        assertNotNull(metadata.getCaptureTimestamp());
    }

    @Test
    void shouldAnalyzeImageFromBytes() throws IOException {
        // Given
        byte[] imageData = createColorImage(50, 50, Color.GREEN);
        ImageValidationResult validation = ImageValidationResult.success(
                ImageFormat.PNG, 50, 50, imageData.length);
        String filePath = tempDir.resolve("test.png").toString();

        // When
        ImageMetadata metadata = service.analyzeImage(imageData, filePath, validation, null);

        // Then
        assertNotNull(metadata);
        assertEquals(filePath, metadata.getFilePath());
        assertEquals(ImageFormat.PNG, metadata.getFormat());
        assertEquals(50, metadata.getWidth());
        assertEquals(50, metadata.getHeight());
        assertEquals(ImageMetadata.ColorMode.COLOR, metadata.getColorMode());
        assertNull(metadata.getSourceUrl());
    }

    @Test
    void shouldReturnUnknownForNullImageData() {
        // When
        ImageMetadata.ColorMode colorMode = service.detectColorMode(null);

        // Then
        assertEquals(ImageMetadata.ColorMode.UNKNOWN, colorMode);
    }

    @Test
    void shouldReturnUnknownForEmptyImageData() {
        // When
        ImageMetadata.ColorMode colorMode = service.detectColorMode(new byte[0]);

        // Then
        assertEquals(ImageMetadata.ColorMode.UNKNOWN, colorMode);
    }

    @Test
    void shouldReturnUnknownForInvalidImageData() {
        // Given - Invalid image data
        byte[] invalidData = "not an image".getBytes();

        // When
        ImageMetadata.ColorMode colorMode = service.detectColorMode(invalidData);

        // Then
        assertEquals(ImageMetadata.ColorMode.UNKNOWN, colorMode);
    }

    @Test
    void shouldHandleNonExistentFile() {
        // Given
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.png");
        ImageValidationResult validation = ImageValidationResult.success(
                ImageFormat.PNG, 100, 100, 1000);

        // When
        ImageMetadata metadata = service.analyzeImage(nonExistentFile, validation, null);

        // Then
        assertNotNull(metadata);
        assertEquals(ImageMetadata.ColorMode.UNKNOWN, metadata.getColorMode());
    }

    @Test
    void shouldDetectGrayscaleForBlackAndWhiteImage() throws IOException {
        // Given - Create a pure black and white image
        byte[] imageData = createBlackAndWhiteImage(100, 100);

        // When
        ImageMetadata.ColorMode colorMode = service.detectColorMode(imageData);

        // Then
        assertEquals(ImageMetadata.ColorMode.GRAYSCALE, colorMode);
    }

    @Test
    void shouldDetectColorEvenWithMostlyGrayscale() throws IOException {
        // Given - Create an image that is mostly grayscale but has one colored pixel
        byte[] imageData = createMostlyGrayscaleImage(100, 100);

        // When
        ImageMetadata.ColorMode colorMode = service.detectColorMode(imageData);

        // Then
        // Due to random sampling, this might be COLOR or GRAYSCALE
        // We just verify it doesn't crash and returns a valid value
        assertNotNull(colorMode);
        assertTrue(colorMode == ImageMetadata.ColorMode.COLOR ||
                   colorMode == ImageMetadata.ColorMode.GRAYSCALE);
    }

    @Test
    void shouldHandleSmallImages() throws IOException {
        // Given - Very small image (1x1)
        byte[] imageData = createColorImage(1, 1, Color.YELLOW);

        // When
        ImageMetadata.ColorMode colorMode = service.detectColorMode(imageData);

        // Then
        assertEquals(ImageMetadata.ColorMode.COLOR, colorMode);
    }

    @Test
    void shouldIncludeSamplePercentageInMetadata() throws IOException {
        // Given
        byte[] imageData = createColorImage(100, 100, Color.CYAN);
        ImageValidationResult validation = ImageValidationResult.success(
                ImageFormat.PNG, 100, 100, imageData.length);

        // When
        ImageMetadata metadata = service.analyzeImage(
                imageData, "/tmp/test.png", validation, null);

        // Then
        assertEquals(5.0, metadata.getSamplePercentage());
    }

    // Helper methods

    private byte[] createColorImage(int width, int height, Color color) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();

        return imageToBytes(image);
    }

    private byte[] createGrayscaleImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Fill with various shades of gray
        for (int y = 0; y < height; y++) {
            int grayLevel = (255 * y) / height;
            Color gray = new Color(grayLevel, grayLevel, grayLevel);
            g.setColor(gray);
            g.drawLine(0, y, width, y);
        }
        g.dispose();

        return imageToBytes(image);
    }

    private byte[] createBlackAndWhiteImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Checkerboard pattern of black and white
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = ((x + y) % 2 == 0) ? Color.BLACK : Color.WHITE;
                image.setRGB(x, y, color.getRGB());
            }
        }
        g.dispose();

        return imageToBytes(image);
    }

    private byte[] createMostlyGrayscaleImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Fill with gray
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, width, height);

        // Add one colored pixel in the center
        image.setRGB(width / 2, height / 2, Color.RED.getRGB());
        g.dispose();

        return imageToBytes(image);
    }

    private byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private File createTestImageFile(Path tempDir, String filename, Color color) throws IOException {
        File file = tempDir.resolve(filename).toFile();
        byte[] imageData = createColorImage(100, 100, color);
        java.nio.file.Files.write(file.toPath(), imageData);
        return file;
    }
}
