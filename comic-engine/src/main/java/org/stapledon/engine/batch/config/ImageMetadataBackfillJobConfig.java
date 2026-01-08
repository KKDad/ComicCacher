package org.stapledon.engine.batch.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ImageMetadata;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.service.AnalysisService;
import org.stapledon.common.service.ComicConfigurationService;
import org.stapledon.common.service.ValidationService;
import org.stapledon.engine.batch.JsonBatchExecutionTracker;
import org.stapledon.engine.batch.scheduler.DailyJobScheduler;
import org.stapledon.engine.storage.ImageMetadataRepository;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
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
@ConditionalOnProperty(name = "batch.image-backfill.enabled", havingValue = "true", matchIfMissing = true)
public class ImageMetadataBackfillJobConfig {

    private static final int UNKNOWN_COMIC_ID = 0;

    private final CacheProperties cacheProperties;
    private final ValidationService imageValidationService;
    private final AnalysisService imageAnalysisService;
    private final ImageMetadataRepository imageMetadataRepository;
    private final ComicConfigurationService comicConfigurationService;

    private final Map<String, ComicItem> comicDirectoryMap = new HashMap<>();

    @Value("${batch.image-backfill.batch-size:100}")
    private int batchSize;

    @Value("${batch.image-backfill.cron}")
    private String cronExpression;

    @Value("${batch.timezone:America/Toronto}")
    private String timezone;

    @PostConstruct
    public void init() {
        comicConfigurationService.loadComicConfig().getItems().values().forEach(comic -> {
            String dirName = comic.getName().replace(" ", "");
            comicDirectoryMap.put(dirName.toLowerCase(), comic);
        });
        log.info("Initialized comic directory map with {} entries", comicDirectoryMap.size());
    }

    /**
     * Scheduler for ImageMetadataBackfillJob - runs daily at configured cron time.
     * Triggered by SchedulerTriggers component.
     */
    @Bean
    public DailyJobScheduler imageMetadataBackfillJobScheduler(
            JobOperator jobOperator,
            JsonBatchExecutionTracker tracker) {
        return new DailyJobScheduler(
                "ImageMetadataBackfillJob", cronExpression, timezone, jobOperator, tracker);
    }

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
     * Tasklet that performs the actual image metadata backfill.
     * Uses streaming to avoid loading all file paths into memory.
     */
    @Bean
    public Tasklet imageBackfillTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting image metadata backfill");

            long startTime = System.currentTimeMillis();
            File cacheRoot = new File(cacheProperties.getLocation());

            if (!cacheRoot.exists() || !cacheRoot.isDirectory()) {
                log.warn("Cache directory does not exist or is not a directory: {}",
                        cacheRoot.getAbsolutePath());
                return RepeatStatus.FINISHED;
            }

            // Process in streaming fashion with configurable max limit
            int maxToProcess = batchSize * 100; // Process up to 100 batches per run
            int[] counters = {0, 0, 0}; // processed, successful, failed

            try (Stream<Path> paths = Files.walk(cacheRoot.toPath())) {
                paths.filter(Files::isRegularFile)
                        .filter(this::isImageFile)
                        .filter(path -> !path.getFileName().toString().equals("avatar.png"))
                        .filter(path -> !imageMetadataRepository.metadataExists(path.toString()))
                        .limit(maxToProcess)
                        .forEach(path -> {
                            try {
                                backfillImageMetadata(path.toFile());
                                counters[1]++;
                            } catch (Exception e) {
                                counters[2]++;
                                log.error("Failed to backfill metadata for {}: {}",
                                        path.toAbsolutePath(), e.getMessage());
                            }

                            counters[0]++;

                            // Log progress every batch
                            if (counters[0] % batchSize == 0) {
                                log.info("Progress: {} images processed ({} successful, {} failed)",
                                        counters[0], counters[1], counters[2]);
                            }
                        });
            }

            if (counters[0] == 0) {
                log.info("No images need metadata backfill. Job complete.");
            } else {
                long duration = System.currentTimeMillis() - startTime;
                log.info("Image metadata backfill complete. Processed {} images in {}ms ({} successful, {} failed)",
                        counters[0], duration, counters[1], counters[2]);

                if (counters[0] >= maxToProcess) {
                    log.info("Reached max limit of {} images per run. More images may need processing.",
                            maxToProcess);
                }
            }

            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Checks if a path is an image file based on extension.
     */
    private boolean isImageFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".png") || fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg") || fileName.endsWith(".gif")
                || fileName.endsWith(".tif") || fileName.endsWith(".tiff")
                || fileName.endsWith(".bmp") || fileName.endsWith(".webp");
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

        // Normalize paths to absolute for comparison
        Path rootPath = Paths.get(cacheProperties.getLocation()).toAbsolutePath().normalize();
        Path filePath = imageFile.toPath().toAbsolutePath().normalize();

        if (!filePath.startsWith(rootPath)) {
            log.warn("Image file {} is not under cache root {}", filePath, rootPath);
            return;
        }

        // Identify comic from path using NIO
        Path relativePath = rootPath.relativize(filePath);

        // Expected format: ComicName/Year/Date.png
        if (relativePath.getNameCount() < 1) {
            log.warn("Cannot determine comic name from path: {}", relativePath);
            return;
        }

        String comicDirName = relativePath.getName(0).toString();

        int comicId = UNKNOWN_COMIC_ID;
        String comicName = comicDirName;

        // O(1) Lookup
        ComicItem comicItem = comicDirectoryMap.get(comicDirName.toLowerCase());
        if (comicItem != null) {
            comicId = comicItem.getId();
            comicName = comicItem.getName();
        }

        // Analyze and create metadata
        ImageMetadata metadata = imageAnalysisService.analyzeImage(
                comicId, comicName, imageData, imageFile.getAbsolutePath(), validation, null);

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
