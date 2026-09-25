package org.stapledon.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.metrics.collector.StorageMetricsCollector;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.metrics.dto.CombinedMetricsData.ComicCombinedMetrics;

import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;

/**
 * Integration tests for MetricsArchiveJob. Tests the complete flow of archiving daily metrics to JSON files.
 */
@Slf4j
@TestPropertySource(properties = {"batch.metrics-archive.enabled=true", "batch.metrics-archive.cron=0 30 6 * * ?"})
class MetricsArchiveJobIT extends AbstractBatchJobIntegrationTest {

    @Autowired private JobOperator jobOperator;

    @Autowired
    @Qualifier("metricsArchiveJob")
    private Job metricsArchiveJob;

    @Autowired private StorageMetricsCollector storageMetricsCollector;

    @Autowired
    @Qualifier("gsonWithLocalDate")
    private Gson gson;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Runs the job manually for testing.
     */
    private JobExecution runJob() throws Exception {
        JobParameters params = new JobParametersBuilder().addString("runId", String.valueOf(System.currentTimeMillis())).addString("trigger", "TEST").toJobParameters();

        return jobOperator.start(metricsArchiveJob, params);
    }

    @BeforeEach
    void setupTestMetrics() throws Exception {
        log.info("Setting up test metrics for MetricsArchiveJob tests");

        // Clean up any existing metrics-history directory from previous tests
        Path historyDir = Paths.get(cacheProperties.getLocation(), "metrics-history");
        if (Files.exists(historyDir)) {
            log.info("Cleaning up existing metrics-history directory");
            try (var walk = Files.walk(historyDir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (Exception e) {
                        log.warn("Failed to delete {}: {}", path, e.getMessage());
                    }
                });
            }
        }

        // Seed the cache with real images; the job builds combined metrics from a storage scan
        createTestImage("TestComic1/2024/2024-01-15.png", 800, 600, ImageFormat.PNG);
        createTestImage("TestComic1/2024/2024-01-16.png", 800, 600, ImageFormat.PNG);
        createTestImage("TestComic2/2024/2024-01-15.jpg", 800, 600, ImageFormat.JPEG);
        storageMetricsCollector.updateStats();

        log.info("Seeded test images for {} comics", 2);
    }

    /**
     * Test: MetricsArchiveJob archives metrics to JSON file for yesterday.
     */
    @Test
    void metricsArchiveJobCreatesArchive() throws Exception {
        log.info("TEST: MetricsArchiveJob creates metrics archive");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        Path expectedFile = getMetricsArchiveFile(yesterday);

        // Verify before state - no archive file
        assertThat(Files.exists(expectedFile)).as("Archive file should not exist before job runs").isFalse();

        // Execute the job
        log.info("Executing MetricsArchiveJob");
        JobExecution jobExecution = runJob();

        // Assert job completed successfully
        assertThat(jobExecution).as("JobExecution should not be null").isNotNull();
        assertThat(jobExecution.getStatus()).as("Job should complete successfully").isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus()).as("Exit status should not be null").isNotNull();
        assertThat(jobExecution.getExitStatus().getExitCode()).as("Exit code should be COMPLETED").isEqualTo("COMPLETED");
        log.info("Job completed with status: {}", jobExecution.getStatus());

        // Verify after state - archive file exists
        assertThat(Files.exists(expectedFile)).as("Archive file should exist after job runs").isTrue();
        assertThat(Files.isRegularFile(expectedFile)).as("Archive should be a regular file").isTrue();
        assertThat(Files.size(expectedFile) > 0).as("Archive file should not be empty").isTrue();

        // Verify archive content
        String json = Files.readString(expectedFile);
        CombinedMetricsData archived = gson.fromJson(json, CombinedMetricsData.class);

        assertThat(archived).as("Archived metrics should not be null").isNotNull();
        assertThat(archived.getPerComicMetrics()).as("Comics map should not be null").isNotNull();
        assertThat(archived.getPerComicMetrics().size()).as("Should have 2 comics").isEqualTo(2);

        // Verify specific comic metrics
        assertThat(archived.getPerComicMetrics().containsKey("TestComic1")).as("TestComic1 should be in archive").isTrue();
        ComicCombinedMetrics comic1 = archived.getPerComicMetrics().get("TestComic1");
        assertThat(comic1.getComicName()).as("Comic name should match").isEqualTo("TestComic1");
        assertThat(comic1.getImageCount()).as("TestComic1 image count should match").isEqualTo(2);
        assertThat(comic1.getStorageBytes()).as("TestComic1 size should be non-zero").isPositive();
        assertThat(archived.getPerComicMetrics().get("TestComic2").getImageCount()).as("TestComic2 image count should match").isEqualTo(1);

        // Verify JsonBatchExecutionTracker recorded the execution
        assertBatchExecutionTracked("MetricsArchiveJob");
        assertBatchExecutionValid("MetricsArchiveJob", "COMPLETED");

        log.info("SUCCESS: Metrics archive created and validated at {}", expectedFile);
    }

    /**
     * Test: MetricsArchiveJob is idempotent and overwrites existing archives.
     */
    @Test
    void metricsArchiveJobIsIdempotent() throws Exception {
        log.info("TEST: MetricsArchiveJob idempotency");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        Path expectedFile = getMetricsArchiveFile(yesterday);

        // Run job first time
        JobExecution execution1 = runJob();
        assertThat(execution1).as("First execution should not be null").isNotNull();
        assertThat(execution1.getStatus()).as("First execution should complete successfully").isEqualTo(BatchStatus.COMPLETED);

        // Verify archive exists
        assertThat(Files.exists(expectedFile)).as("Archive should exist after first run").isTrue();
        CombinedMetricsData firstArchive = gson.fromJson(Files.readString(expectedFile), CombinedMetricsData.class);

        // Run job second time
        JobExecution execution2 = runJob();
        assertThat(execution2).as("Second execution should not be null").isNotNull();
        assertThat(execution2.getStatus()).as("Second execution should complete successfully").isEqualTo(BatchStatus.COMPLETED);

        // Verify archive still exists
        assertThat(Files.exists(expectedFile)).as("Archive should still exist after second run").isTrue();
        CombinedMetricsData secondArchive = gson.fromJson(Files.readString(expectedFile), CombinedMetricsData.class);
        // lastUpdated changes on every run since metrics are built on demand; the per-comic data should not
        assertThat(secondArchive.getPerComicMetrics()).as("Archived comic metrics should be consistent across runs").isEqualTo(firstArchive.getPerComicMetrics());

        log.info("SUCCESS: Job is idempotent");
    }

    /**
     * Helper method to get the expected archive file path for a date
     */
    private Path getMetricsArchiveFile(LocalDate date) {
        String filename = date.format(DATE_FORMATTER) + ".json";
        return Paths.get(cacheProperties.getLocation(), "metrics-history", filename);
    }
}
