package org.stapledon.engine.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ImageMetadata;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.service.AnalysisService;
import org.stapledon.common.service.ValidationService;
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
 * Spring Batch configuration for image metadata backfill job.
 * Backfills metadata for existing images that don't have metadata files.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ImageMetadataBackfillJobConfig {

    private final CacheProperties cacheProperties;
    private final ValidationService imageValidationService;
    private final AnalysisService imageAnalysisService;
    private final ImageMetadataRepository imageMetadataRepository;

    @Value("${batch.image-backfill.batch-size:100}")
    private int batchSize;

    /**
     * Job for backfilling image metadata
     */
    @Bean
    public Job imageMetadataBackfillJob(
            JobRepository jobRepository,
            @Qualifier("imageBackfillStep") Step imageBackfillStep,
            JsonBatchExecutionTracker jsonBatchExecutionTracker) {

        return new JobBuilder("ImageMetadataBackfillJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jsonBatchExecutionTracker)
                .start(imageBackfillStep)
                .build();
    }

    /**
     * Step for performing image metadata backfill
     */
    @Bean
    public Step imageBackfillStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {

        return new StepBuilder("imageBackfillStep", jobRepository)
                .tasklet(imageBackfillTasklet(), transactionManager)
                .build();
    }

    /**
     * Tasklet that performs the actual image metadata backfill
     */
    @Bean
    public Tasklet imageBackfillTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting image metadata backfill");

            long startTime = System.currentTimeMillis();
            List<File> imagesToProcess = findImagesWithoutMetadata();

            if (imagesToProcess.isEmpty()) {
                log.info("No images need metadata backfill. Job complete.");
                return RepeatStatus.FINISHED;
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
            log.info("Image metadata backfill complete. Processed {} images in {}ms ({} successful, {} failed)",
                    processed, duration, successful, failed);

            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Finds all image files in the cache directory that don't have associated
     * metadata files.
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
                        return fileName.endsWith(".png") || fileName.endsWith(".jpg")
                                || fileName.endsWith(".jpeg") || fileName.endsWith(".gif")
                                || fileName.endsWith(".tif") || fileName.endsWith(".tiff")
                                || fileName.endsWith(".bmp") || fileName.endsWith(".webp");
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
            String error = String.format("Metadata validation failed for %s: format=%s, dimensions=%dx%d, size=%d",
                    imageFile.getName(), metadata.getFormat(), metadata.getWidth(),
                    metadata.getHeight(), metadata.getSizeInBytes());
            log.error("Backfill failed for {}: {}", imageFile.getAbsolutePath(), error);
            throw new IOException(error);
        }
    }
}
