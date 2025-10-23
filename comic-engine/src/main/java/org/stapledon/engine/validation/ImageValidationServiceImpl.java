package org.stapledon.engine.validation;

import org.springframework.stereotype.Component;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.service.ImageValidationService;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of ImageValidationService that validates image data using Java's standard ImageIO.
 * Ensures only well-formed images are saved to the comic cache.
 *
 * This service validates:
 * - Image data is not null or empty
 * - Image size is within acceptable limits
 * - Image can be successfully decoded by ImageIO
 * - Image has valid dimensions (width and height > 0)
 * - Image format can be identified (PNG, JPEG, GIF, WEBP, etc.)
 */
@Slf4j
@Component
public class ImageValidationServiceImpl implements ImageValidationService {

    private static final int MIN_COMIC_WIDTH = 100;
    private static final int MIN_COMIC_HEIGHT = 50;
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    public ImageValidationResult validate(byte[] imageData) {
        // 1. Null/empty check
        if (imageData == null || imageData.length == 0) {
            return ImageValidationResult.failure("Image data is null or empty");
        }

        // 2. Size check
        if (imageData.length > MAX_IMAGE_SIZE) {
            return ImageValidationResult.failure(
                    String.format("Image exceeds maximum size of %d bytes (actual: %d bytes)",
                            MAX_IMAGE_SIZE, imageData.length));
        }

        // 3. Try to read with ImageIO (WebP supported via TwelveMonkeys plugin if present)
        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageData)) {
            BufferedImage image = ImageIO.read(bis);

            if (image == null) {
                return ImageValidationResult.failure(
                        "ImageIO could not decode image data - may be corrupted or unsupported format");
            }

            // 4. Validate dimensions
            int width = image.getWidth();
            int height = image.getHeight();

            if (width <= 0 || height <= 0) {
                return ImageValidationResult.failure(
                        String.format("Invalid dimensions: %dx%d", width, height));
            }

            // 5. Determine format from image readers
            ImageFormat format = determineFormat(imageData);

            log.debug("Image validation successful: format={}, size={}x{}, bytes={}",
                    format, width, height, imageData.length);

            return ImageValidationResult.success(format, width, height, imageData.length);

        } catch (IOException e) {
            log.error("Image validation failed: {}", e.getMessage());
            return ImageValidationResult.failure("Failed to read image: " + e.getMessage());
        }
    }

    @Override
    public ImageValidationResult validateWithMinDimensions(byte[] imageData,
                                                          int minWidth,
                                                          int minHeight) {
        ImageValidationResult result = validate(imageData);

        if (!result.isValid()) {
            return result;
        }

        if (result.getWidth() < minWidth || result.getHeight() < minHeight) {
            return ImageValidationResult.failure(
                    String.format("Image dimensions %dx%d below minimum %dx%d",
                            result.getWidth(), result.getHeight(), minWidth, minHeight));
        }

        return result;
    }

    @Override
    public boolean isValidImage(byte[] imageData) {
        return validate(imageData).isValid();
    }

    /**
     * Determines the image format by inspecting the image readers registered with ImageIO.
     * This works for standard formats (PNG, JPEG, GIF) and any additional formats
     * registered via ImageIO plugins (e.g., WebP through TwelveMonkeys).
     *
     * @param imageData The raw image bytes
     * @return The detected ImageFormat, or UNKNOWN if format cannot be determined
     */
    private ImageFormat determineFormat(byte[] imageData) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
             ImageInputStream iis = ImageIO.createImageInputStream(bis)) {

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                String formatName = reader.getFormatName().toUpperCase();
                reader.dispose();

                return switch (formatName) {
                    case "PNG" -> ImageFormat.PNG;
                    case "JPEG", "JPG" -> ImageFormat.JPEG;
                    case "GIF" -> ImageFormat.GIF;
                    case "TIFF", "TIF" -> ImageFormat.TIFF;
                    case "BMP" -> ImageFormat.BMP;
                    case "WEBP" -> ImageFormat.WEBP;
                    default -> {
                        log.warn("Unknown image format detected: {}", formatName);
                        yield ImageFormat.UNKNOWN;
                    }
                };
            }
        } catch (IOException e) {
            log.warn("Could not determine image format: {}", e.getMessage());
        }

        return ImageFormat.UNKNOWN;
    }
}
