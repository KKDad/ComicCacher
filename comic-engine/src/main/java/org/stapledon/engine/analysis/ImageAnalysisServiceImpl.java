package org.stapledon.engine.analysis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.stapledon.common.dto.ImageMetadata;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.service.ImageAnalysisService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of ImageAnalysisService that analyzes images for metadata.
 * Uses pixel sampling to determine if an image is grayscale or color.
 */
@Slf4j
@Service
public class ImageAnalysisServiceImpl implements ImageAnalysisService {
    private final double samplePercentage;
    private final Random random;

    public ImageAnalysisServiceImpl(
            @Value("${comics.metrics.color-detection.sample-percentage:5.0}") double samplePercentage) {
        this.samplePercentage = samplePercentage;
        this.random = new Random();
    }

    @Override
    public ImageMetadata analyzeImage(File imageFile, ImageValidationResult validation, String sourceUrl) {
        if (!imageFile.exists()) {
            log.warn("Image file does not exist: {}", imageFile.getAbsolutePath());
            return buildUnknownMetadata(imageFile.getAbsolutePath(), validation, sourceUrl);
        }

        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                log.warn("Could not read image file: {}", imageFile.getAbsolutePath());
                return buildUnknownMetadata(imageFile.getAbsolutePath(), validation, sourceUrl);
            }

            ImageMetadata.ColorMode colorMode = detectColorModeFromImage(image);

            return ImageMetadata.builder()
                    .filePath(imageFile.getAbsolutePath())
                    .format(validation.getFormat())
                    .width(validation.getWidth())
                    .height(validation.getHeight())
                    .sizeInBytes(validation.getSizeInBytes())
                    .colorMode(colorMode)
                    .samplePercentage(samplePercentage)
                    .captureTimestamp(LocalDateTime.now())
                    .sourceUrl(sourceUrl)
                    .build();
        } catch (IOException e) {
            log.error("Error analyzing image file {}: {}", imageFile.getAbsolutePath(), e.getMessage());
            return buildUnknownMetadata(imageFile.getAbsolutePath(), validation, sourceUrl);
        }
    }

    @Override
    public ImageMetadata analyzeImage(byte[] imageData, String filePath, ImageValidationResult validation, String sourceUrl) {
        if (imageData == null || imageData.length == 0) {
            log.warn("Image data is null or empty for path: {}", filePath);
            return buildUnknownMetadata(filePath, validation, sourceUrl);
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
            BufferedImage image = ImageIO.read(bais);
            if (image == null) {
                log.warn("Could not read image data for path: {}", filePath);
                return buildUnknownMetadata(filePath, validation, sourceUrl);
            }

            ImageMetadata.ColorMode colorMode = detectColorModeFromImage(image);

            return ImageMetadata.builder()
                    .filePath(filePath)
                    .format(validation.getFormat())
                    .width(validation.getWidth())
                    .height(validation.getHeight())
                    .sizeInBytes(validation.getSizeInBytes())
                    .colorMode(colorMode)
                    .samplePercentage(samplePercentage)
                    .captureTimestamp(LocalDateTime.now())
                    .sourceUrl(sourceUrl)
                    .build();
        } catch (IOException e) {
            log.error("Error analyzing image data for path {}: {}", filePath, e.getMessage());
            return buildUnknownMetadata(filePath, validation, sourceUrl);
        }
    }

    @Override
    public ImageMetadata.ColorMode detectColorMode(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            return ImageMetadata.ColorMode.UNKNOWN;
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
            BufferedImage image = ImageIO.read(bais);
            if (image == null) {
                return ImageMetadata.ColorMode.UNKNOWN;
            }

            return detectColorModeFromImage(image);
        } catch (IOException e) {
            log.error("Error detecting color mode: {}", e.getMessage());
            return ImageMetadata.ColorMode.UNKNOWN;
        }
    }

    /**
     * Detects color mode by sampling a percentage of pixels from the image.
     * If any sampled pixel is colored (R != G or G != B), returns COLOR.
     * Otherwise returns GRAYSCALE.
     */
    private ImageMetadata.ColorMode detectColorModeFromImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;

        if (totalPixels == 0) {
            return ImageMetadata.ColorMode.UNKNOWN;
        }

        // Calculate number of pixels to sample
        int sampleCount = Math.max(1, (int) (totalPixels * (samplePercentage / 100.0)));

        // Sample random pixels
        for (int i = 0; i < sampleCount; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);

            int rgb = image.getRGB(x, y);

            // Extract RGB components
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;

            // If any component differs, it's a color image
            if (red != green || green != blue) {
                return ImageMetadata.ColorMode.COLOR;
            }
        }

        // All sampled pixels are grayscale
        return ImageMetadata.ColorMode.GRAYSCALE;
    }

    /**
     * Builds metadata with UNKNOWN color mode when image cannot be analyzed
     */
    private ImageMetadata buildUnknownMetadata(String filePath, ImageValidationResult validation, String sourceUrl) {
        return ImageMetadata.builder()
                .filePath(filePath)
                .format(validation.getFormat())
                .width(validation.getWidth())
                .height(validation.getHeight())
                .sizeInBytes(validation.getSizeInBytes())
                .colorMode(ImageMetadata.ColorMode.UNKNOWN)
                .samplePercentage(samplePercentage)
                .captureTimestamp(LocalDateTime.now())
                .sourceUrl(sourceUrl)
                .build();
    }
}
