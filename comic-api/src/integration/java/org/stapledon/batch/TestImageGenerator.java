package org.stapledon.batch;

import org.stapledon.common.dto.ImageFormat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for generating test images for batch integration tests.
 * Creates realistic images in various formats for testing image metadata processing.
 */
@Slf4j
public class TestImageGenerator {

    /**
     * Creates a test image with specified dimensions and format.
     *
     * @param outputFile The file to write the image to
     * @param width Image width in pixels
     * @param height Image height in pixels
     * @param format Image format (PNG, JPEG, GIF)
     * @throws IOException if image creation or writing fails
     */
    public static void createTestImage(File outputFile, int width, int height, ImageFormat format) throws IOException {
        BufferedImage image = createBufferedImage(width, height, format);
        writeImage(image, outputFile, format);
        log.debug("Created test image: {} ({}x{}, {})", outputFile.getAbsolutePath(), width, height, format);
    }

    /**
     * Creates a BufferedImage with a gradient pattern for testing.
     *
     * @param width Image width
     * @param height Image height
     * @param format Image format (determines if grayscale or color)
     * @return BufferedImage with test pattern
     */
    private static BufferedImage createBufferedImage(int width, int height, ImageFormat format) {
        // Use RGB for color formats, GRAY for efficiency where possible
        int imageType = (format == ImageFormat.JPEG || format == ImageFormat.PNG)
            ? BufferedImage.TYPE_INT_RGB
            : BufferedImage.TYPE_INT_RGB;

        BufferedImage image = new BufferedImage(width, height, imageType);
        Graphics2D g2d = image.createGraphics();

        // Draw a gradient background (left to right, blue to orange)
        for (int x = 0; x < width; x++) {
            float ratio = (float) x / width;
            int red = (int) (ratio * 255);
            int blue = (int) ((1 - ratio) * 255);
            g2d.setColor(new Color(red, 100, blue));
            g2d.drawLine(x, 0, x, height);
        }

        // Draw some test patterns
        g2d.setColor(Color.WHITE);
        g2d.fillRect(width / 4, height / 4, width / 2, height / 2);

        g2d.setColor(Color.BLACK);
        g2d.drawRect(width / 4, height / 4, width / 2, height / 2);
        g2d.drawLine(0, 0, width, height);
        g2d.drawLine(width, 0, 0, height);

        g2d.dispose();
        return image;
    }

    /**
     * Writes a BufferedImage to a file in the specified format.
     *
     * @param image The image to write
     * @param outputFile The output file
     * @param format The image format
     * @throws IOException if writing fails
     */
    private static void writeImage(BufferedImage image, File outputFile, ImageFormat format) throws IOException {
        String formatName = switch (format) {
            case PNG -> "png";
            case JPEG -> "jpg";
            case GIF -> "gif";
            case WEBP -> "webp";
            case TIFF -> "tiff";
            case BMP -> "bmp";
            default -> "png";
        };

        // Ensure parent directory exists
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }

        boolean written = ImageIO.write(image, formatName, outputFile);
        if (!written) {
            throw new IOException("Failed to write image format: " + formatName);
        }
    }

    /**
     * Creates a corrupted/invalid image file for testing error handling.
     *
     * @param outputFile The file to write the corrupted data to
     * @throws IOException if writing fails
     */
    public static void createInvalidImage(File outputFile) throws IOException {
        // Write invalid data (not a valid image format)
        byte[] corruptData = "This is not a valid image file".getBytes();

        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }

        Files.write(outputFile.toPath(), corruptData);
        log.debug("Created invalid image file: {}", outputFile.getAbsolutePath());
    }

    /**
     * Creates a truncated PNG file (incomplete) for testing validation.
     *
     * @param outputFile The file to write the truncated image to
     * @throws IOException if writing fails
     */
    public static void createTruncatedImage(File outputFile) throws IOException {
        // Create a valid small image first
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);

        // Truncate it to simulate corruption
        byte[] fullData = baos.toByteArray();
        byte[] truncatedData = new byte[fullData.length / 2]; // Only half the data
        System.arraycopy(fullData, 0, truncatedData, 0, truncatedData.length);

        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }

        Files.write(outputFile.toPath(), truncatedData);
        log.debug("Created truncated image file: {}", outputFile.getAbsolutePath());
    }
}
