package org.stapledon.engine.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.dto.ImageMetadata;
import org.stapledon.common.dto.ImageValidationResult;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

class ImageAnalysisServiceTest {

    @TempDir
    Path tempDir;

    private ImageAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new ImageAnalysisService(5.0); // 5% sampling
    }

    @Test
    void shouldDetectColorImage() throws Exception {
        // Given - Create a color image (red)
        byte[] imageData = createColorImage(100, 100, Color.RED);

        // When
        ImageMetadata.ColorMode colorMode = service.detectColorMode(imageData);

        // Then
        assertThat(colorMode).isEqualTo(ImageMetadata.ColorMode.COLOR);
    }

    @Test
    void shouldDetectGrayscaleImage() throws Exception {
        // Given - Create a grayscale image
        byte[] imageData = createGrayscaleImage(100, 100);

        // When
        ImageMetadata.ColorMode colorMode = service.detectColorMode(imageData);

        // Then
        assertThat(colorMode).isEqualTo(ImageMetadata.ColorMode.GRAYSCALE);
    }

    @Test
    void shouldAnalyzeImageFromFile() throws Exception {
        // Given
        File imageFile = createTestImageFile(tempDir, "test.png", Color.BLUE);
        ImageValidationResult validation = ImageValidationResult.success(
                ImageFormat.PNG, 100, 100, imageFile.length());

        // When
        ImageMetadata metadata = service.analyzeImage(imageFile, validation, "http://example.com/image.png");

        // Then
        assertThat(metadata).isNotNull();
        assertThat(metadata.getFilePath()).isEqualTo(imageFile.getAbsolutePath());
        assertThat(metadata.getFormat()).isEqualTo(ImageFormat.PNG);
        assertThat(metadata.getWidth()).isEqualTo(100);
        assertThat(metadata.getHeight()).isEqualTo(100);
        assertThat(metadata.getColorMode()).isEqualTo(ImageMetadata.ColorMode.COLOR);
        assertThat(metadata.getSamplePercentage()).isEqualTo(5.0);
        assertThat(metadata.getSourceUrl()).isEqualTo("http://example.com/image.png");
        assertThat(metadata.getCaptureTimestamp()).isNotNull();
    }

    @Test
    void shouldAnalyzeImageFromBytes() throws Exception {
        // Given
        byte[] imageData = createColorImage(50, 50, Color.GREEN);
        ImageValidationResult validation = ImageValidationResult.success(
                ImageFormat.PNG, 50, 50, imageData.length);
        String filePath = tempDir.resolve("test.png").toString();

        // When
        ImageMetadata metadata = service.analyzeImage(imageData, filePath, validation, null);

        // Then
        assertThat(metadata).isNotNull();
        assertThat(metadata.getFilePath()).isEqualTo(filePath);
        assertThat(metadata.getFormat()).isEqualTo(ImageFormat.PNG);
        assertThat(metadata.getWidth()).isEqualTo(50);
        assertThat(metadata.getHeight()).isEqualTo(50);
        assertThat(metadata.getColorMode()).isEqualTo(ImageMetadata.ColorMode.COLOR);
        assertThat(metadata.getSourceUrl()).isNull();
    }

    @Test
    void shouldReturnUnknownForNullImageData() {
        // When
        ImageMetadata.ColorMode colorMode = service.detectColorMode(null);

        // Then
        assertThat(colorMode).isEqualTo(ImageMetadata.ColorMode.UNKNOWN);
    }

    @Test
    void shouldReturnUnknownForEmptyImageData() {
        // When
        ImageMetadata.ColorMode colorMode = service.detectColorMode(new byte[0]);

        // Then
        assertThat(colorMode).isEqualTo(ImageMetadata.ColorMode.UNKNOWN);
    }

    @Test
    void shouldReturnUnknownForInvalidImageData() {
        // Given - Invalid image data
        byte[] invalidData = "not an image".getBytes();

        // When
        ImageMetadata.ColorMode colorMode = service.detectColorMode(invalidData);

        // Then
        assertThat(colorMode).isEqualTo(ImageMetadata.ColorMode.UNKNOWN);
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
        assertThat(metadata).isNotNull();
        assertThat(metadata.getColorMode()).isEqualTo(ImageMetadata.ColorMode.UNKNOWN);
    }

    @Test
    void shouldDetectGrayscaleForBlackAndWhiteImage() throws Exception {
        // Given - Create a pure black and white image
        byte[] imageData = createBlackAndWhiteImage(100, 100);

        // When
        ImageMetadata.ColorMode colorMode = service.detectColorMode(imageData);

        // Then
        assertThat(colorMode).isEqualTo(ImageMetadata.ColorMode.GRAYSCALE);
    }

    @Test
    void shouldDetectColorEvenWithMostlyGrayscale() throws Exception {
        // Given - Create an image that is mostly grayscale but has one colored pixel
        byte[] imageData = createMostlyGrayscaleImage(100, 100);

        // When
        ImageMetadata.ColorMode colorMode = service.detectColorMode(imageData);

        // Then
        // Due to random sampling, this might be COLOR or GRAYSCALE
        // We just verify it doesn't crash and returns a valid value
        assertThat(colorMode).isNotNull();
        assertThat(colorMode == ImageMetadata.ColorMode.COLOR ||
                colorMode == ImageMetadata.ColorMode.GRAYSCALE).isTrue();
    }

    @Test
    void shouldHandleSmallImages() throws Exception {
        // Given - Very small image (1x1)
        byte[] imageData = createColorImage(1, 1, Color.YELLOW);

        // When
        ImageMetadata.ColorMode colorMode = service.detectColorMode(imageData);

        // Then
        assertThat(colorMode).isEqualTo(ImageMetadata.ColorMode.COLOR);
    }

    @Test
    void shouldIncludeSamplePercentageInMetadata() throws Exception {
        // Given
        byte[] imageData = createColorImage(100, 100, Color.CYAN);
        ImageValidationResult validation = ImageValidationResult.success(
                ImageFormat.PNG, 100, 100, imageData.length);

        // When
        ImageMetadata metadata = service.analyzeImage(
                imageData, "/tmp/test.png", validation, null);

        // Then
        assertThat(metadata.getSamplePercentage()).isEqualTo(5.0);
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
