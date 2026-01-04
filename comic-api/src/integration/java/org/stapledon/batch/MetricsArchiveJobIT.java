package org.stapledon.batch;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.stapledon.engine.batch.MetricsArchiveJobScheduler;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.metrics.dto.CombinedMetricsData.ComicCombinedMetrics;
import org.stapledon.metrics.repository.CombinedMetricsRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MetricsArchiveJob.
 * Tests the complete flow of archiving daily metrics to JSON files.
 */
@Slf4j
@TestPropertySource(properties = {
        "batch.metrics-archive.enabled=true",
        "batch.metrics-archive.cron=0 30 6 * * ?"
})
class MetricsArchiveJobIT extends AbstractBatchJobIntegrationTest {

    @Autowired
    private MetricsArchiveJobScheduler metricsArchiveJobScheduler;

    @Autowired
    private CombinedMetricsRepository combinedMetricsRepository;

    @Autowired
    @Qualifier("gsonWithLocalDate")
    private Gson gson;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @BeforeEach
    void setupTestMetrics() throws Exception {
        log.info("Setting up test metrics for MetricsArchiveJob tests");

        // Clean up any existing metrics-history directory from previous tests
        Path historyDir = Paths.get(cacheProperties.getLocation(), "metrics-history");
        if (Files.exists(historyDir)) {
            log.info("Cleaning up existing metrics-history directory");
            try (var walk = Files.walk(historyDir)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (Exception e) {
                                log.warn("Failed to delete {}: {}", path, e.getMessage());
                            }
                        });
            }
        }

        // Create test metrics data
        Map<String, ComicCombinedMetrics> comicsMap = new HashMap<>();
        comicsMap.put("TestComic1", ComicCombinedMetrics.builder()
                .comicName("TestComic1")
                .imageCount(10)
                .storageBytes(1024000L)
                .build());
        comicsMap.put("TestComic2", ComicCombinedMetrics.builder()
                .comicName("TestComic2")
                .imageCount(5)
                .storageBytes(512000L)
                .build());

        CombinedMetricsData testMetrics = CombinedMetricsData.builder()
                .comics(comicsMap)
                .build();

        combinedMetricsRepository.save(testMetrics);
        log.info("Created test metrics: {} comics", 2);
    }

    /**
     * Test: MetricsArchiveJob archives metrics to JSON file for yesterday.
     *
     * Given: Metrics data exists in the repository
     * When: MetricsArchiveJob is executed
     * Then: A JSON file is created in metrics-history with yesterday's date
     */
    @Test
    void testMetricsArchiveJobCreatesArchive() throws Exception {
        log.info("TEST: MetricsArchiveJob creates metrics archive");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        Path expectedFile = getMetricsArchiveFile(yesterday);

        // Verify before state - no archive file
        assertThat(Files.exists(expectedFile)).as("Archive file should not exist before job runs").isFalse();

        // Execute the job
        log.info("Executing MetricsArchiveJob via scheduler");
        JobExecution jobExecution = metricsArchiveJobScheduler.runMetricsArchiveJob("TEST");

        // Assert job completed successfully
        assertThat(jobExecution).as("JobExecution should not be null").isNotNull();
        assertThat(jobExecution.getStatus()).as("Job should complete successfully").isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus()).as("Exit status should not be null").isNotNull();
        assertThat(jobExecution.getExitStatus().getExitCode()).as("Exit code should be COMPLETED")
                .isEqualTo("COMPLETED");
        log.info("Job completed with status: {}", jobExecution.getStatus());

        // Verify after state - archive file exists
        assertThat(Files.exists(expectedFile)).as("Archive file should exist after job runs").isTrue();
        assertThat(Files.isRegularFile(expectedFile)).as("Archive should be a regular file").isTrue();
        assertThat(Files.size(expectedFile) > 0).as("Archive file should not be empty").isTrue();

        // Verify archive content
        String json = Files.readString(expectedFile);
        CombinedMetricsData archived = gson.fromJson(json, CombinedMetricsData.class);

        assertThat(archived).as("Archived metrics should not be null").isNotNull();
        assertThat(archived.getComics()).as("Comics map should not be null").isNotNull();
        assertThat(archived.getComics().size()).as("Should have 2 comics").isEqualTo(2);

        // Verify specific comic metrics
        assertThat(archived.getComics().containsKey("TestComic1")).as("TestComic1 should be in archive").isTrue();
        ComicCombinedMetrics comic1 = archived.getComics().get("TestComic1");
        assertThat(comic1.getComicName()).as("Comic name should match").isEqualTo("TestComic1");
        assertThat(comic1.getImageCount()).as("TestComic1 image count should match").isEqualTo(10);
        assertThat(comic1.getStorageBytes()).as("TestComic1 size should match").isEqualTo(1024000L);

        // Verify JsonBatchExecutionTracker recorded the execution
        assertBatchExecutionTracked("MetricsArchiveJob");
        assertBatchExecutionValid("MetricsArchiveJob", "COMPLETED"); // Tasklet job, no item counts

        log.info("SUCCESS: Metrics archive created and validated at {}", expectedFile);
    }

    /**
     * Test: MetricsArchiveJob is idempotent and overwrites existing archives.
     *
     * Given: Job has already run and created an archive
     * When: Job is run again
     * Then: Archive is updated successfully
     */
    @Test
    void testMetricsArchiveJobIsIdempotent() throws Exception {
        log.info("TEST: MetricsArchiveJob idempotency");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        Path expectedFile = getMetricsArchiveFile(yesterday);

        // Run job first time
        JobExecution execution1 = metricsArchiveJobScheduler.runMetricsArchiveJob("TEST");
        assertThat(execution1).as("First execution should not be null").isNotNull();
        assertThat(execution1.getStatus()).as("First execution should complete successfully")
                .isEqualTo(BatchStatus.COMPLETED);

        // Verify archive exists
        assertThat(Files.exists(expectedFile)).as("Archive should exist after first run").isTrue();
        long firstSize = Files.size(expectedFile);

        // Run job second time
        JobExecution execution2 = metricsArchiveJobScheduler.runMetricsArchiveJob("TEST");
        assertThat(execution2).as("Second execution should not be null").isNotNull();
        assertThat(execution2.getStatus()).as("Second execution should complete successfully")
                .isEqualTo(BatchStatus.COMPLETED);

        // Verify archive still exists
        assertThat(Files.exists(expectedFile)).as("Archive should still exist after second run").isTrue();
        long secondSize = Files.size(expectedFile);
        assertThat(secondSize).as("Archive size should be consistent across runs").isEqualTo(firstSize);

        log.info("SUCCESS: Job is idempotent");
    }

    /**
     * Test: MetricsArchiveJob fails gracefully when no metrics exist.
     *
     * Given: No metrics data exists (empty comics map)
     * When: MetricsArchiveJob is executed
     * Then: Job fails appropriately and no archive is created
     */
    @Test
    void testMetricsArchiveJobFailsWithNoMetrics() throws Exception {
        log.info("TEST: MetricsArchiveJob fails with no metrics");

        // Clear metrics repository
        combinedMetricsRepository.save(CombinedMetricsData.builder()
                .comics(new HashMap<>())
                .build());

        LocalDate yesterday = LocalDate.now().minusDays(1);
        Path expectedFile = getMetricsArchiveFile(yesterday);

        // Execute the job
        JobExecution jobExecution = metricsArchiveJobScheduler.runMetricsArchiveJob("TEST");

        // Job should fail when there are no metrics
        assertThat(jobExecution).as("JobExecution should not be null").isNotNull();
        assertThat(jobExecution.getStatus()).as("Job should fail with no metrics").isEqualTo(BatchStatus.FAILED);
        assertThat(jobExecution.getExitStatus()).as("Exit status should not be null").isNotNull();
        assertThat(jobExecution.getExitStatus().getExitCode()).as("Exit code should be FAILED").isEqualTo("FAILED");

        // No archive file should be created
        assertThat(Files.exists(expectedFile)).as("Archive file should not exist when no metrics").isFalse();

        // Verify JsonBatchExecutionTracker recorded the failure
        assertBatchExecutionTracked("MetricsArchiveJob");
        assertBatchExecutionValid("MetricsArchiveJob", "FAILED"); // Failed job, no item counts

        log.info("SUCCESS: Job failed appropriately with empty metrics");
    }

    /**
     * Helper method to get the expected archive file path for a date
     */
    private Path getMetricsArchiveFile(LocalDate date) {
        String filename = date.format(DATE_FORMATTER) + ".json";
        return Paths.get(cacheProperties.getLocation(), "metrics-history", filename);
    }
}
