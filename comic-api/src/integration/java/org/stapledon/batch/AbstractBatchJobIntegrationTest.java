package org.stapledon.batch;

import tools.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.stapledon.ComicApiApplication;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.dto.ImageMetadata;
import org.stapledon.engine.batch.JsonBatchExecutionTracker;
import org.stapledon.engine.batch.dto.BatchExecutionSummary;
import org.stapledon.engine.storage.ImageMetadataRepository;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base class for batch job integration tests.
 * Provides utilities for testing Spring Batch jobs in isolation.
 */
@Slf4j
@SpringBootTest(classes = ComicApiApplication.class)
@ActiveProfiles("batch-integration")
@Getter
public abstract class AbstractBatchJobIntegrationTest {

    @Autowired
    protected CacheProperties cacheProperties;

    @Autowired
    protected ImageMetadataRepository imageMetadataRepository;

    @Autowired
    @Qualifier("gsonWithLocalDate")
    protected Gson gson;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final String BATCH_CACHE_DIR = "./batch-integration-cache";

    /**
     * Clean up and create fresh batch integration cache directory before all tests.
     */
    @BeforeAll
    static void setupBatchCache() {
        try {
            Path batchCacheDir = Paths.get(BATCH_CACHE_DIR);

            // Clean up existing directory
            if (Files.exists(batchCacheDir)) {
                log.info("Cleaning up existing batch integration cache: {}", batchCacheDir);
                try (Stream<Path> walk = Files.walk(batchCacheDir)) {
                    walk.sorted(java.util.Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
            }

            // Create fresh directory structure
            log.info("Creating batch integration cache directory: {}", batchCacheDir);
            Files.createDirectories(batchCacheDir);

            // Create required JSON config files
            createEmptyJsonFile(batchCacheDir.resolve("comics.json"));
            createEmptyJsonFile(batchCacheDir.resolve("users.json"));
            createEmptyJsonFile(batchCacheDir.resolve("preferences.json"));
            createEmptyJsonFile(batchCacheDir.resolve("bootstrap.json"));

        } catch (IOException e) {
            log.error("Failed to setup batch integration cache: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to setup batch integration cache", e);
        }
    }

    /**
     * Creates an empty JSON file (containing "{}")
     */
    private static void createEmptyJsonFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            log.info("Creating empty JSON file: {}", path);
            Files.write(path, "{}".getBytes());
        }
    }

    /**
     * Clean up test data before each test to ensure isolation.
     */
    @BeforeEach
    void cleanupTestData() {
        log.info("Cleaning up test data before test");
        // Subclasses can override to add specific cleanup
    }

    // ==================== Test Image Helpers ====================

    /**
     * Creates a test image file with specified dimensions and format.
     *
     * @param relativePath Path relative to batch cache directory (e.g.,
     *                     "TestComic/2024/2024-01-15.png")
     * @param width        Image width in pixels
     * @param height       Image height in pixels
     * @param format       Image format
     * @return The created File object
     * @throws IOException if image creation fails
     */
    protected File createTestImage(String relativePath, int width, int height, ImageFormat format) throws IOException {
        Path batchCacheDir = Paths.get(BATCH_CACHE_DIR);
        File outputFile = batchCacheDir.resolve(relativePath).toFile();

        TestImageGenerator.createTestImage(outputFile, width, height, format);

        return outputFile;
    }

    /**
     * Creates an invalid/corrupted image file for testing error handling.
     *
     * @param relativePath Path relative to batch cache directory
     * @return The created File object
     * @throws IOException if file creation fails
     */
    protected File createInvalidImage(String relativePath) throws IOException {
        Path batchCacheDir = Paths.get(BATCH_CACHE_DIR);
        File outputFile = batchCacheDir.resolve(relativePath).toFile();

        TestImageGenerator.createInvalidImage(outputFile);

        return outputFile;
    }

    // ==================== Metadata Assertion Helpers ====================

    /**
     * Asserts that a metadata JSON file exists for the given image.
     *
     * @param imageFilePath Absolute path to the image file
     */
    protected void assertMetadataExists(String imageFilePath) {
        File metadataFile = getMetadataFile(imageFilePath);
        assertThat(metadataFile.exists()).as("Metadata file should exist: " + metadataFile.getAbsolutePath()).isTrue();
    }

    /**
     * Asserts that NO metadata JSON file exists for the given image.
     *
     * @param imageFilePath Absolute path to the image file
     */
    protected void assertMetadataNotExists(String imageFilePath) {
        File metadataFile = getMetadataFile(imageFilePath);
        assertThat(metadataFile.exists()).as("Metadata file should NOT exist: " + metadataFile.getAbsolutePath())
                .isFalse();
    }

    /**
     * Asserts that metadata exists and validates its content.
     *
     * @param imageFilePath  Absolute path to the image file
     * @param expectedFormat Expected image format
     * @param expectedWidth  Expected image width
     * @param expectedHeight Expected image height
     * @throws IOException if reading metadata fails
     */
    protected void assertMetadataValid(String imageFilePath, ImageFormat expectedFormat,
            int expectedWidth, int expectedHeight) throws IOException {
        assertMetadataExists(imageFilePath);

        File metadataFile = getMetadataFile(imageFilePath);
        try (FileReader reader = new FileReader(metadataFile)) {
            ImageMetadata metadata = gson.fromJson(reader, ImageMetadata.class);

            assertThat(metadata).as("Metadata should not be null").isNotNull();
            assertThat(metadata.getFormat()).as("Image format mismatch").isEqualTo(expectedFormat);
            assertThat(metadata.getWidth()).as("Image width mismatch").isEqualTo(expectedWidth);
            assertThat(metadata.getHeight()).as("Image height mismatch").isEqualTo(expectedHeight);
            assertThat(metadata.getSizeInBytes() > 0).as("Image size should be > 0").isTrue();
            assertThat(metadata.getColorMode()).as("ColorMode should not be null").isNotNull();
            assertThat(metadata.getFilePath()).as("FilePath should not be null").isNotNull();
        }
    }

    /**
     * Loads metadata from the JSON file for inspection.
     *
     * @param imageFilePath Absolute path to the image file
     * @return ImageMetadata object, or null if file doesn't exist
     * @throws IOException if reading fails
     */
    protected ImageMetadata loadMetadata(String imageFilePath) throws IOException {
        File metadataFile = getMetadataFile(imageFilePath);

        if (!metadataFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(metadataFile)) {
            return gson.fromJson(reader, ImageMetadata.class);
        }
    }

    /**
     * Deletes the metadata JSON file for the given image (for setup/cleanup).
     *
     * @param imageFilePath Absolute path to the image file
     */
    protected void deleteMetadataFile(String imageFilePath) {
        File metadataFile = getMetadataFile(imageFilePath);
        if (metadataFile.exists()) {
            boolean deleted = metadataFile.delete();
            log.debug("Deleted metadata file: {} (success={})", metadataFile.getAbsolutePath(), deleted);
        }
    }

    /**
     * Gets the metadata file path for an image file.
     *
     * @param imageFilePath Absolute path to the image file
     * @return The corresponding metadata file
     */
    private File getMetadataFile(String imageFilePath) {
        String metadataPath = imageFilePath.replaceAll("\\.(png|jpg|jpeg|gif|tif|tiff|bmp|webp)$", ".json");

        if (metadataPath.equals(imageFilePath)) {
            metadataPath = imageFilePath + ".json";
        }

        return new File(metadataPath);
    }

    // ==================== Job Execution Helpers ====================

    /**
     * Verifies that the batch-executions.json tracking file contains an entry for
     * the given job.
     *
     * @param jobName The name of the job to look for
     * @throws IOException if reading the tracking file fails
     */
    protected void assertBatchExecutionTracked(String jobName) throws IOException {
        File trackingFile = Paths.get(BATCH_CACHE_DIR, "batch-executions.json").toFile();
        assertThat(trackingFile.exists()).as("Batch execution tracking file should exist").isTrue();

        String content = Files.readString(trackingFile.toPath());
        assertThat(content.contains(jobName)).as("Batch execution tracking should contain job: " + jobName).isTrue();
    }

    /**
     * Loads and parses the batch execution summary for a specific job from
     * batch-executions.json.
     *
     * @param jobName The name of the job to load
     * @return BatchExecutionSummary for the job
     * @throws IOException if reading or parsing fails
     */
    protected BatchExecutionSummary loadBatchExecutionSummary(String jobName) throws IOException {
        File trackingFile = Paths.get(BATCH_CACHE_DIR, "batch-executions.json").toFile();
        assertThat(trackingFile.exists()).as("Batch execution tracking file should exist").isTrue();

        String content = Files.readString(trackingFile.toPath());

        // Parse the entire map
        java.util.Map<String, BatchExecutionSummary> executions = objectMapper.readValue(content,
                objectMapper.getTypeFactory().constructMapType(
                        java.util.HashMap.class, String.class, BatchExecutionSummary.class));

        BatchExecutionSummary summary = executions.get(jobName);
        assertThat(summary).as("Should have execution summary for job: " + jobName).isNotNull();

        return summary;
    }

    /**
     * Validates key fields in the batch execution summary for a job.
     *
     * @param jobName                The job name to validate
     * @param expectedStatus         Expected status (e.g., "COMPLETED", "FAILED")
     * @throws IOException if reading or parsing fails
     */
    protected void assertBatchExecutionValid(String jobName, String expectedStatus) throws IOException {
        BatchExecutionSummary summary = loadBatchExecutionSummary(jobName);

        // Validate status
        assertEquals(expectedStatus, summary.getStatus(),
                "Job status should be " + expectedStatus);
        assertEquals(expectedStatus, summary.getExitCode(),
                "Exit code should be " + expectedStatus);

        // Validate execution metadata
        assertNotNull(summary.getLastExecutionId(), "Execution ID should not be null");
        assertNotNull(summary.getStartTime(), "Start time should not be null");
        assertNotNull(summary.getEndTime(), "End time should not be null");

        log.info("Batch execution validation passed for {}: status={}", jobName, summary.getStatus());
    }
}
