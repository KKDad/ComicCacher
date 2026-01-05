package org.stapledon.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.dto.ImageMetadata;

import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * Integration tests for ImageMetadataJob.
 * Tests the complete flow of metadata generation for images without metadata
 * files.
 */
@Slf4j
class ImageMetadataJobIT extends AbstractBatchJobIntegrationTest {

        @Autowired
        private JobLauncher jobLauncher;

        @Autowired
        @Qualifier("imageMetadataBackfillJob")
        private Job imageMetadataBackfillJob;

        private File testImage1;
        private File testImage2;
        private File testImage3;

        /**
         * Runs the job manually for testing.
         */
        private JobExecution runJob() throws Exception {
                return jobLauncher.run(imageMetadataBackfillJob,
                                new JobParametersBuilder()
                                                .addLong("runId", System.currentTimeMillis())
                                                .addString("trigger", "TEST")
                                                .toJobParameters());
        }

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
         * Test: ImageMetadataJob processes all images without metadata and creates
         * valid metadata files.
         */
        @Test
        void testImageMetadataJobProcessesImagesWithoutMetadata() throws Exception {
                log.info("TEST: ImageMetadataJob processes images without metadata");

                // Verify before state - no metadata files
                assertMetadataNotExists(testImage1.getAbsolutePath());
                assertMetadataNotExists(testImage2.getAbsolutePath());
                assertMetadataNotExists(testImage3.getAbsolutePath());

                // Execute the job
                log.info("Executing ImageMetadataJob");
                JobExecution jobExecution = runJob();

                // Assert job completed successfully
                assertThat(jobExecution).as("JobExecution should not be null").isNotNull();
                assertThat(jobExecution.getStatus()).as("Job should complete successfully")
                                .isEqualTo(BatchStatus.COMPLETED);
                log.info("Job completed with status: {}", jobExecution.getStatus());

                // Verify after state - all metadata files exist with correct data
                assertMetadataValid(testImage1.getAbsolutePath(), ImageFormat.PNG, 800, 600);
                assertMetadataValid(testImage2.getAbsolutePath(), ImageFormat.JPEG, 1024, 768);
                assertMetadataValid(testImage3.getAbsolutePath(), ImageFormat.GIF, 400, 300);

                // Verify metadata content in detail for first image
                ImageMetadata metadata1 = loadMetadata(testImage1.getAbsolutePath());
                assertThat(metadata1.getColorMode()).as("ColorMode should not be null").isNotNull();
                assertThat(metadata1.getColorMode()).as("ColorMode should be detected (not UNKNOWN)")
                                .isNotEqualTo(ImageMetadata.ColorMode.UNKNOWN);
                assertThat(metadata1.getSizeInBytes() > 0).as("File size should be > 0").isTrue();
                assertThat(metadata1.getFilePath().endsWith("2024-01-15.png")).as("File path should be correct")
                                .isTrue();
                assertThat(ImageFormat.PNG).as("Format should match").isEqualTo(metadata1.getFormat());

                // Verify job execution details
                assertThat(jobExecution.getExitStatus()).as("Exit status should not be null").isNotNull();
                assertThat(jobExecution.getExitStatus().getExitCode()).as("Exit code should be COMPLETED")
                                .isEqualTo("COMPLETED");

                // Verify JsonBatchExecutionTracker recorded the execution
                assertBatchExecutionTracked("ImageMetadataBackfillJob");
                assertBatchExecutionValid("ImageMetadataBackfillJob", "COMPLETED");

                log.info("SUCCESS: All 3 images have valid metadata and execution tracked");
        }

        /**
         * Test: ImageMetadataJob skips images that already have metadata.
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
                assertThat(saved).as("Should save existing metadata for test setup").isTrue();

                // Verify before state
                assertMetadataExists(testImage1.getAbsolutePath());
                assertMetadataNotExists(testImage2.getAbsolutePath());

                // Execute the job
                JobExecution jobExecution = runJob();

                // Assert job completed successfully
                assertThat(jobExecution).as("JobExecution should not be null").isNotNull();
                assertThat(jobExecution.getStatus()).as("Job should complete successfully")
                                .isEqualTo(BatchStatus.COMPLETED);
                assertThat(jobExecution.getExitStatus()).as("Exit status should not be null").isNotNull();
                assertThat(jobExecution.getExitStatus().getExitCode()).as("Exit code should be COMPLETED")
                                .isEqualTo("COMPLETED");

                // Verify image1 metadata was NOT changed (still has test size)
                ImageMetadata metadata1After = loadMetadata(testImage1.getAbsolutePath());
                assertThat(metadata1After).as("Metadata should exist").isNotNull();
                assertThat(metadata1After.getSizeInBytes()).as("Existing metadata should not be overwritten")
                                .isEqualTo(12345L);
                assertThat(metadata1After.getFormat()).as("Format should remain unchanged").isEqualTo(ImageFormat.PNG);
                assertThat(metadata1After.getWidth()).as("Width should remain unchanged").isEqualTo(800);
                assertThat(metadata1After.getHeight()).as("Height should remain unchanged").isEqualTo(600);

                // Verify image2 got new metadata
                assertMetadataValid(testImage2.getAbsolutePath(), ImageFormat.JPEG, 1024, 768);

                log.info("SUCCESS: Job skipped image with existing metadata");
        }

        /**
         * Test: ImageMetadataJob handles invalid images gracefully.
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
                JobExecution jobExecution = runJob();

                // Assert job completed successfully even with invalid images
                assertThat(jobExecution).as("JobExecution should not be null").isNotNull();
                assertThat(jobExecution.getStatus()).as("Job should complete even with invalid images")
                                .isEqualTo(BatchStatus.COMPLETED);
                assertThat(jobExecution.getExitStatus()).as("Exit status should not be null").isNotNull();
                assertThat(jobExecution.getExitStatus().getExitCode()).as("Exit code should be COMPLETED")
                                .isEqualTo("COMPLETED");

                // Valid image should have metadata with correct values
                assertMetadataValid(testImage1.getAbsolutePath(), ImageFormat.PNG, 800, 600);
                ImageMetadata validMetadata = loadMetadata(testImage1.getAbsolutePath());
                assertThat(validMetadata.getColorMode()).as("ColorMode should not be null").isNotNull();
                assertThat(validMetadata.getSizeInBytes() > 0).as("File size should be > 0").isTrue();

                // Invalid image should NOT have metadata
                assertMetadataNotExists(invalidImage.getAbsolutePath());

                log.info("SUCCESS: Job handled invalid image gracefully");
        }

        /**
         * Test: ImageMetadataJob processes images with various formats correctly.
         */
        @Test
        void testImageMetadataJobDetectsFormatsCorrectly() throws Exception {
                log.info("TEST: ImageMetadataJob format detection");

                // Execute the job
                JobExecution jobExecution = runJob();
                assertThat(jobExecution).as("JobExecution should not be null").isNotNull();
                assertThat(jobExecution.getStatus()).as("Job should complete successfully")
                                .isEqualTo(BatchStatus.COMPLETED);

                // Load and verify format for PNG image
                ImageMetadata pngMetadata = loadMetadata(testImage1.getAbsolutePath());
                assertThat(pngMetadata).as("PNG metadata should exist").isNotNull();
                assertThat(pngMetadata.getFormat()).as("PNG format should be detected").isEqualTo(ImageFormat.PNG);
                assertThat(pngMetadata.getWidth()).as("PNG width should be correct").isEqualTo(800);
                assertThat(pngMetadata.getHeight()).as("PNG height should be correct").isEqualTo(600);

                // Load and verify format for JPEG image
                ImageMetadata jpegMetadata = loadMetadata(testImage2.getAbsolutePath());
                assertThat(jpegMetadata).as("JPEG metadata should exist").isNotNull();
                assertThat(jpegMetadata.getFormat()).as("JPEG format should be detected").isEqualTo(ImageFormat.JPEG);
                assertThat(jpegMetadata.getWidth()).as("JPEG width should be correct").isEqualTo(1024);
                assertThat(jpegMetadata.getHeight()).as("JPEG height should be correct").isEqualTo(768);

                // Load and verify format for GIF image
                ImageMetadata gifMetadata = loadMetadata(testImage3.getAbsolutePath());
                assertThat(gifMetadata).as("GIF metadata should exist").isNotNull();
                assertThat(gifMetadata.getFormat()).as("GIF format should be detected").isEqualTo(ImageFormat.GIF);
                assertThat(gifMetadata.getWidth()).as("GIF width should be correct").isEqualTo(400);
                assertThat(gifMetadata.getHeight()).as("GIF height should be correct").isEqualTo(300);

                log.info("SUCCESS: All image formats detected correctly");
        }

        /**
         * Test: ImageMetadataJob can be run multiple times safely (idempotent).
         */
        @Test
        void testImageMetadataJobIsIdempotent() throws Exception {
                log.info("TEST: ImageMetadataJob idempotency");

                // Run job first time
                JobExecution execution1 = runJob();
                assertThat(execution1).as("First execution should not be null").isNotNull();
                assertThat(execution1.getStatus()).as("First execution should complete successfully")
                                .isEqualTo(BatchStatus.COMPLETED);

                // Verify metadata exists after first run
                ImageMetadata metadataAfterFirst = loadMetadata(testImage1.getAbsolutePath());
                assertThat(metadataAfterFirst).as("Metadata should exist after first run").isNotNull();
                long firstSize = metadataAfterFirst.getSizeInBytes();
                ImageFormat firstFormat = metadataAfterFirst.getFormat();
                int firstWidth = metadataAfterFirst.getWidth();
                int firstHeight = metadataAfterFirst.getHeight();

                // Run job second time
                JobExecution execution2 = runJob();
                assertThat(execution2).as("Second execution should not be null").isNotNull();
                assertThat(execution2.getStatus()).as("Second execution should complete successfully")
                                .isEqualTo(BatchStatus.COMPLETED);

                // Verify metadata still exists and hasn't changed
                ImageMetadata metadataAfterSecond = loadMetadata(testImage1.getAbsolutePath());
                assertThat(metadataAfterSecond).as("Metadata should still exist after second run").isNotNull();
                assertThat(metadataAfterSecond.getSizeInBytes()).as("File size should not change on second run")
                                .isEqualTo(firstSize);
                assertThat(metadataAfterSecond.getFormat()).as("Format should not change on second run")
                                .isEqualTo(firstFormat);
                assertThat(metadataAfterSecond.getWidth()).as("Width should not change on second run")
                                .isEqualTo(firstWidth);
                assertThat(metadataAfterSecond.getHeight()).as("Height should not change on second run")
                                .isEqualTo(firstHeight);

                log.info("SUCCESS: Job is idempotent");
        }
}
