package org.stapledon.engine.batch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ImageMetadata;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.service.ImageAnalysisService;
import org.stapledon.common.service.ImageValidationService;
import org.stapledon.engine.storage.ImageMetadataRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job to backfill image metadata for existing images that don't have metadata files.
 * Runs daily and processes images in batches to avoid overwhelming the system.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "comics.metrics.backfill.enabled", havingValue = "true", matchIfMissing = true)
public class ImageMetadataBackfillJob {
    private final CacheProperties cacheProperties;
    private final ImageValidationService imageValidationService;
    private final ImageAnalysisService imageAnalysisService;
    private final ImageMetadataRepository imageMetadataRepository;

    @Value("${comics.metrics.backfill.batch-size:100}")
    private int batchSize;

    /**
     * Runs daily at 3:00 AM to backfill metadata for images without metadata files.
     * Processes images in batches and exits gracefully if there's nothing to do.
     */
    @Scheduled(cron = "${comics.metrics.backfill.cron:0 0 3 * * ?}")
    public void backfillImageMetadata() {
        log.info("Starting image metadata backfill job");
        long startTime = System.currentTimeMillis();

        try {
            List<File> imagesToProcess = findImagesWithoutMetadata();

            if (imagesToProcess.isEmpty()) {
                log.info("No images need metadata backfill. Job complete.");
                return;
            }

            log.info("Found {} images without metadata. Processing in batches of {}",
                    imagesToProcess.size(), batchSize);

            int processed = 0;
            int successful = 0;
            int failed = 0;

            for (File imageFile : imagesToProcess) {
                try {
                    backfillImageMetadata(imageFile);
                    successful++;
                } catch (Exception e) {
                    failed++;
                    log.error("Failed to backfill metadata for {}: {}",
                            imageFile.getAbsolutePath(), e.getMessage());
                }

                processed++;

                // Log progress every batch
                if (processed % batchSize == 0) {
                    log.info("Progress: {}/{} images processed ({} successful, {} failed)",
                            processed, imagesToProcess.size(), successful, failed);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Image metadata backfill job complete. Processed {} images in {}ms ({} successful, {} failed)",
                    processed, duration, successful, failed);

        } catch (Exception e) {
            log.error("Image metadata backfill job failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Finds all image files in the cache directory that don't have associated metadata files.
     *
     * @return List of image files without metadata
     */
    private List<File> findImagesWithoutMetadata() throws IOException {
        List<File> imagesWithoutMetadata = new ArrayList<>();
        File cacheRoot = new File(cacheProperties.getLocation());

        if (!cacheRoot.exists() || !cacheRoot.isDirectory()) {
            log.warn("Cache directory does not exist or is not a directory: {}", cacheRoot.getAbsolutePath());
            return imagesWithoutMetadata;
        }

        // Walk through all subdirectories
        try (Stream<Path> paths = Files.walk(cacheRoot.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase();
                        return fileName.endsWith(".png") || fileName.endsWith(".jpg") ||
                               fileName.endsWith(".jpeg") || fileName.endsWith(".gif") ||
                               fileName.endsWith(".tif") || fileName.endsWith(".tiff") ||
                               fileName.endsWith(".bmp") || fileName.endsWith(".webp");
                    })
                    .filter(path -> {
                        // Exclude avatar files
                        String fileName = path.getFileName().toString();
                        return !fileName.equals("avatar.png");
                    })
                    .filter(path -> {
                        // Check if metadata file exists
                        String imagePath = path.toString();
                        return !imageMetadataRepository.metadataExists(imagePath);
                    })
                    .forEach(path -> imagesWithoutMetadata.add(path.toFile()));
        }

        return imagesWithoutMetadata;
    }

    /**
     * Backfills metadata for a single image file.
     *
     * @param imageFile The image file to process
     */
    private void backfillImageMetadata(File imageFile) throws IOException {
        // Read the image file
        byte[] imageData = Files.readAllBytes(imageFile.toPath());

        // Validate the image
        ImageValidationResult validation = imageValidationService.validate(imageData);
        if (!validation.isValid()) {
            log.warn("Skipping invalid image during backfill: {} - {}",
                    imageFile.getAbsolutePath(), validation.getErrorMessage());
            return;
        }

        // Analyze and create metadata
        ImageMetadata metadata = imageAnalysisService.analyzeImage(
                imageData, imageFile.getAbsolutePath(), validation, null);

        // Save metadata - will return false if metadata is invalid
        boolean saved = imageMetadataRepository.saveMetadata(metadata);
        if (saved) {
            log.debug("Backfilled metadata for: {}", imageFile.getAbsolutePath());
        } else {
            // Metadata was invalid (UNKNOWN format, 0 dimensions, etc.)
            // This allows the backfill job to try again on next run
            String error = String.format("Metadata validation failed for %s: format=%s, dimensions=%dx%d, size=%d",
                    imageFile.getName(), metadata.getFormat(), metadata.getWidth(),
                    metadata.getHeight(), metadata.getSizeInBytes());
            log.error("Backfill failed for {}: {}", imageFile.getAbsolutePath(), error);
            throw new IOException(error);
        }
    }
}
