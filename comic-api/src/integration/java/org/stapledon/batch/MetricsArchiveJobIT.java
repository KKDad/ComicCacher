package org.stapledon.batch;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
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

import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.*;

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
    void setupTestMetrics() {
        log.info("Setting up test metrics for MetricsArchiveJob tests");

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
        assertFalse(Files.exists(expectedFile), "Archive file should not exist before job runs");

        // Execute the job
        log.info("Executing MetricsArchiveJob via scheduler");
        JobExecution jobExecution = metricsArchiveJobScheduler.runMetricsArchiveJob("TEST");

        // Assert job completed successfully
        assertNotNull(jobExecution, "JobExecution should not be null");
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus(),
            "Job should complete successfully");
        assertNotNull(jobExecution.getExitStatus(), "Exit status should not be null");
        assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode(),
            "Exit code should be COMPLETED");
        log.info("Job completed with status: {}", jobExecution.getStatus());

        // Verify after state - archive file exists
        assertTrue(Files.exists(expectedFile), "Archive file should exist after job runs");
        assertTrue(Files.isRegularFile(expectedFile), "Archive should be a regular file");
        assertTrue(Files.size(expectedFile) > 0, "Archive file should not be empty");

        // Verify archive content
        String json = Files.readString(expectedFile);
        CombinedMetricsData archived = gson.fromJson(json, CombinedMetricsData.class);

        assertNotNull(archived, "Archived metrics should not be null");
        assertNotNull(archived.getComics(), "Comics map should not be null");
        assertEquals(2, archived.getComics().size(), "Should have 2 comics");

        // Verify specific comic metrics
        assertTrue(archived.getComics().containsKey("TestComic1"),
            "TestComic1 should be in archive");
        ComicCombinedMetrics comic1 = archived.getComics().get("TestComic1");
        assertEquals("TestComic1", comic1.getComicName(), "Comic name should match");
        assertEquals(10, comic1.getImageCount(), "TestComic1 image count should match");
        assertEquals(1024000L, comic1.getStorageBytes(), "TestComic1 size should match");

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
        assertNotNull(execution1, "First execution should not be null");
        assertEquals(BatchStatus.COMPLETED, execution1.getStatus(),
            "First execution should complete successfully");

        // Verify archive exists
        assertTrue(Files.exists(expectedFile), "Archive should exist after first run");
        long firstSize = Files.size(expectedFile);

        // Run job second time
        JobExecution execution2 = metricsArchiveJobScheduler.runMetricsArchiveJob("TEST");
        assertNotNull(execution2, "Second execution should not be null");
        assertEquals(BatchStatus.COMPLETED, execution2.getStatus(),
            "Second execution should complete successfully");

        // Verify archive still exists
        assertTrue(Files.exists(expectedFile), "Archive should still exist after second run");
        long secondSize = Files.size(expectedFile);
        assertEquals(firstSize, secondSize, "Archive size should be consistent across runs");

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
        assertNotNull(jobExecution, "JobExecution should not be null");
        assertEquals(BatchStatus.FAILED, jobExecution.getStatus(),
            "Job should fail with no metrics");
        assertNotNull(jobExecution.getExitStatus(), "Exit status should not be null");
        assertEquals("FAILED", jobExecution.getExitStatus().getExitCode(),
            "Exit code should be FAILED");

        // No archive file should be created
        assertFalse(Files.exists(expectedFile), "Archive file should not exist when no metrics");

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
