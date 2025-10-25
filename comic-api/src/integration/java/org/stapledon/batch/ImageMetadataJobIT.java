package org.stapledon.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.dto.ImageMetadata;
import org.stapledon.engine.batch.ImageMetadataJobScheduler;

import java.io.File;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ImageMetadataJob.
 * Tests the complete flow of metadata generation for images without metadata files.
 */
@Slf4j
class ImageMetadataJobIT extends AbstractBatchJobIntegrationTest {

    @Autowired
    private ImageMetadataJobScheduler imageMetadataJobScheduler;

    private File testImage1;
    private File testImage2;
    private File testImage3;

    @BeforeEach
    void setupTestImages() throws IOException {
        log.info("Setting up test images for ImageMetadataJob tests");

        // Create test images in different formats
        testImage1 = createTestImage("TestComic/2024/2024-01-15.png", 800, 600, ImageFormat.PNG);
        testImage2 = createTestImage("TestComic/2024/2024-01-16.jpg", 1024, 768, ImageFormat.JPEG);
        testImage3 = createTestImage("TestComic/2024/2024-01-17.gif", 400, 300, ImageFormat.GIF);

        // Ensure no metadata files exist before test
        deleteMetadataFile(testImage1.getAbsolutePath());
        deleteMetadataFile(testImage2.getAbsolutePath());
        deleteMetadataFile(testImage3.getAbsolutePath());

        log.info("Created {} test images without metadata", 3);
    }

    /**
     * Test: ImageMetadataJob processes all images without metadata and creates valid metadata files.
     *
     * Given: 3 test images exist without metadata files
     * When: ImageMetadataJob is executed
     * Then: All 3 images get metadata JSON files with correct content
     */
    @Test
    void testImageMetadataJobProcessesImagesWithoutMetadata() throws Exception {
        log.info("TEST: ImageMetadataJob processes images without metadata");

        // Verify before state - no metadata files
        assertMetadataNotExists(testImage1.getAbsolutePath());
        assertMetadataNotExists(testImage2.getAbsolutePath());
        assertMetadataNotExists(testImage3.getAbsolutePath());

        // Execute the job synchronously via scheduler interface method
        log.info("Executing ImageMetadataJob via scheduler");
        JobExecution jobExecution = imageMetadataJobScheduler.runImageMetadataJob("TEST");

        // Assert job completed successfully
        assertNotNull(jobExecution, "JobExecution should not be null");
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus(),
            "Job should complete successfully");
        log.info("Job completed with status: {}", jobExecution.getStatus());

        // Verify after state - all metadata files exist with correct data
        assertMetadataValid(testImage1.getAbsolutePath(), ImageFormat.PNG, 800, 600);
        assertMetadataValid(testImage2.getAbsolutePath(), ImageFormat.JPEG, 1024, 768);
        assertMetadataValid(testImage3.getAbsolutePath(), ImageFormat.GIF, 400, 300);

        // Verify metadata content in detail for first image
        ImageMetadata metadata1 = loadMetadata(testImage1.getAbsolutePath());
        assertNotNull(metadata1.getColorMode(), "ColorMode should not be null");
        assertNotEquals(ImageMetadata.ColorMode.UNKNOWN, metadata1.getColorMode(),
            "ColorMode should be detected (not UNKNOWN)");
        assertTrue(metadata1.getSizeInBytes() > 0, "File size should be > 0");
        assertTrue(metadata1.getFilePath().endsWith("2024-01-15.png"), "File path should be correct");
        assertEquals(metadata1.getFormat(), ImageFormat.PNG, "Format should match");

        // Verify job execution details
        assertNotNull(jobExecution.getExitStatus(), "Exit status should not be null");
        assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode(), "Exit code should be COMPLETED");

        // Verify JsonBatchExecutionTracker recorded the execution
        assertBatchExecutionTracked("ImageMetadataBackfillJob");
        assertBatchExecutionValid("ImageMetadataBackfillJob", "COMPLETED", null); // Tasklet job, no item counts

        log.info("SUCCESS: All 3 images have valid metadata and execution tracked");
    }

    /**
     * Test: ImageMetadataJob skips images that already have metadata.
     *
     * Given: 2 images exist, one with metadata, one without
     * When: ImageMetadataJob is executed
     * Then: Only the image without metadata is processed
     */
    @Test
    void testImageMetadataJobSkipsImagesWithExistingMetadata() throws Exception {
        log.info("TEST: ImageMetadataJob skips images with existing metadata");

        // Setup: Create metadata for image1, leave image2 without metadata
        ImageMetadata existingMetadata = ImageMetadata.builder()
            .filePath(testImage1.getAbsolutePath())
            .format(ImageFormat.PNG)
            .width(800)
            .height(600)
            .sizeInBytes(12345L)
            .colorMode(ImageMetadata.ColorMode.COLOR)
            .build();

        boolean saved = imageMetadataRepository.saveMetadata(existingMetadata);
        assertTrue(saved, "Should save existing metadata for test setup");

        // Verify before state
        assertMetadataExists(testImage1.getAbsolutePath());
        assertMetadataNotExists(testImage2.getAbsolutePath());

        // Execute the job
        JobExecution jobExecution = imageMetadataJobScheduler.runImageMetadataJob("TEST");

        // Assert job completed successfully
        assertNotNull(jobExecution, "JobExecution should not be null");
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus(),
            "Job should complete successfully");
        assertNotNull(jobExecution.getExitStatus(), "Exit status should not be null");
        assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode(),
            "Exit code should be COMPLETED");

        // Verify image1 metadata was NOT changed (still has test size)
        ImageMetadata metadata1After = loadMetadata(testImage1.getAbsolutePath());
        assertNotNull(metadata1After, "Metadata should exist");
        assertEquals(12345L, metadata1After.getSizeInBytes(),
            "Existing metadata should not be overwritten");
        assertEquals(ImageFormat.PNG, metadata1After.getFormat(),
            "Format should remain unchanged");
        assertEquals(800, metadata1After.getWidth(), "Width should remain unchanged");
        assertEquals(600, metadata1After.getHeight(), "Height should remain unchanged");

        // Verify image2 got new metadata
        assertMetadataValid(testImage2.getAbsolutePath(), ImageFormat.JPEG, 1024, 768);

        log.info("SUCCESS: Job skipped image with existing metadata");
    }

    /**
     * Test: ImageMetadataJob handles invalid images gracefully.
     *
     * Given: Mix of valid and invalid images
     * When: ImageMetadataJob is executed
     * Then: Job completes, valid images get metadata, invalid images are skipped
     */
    @Test
    void testImageMetadataJobHandlesInvalidImages() throws Exception {
        log.info("TEST: ImageMetadataJob handles invalid images");

        // Create an invalid image
        File invalidImage = createInvalidImage("TestComic/2024/invalid.png");

        // Verify before state
        assertMetadataNotExists(testImage1.getAbsolutePath());
        assertMetadataNotExists(invalidImage.getAbsolutePath());

        // Execute the job
        JobExecution jobExecution = imageMetadataJobScheduler.runImageMetadataJob("TEST");

        // Assert job completed successfully even with invalid images
        assertNotNull(jobExecution, "JobExecution should not be null");
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus(),
            "Job should complete even with invalid images");
        assertNotNull(jobExecution.getExitStatus(), "Exit status should not be null");
        assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode(),
            "Exit code should be COMPLETED");

        // Valid image should have metadata with correct values
        assertMetadataValid(testImage1.getAbsolutePath(), ImageFormat.PNG, 800, 600);
        ImageMetadata validMetadata = loadMetadata(testImage1.getAbsolutePath());
        assertNotNull(validMetadata.getColorMode(), "ColorMode should not be null");
        assertTrue(validMetadata.getSizeInBytes() > 0, "File size should be > 0");

        // Invalid image should NOT have metadata
        assertMetadataNotExists(invalidImage.getAbsolutePath());

        log.info("SUCCESS: Job handled invalid image gracefully");
    }

    /**
     * Test: ImageMetadataJob processes images with various formats correctly.
     *
     * Given: Images in PNG, JPEG, and GIF formats
     * When: ImageMetadataJob is executed
     * Then: Correct format is detected for each image
     */
    @Test
    void testImageMetadataJobDetectsFormatsCorrectly() throws Exception {
        log.info("TEST: ImageMetadataJob format detection");

        // Execute the job
        JobExecution jobExecution = imageMetadataJobScheduler.runImageMetadataJob("TEST");
        assertNotNull(jobExecution, "JobExecution should not be null");
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus(),
            "Job should complete successfully");

        // Load and verify format for PNG image
        ImageMetadata pngMetadata = loadMetadata(testImage1.getAbsolutePath());
        assertNotNull(pngMetadata, "PNG metadata should exist");
        assertEquals(ImageFormat.PNG, pngMetadata.getFormat(), "PNG format should be detected");
        assertEquals(800, pngMetadata.getWidth(), "PNG width should be correct");
        assertEquals(600, pngMetadata.getHeight(), "PNG height should be correct");

        // Load and verify format for JPEG image
        ImageMetadata jpegMetadata = loadMetadata(testImage2.getAbsolutePath());
        assertNotNull(jpegMetadata, "JPEG metadata should exist");
        assertEquals(ImageFormat.JPEG, jpegMetadata.getFormat(), "JPEG format should be detected");
        assertEquals(1024, jpegMetadata.getWidth(), "JPEG width should be correct");
        assertEquals(768, jpegMetadata.getHeight(), "JPEG height should be correct");

        // Load and verify format for GIF image
        ImageMetadata gifMetadata = loadMetadata(testImage3.getAbsolutePath());
        assertNotNull(gifMetadata, "GIF metadata should exist");
        assertEquals(ImageFormat.GIF, gifMetadata.getFormat(), "GIF format should be detected");
        assertEquals(400, gifMetadata.getWidth(), "GIF width should be correct");
        assertEquals(300, gifMetadata.getHeight(), "GIF height should be correct");

        log.info("SUCCESS: All image formats detected correctly");
    }

    /**
     * Test: ImageMetadataJob can be run multiple times safely (idempotent).
     *
     * Given: Job has already run and created metadata
     * When: Job is run again
     * Then: No errors occur, metadata remains valid
     */
    @Test
    void testImageMetadataJobIsIdempotent() throws Exception {
        log.info("TEST: ImageMetadataJob idempotency");

        // Run job first time
        JobExecution execution1 = imageMetadataJobScheduler.runImageMetadataJob("TEST");
        assertNotNull(execution1, "First execution should not be null");
        assertEquals(BatchStatus.COMPLETED, execution1.getStatus(),
            "First execution should complete successfully");

        // Verify metadata exists after first run
        ImageMetadata metadataAfterFirst = loadMetadata(testImage1.getAbsolutePath());
        assertNotNull(metadataAfterFirst, "Metadata should exist after first run");
        long firstSize = metadataAfterFirst.getSizeInBytes();
        ImageFormat firstFormat = metadataAfterFirst.getFormat();
        int firstWidth = metadataAfterFirst.getWidth();
        int firstHeight = metadataAfterFirst.getHeight();

        // Run job second time
        JobExecution execution2 = imageMetadataJobScheduler.runImageMetadataJob("TEST");
        assertNotNull(execution2, "Second execution should not be null");
        assertEquals(BatchStatus.COMPLETED, execution2.getStatus(),
            "Second execution should complete successfully");

        // Verify metadata still exists and hasn't changed
        ImageMetadata metadataAfterSecond = loadMetadata(testImage1.getAbsolutePath());
        assertNotNull(metadataAfterSecond, "Metadata should still exist after second run");
        assertEquals(firstSize, metadataAfterSecond.getSizeInBytes(),
            "File size should not change on second run");
        assertEquals(firstFormat, metadataAfterSecond.getFormat(),
            "Format should not change on second run");
        assertEquals(firstWidth, metadataAfterSecond.getWidth(),
            "Width should not change on second run");
        assertEquals(firstHeight, metadataAfterSecond.getHeight(),
            "Height should not change on second run");

        log.info("SUCCESS: Job is idempotent");
    }
}
