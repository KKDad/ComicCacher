package org.stapledon.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.service.RetrievalStatusService;
import org.stapledon.engine.batch.RetrievalRecordPurgeJobScheduler;

import java.time.LocalDate;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RetrievalRecordPurgeJob.
 * Tests the complete flow of purging old retrieval records.
 */
@Slf4j
@TestPropertySource(properties = {
    "batch.record-purge.enabled=true",
    "batch.record-purge.cron=0 45 6 * * ?",
    "batch.record-purge.days-to-keep=30"
})
class RetrievalRecordPurgeJobIT extends AbstractBatchJobIntegrationTest {

    @Autowired
    private RetrievalRecordPurgeJobScheduler retrievalRecordPurgeJobScheduler;

    @Autowired
    private RetrievalStatusService retrievalStatusService;

    @BeforeEach
    void setupTestRecords() {
        log.info("Setting up test retrieval records for RetrievalRecordPurgeJob tests");

        // Create recent records (within 30 days) - should NOT be purged
        for (int i = 1; i <= 5; i++) {
            ComicRetrievalRecord recentRecord = ComicRetrievalRecord.success(
                "RecentComic" + i,
                LocalDate.now().minusDays(i),
                "test",
                100L,
                50000L
            );
            retrievalStatusService.recordRetrievalResult(recentRecord);
        }

        // Create old records (older than 30 days) - should be purged
        for (int i = 1; i <= 10; i++) {
            ComicRetrievalRecord oldRecord = ComicRetrievalRecord.success(
                "OldComic" + i,
                LocalDate.now().minusDays(30 + i),
                "test",
                100L,
                50000L
            );
            retrievalStatusService.recordRetrievalResult(oldRecord);
        }

        log.info("Created {} test retrieval records", 15);
    }

    /**
     * Test: RetrievalRecordPurgeJob purges old records while keeping recent ones.
     *
     * Given: 15 retrieval records exist (5 recent, 10 old)
     * When: RetrievalRecordPurgeJob is executed with 30-day retention
     * Then: Old records are purged, recent records remain
     */
    @Test
    void testRetrievalRecordPurgeJobPurgesOldRecords() throws Exception {
        log.info("TEST: RetrievalRecordPurgeJob purges old records");

        // Verify before state - all records exist
        List<ComicRetrievalRecord> allRecordsBefore = retrievalStatusService.getRetrievalRecords(
            null, null, null, null, Integer.MAX_VALUE);
        assertEquals(15, allRecordsBefore.size(), "Should have 15 total records before purge");

        long recentCountBefore = allRecordsBefore.stream()
            .filter(r -> r.getComicDate().isAfter(LocalDate.now().minusDays(30)))
            .count();
        assertEquals(5, recentCountBefore, "Should have 5 recent records before purge");

        long oldCountBefore = allRecordsBefore.stream()
            .filter(r -> r.getComicDate().isBefore(LocalDate.now().minusDays(30)))
            .count();
        assertEquals(10, oldCountBefore, "Should have 10 old records before purge");

        // Execute the job
        log.info("Executing RetrievalRecordPurgeJob via scheduler");
        JobExecution jobExecution = retrievalRecordPurgeJobScheduler.runRecordPurgeJob("TEST");

        // Assert job completed successfully
        assertNotNull(jobExecution, "JobExecution should not be null");
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus(),
            "Job should complete successfully");
        assertNotNull(jobExecution.getExitStatus(), "Exit status should not be null");
        assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode(),
            "Exit code should be COMPLETED");
        log.info("Job completed with status: {}", jobExecution.getStatus());

        // Verify after state - only recent records remain
        List<ComicRetrievalRecord> allRecordsAfter = retrievalStatusService.getRetrievalRecords(
            null, null, null, null, Integer.MAX_VALUE);
        assertEquals(5, allRecordsAfter.size(), "Should have 5 records after purge");

        // All remaining records should be recent
        for (ComicRetrievalRecord record : allRecordsAfter) {
            assertTrue(record.getComicDate().isAfter(LocalDate.now().minusDays(31)),
                "All remaining records should be recent (within 30 days)");
            assertTrue(record.getComicName().startsWith("RecentComic"),
                "Remaining records should be recent comics");
        }

        // Verify no old records remain
        boolean hasOldRecords = allRecordsAfter.stream()
            .anyMatch(r -> r.getComicName().startsWith("OldComic"));
        assertFalse(hasOldRecords, "No old records should remain after purge");

        // Verify JsonBatchExecutionTracker recorded the execution
        assertBatchExecutionTracked("RetrievalRecordPurgeJob");
        assertBatchExecutionValid("RetrievalRecordPurgeJob", "COMPLETED", null);

        log.info("SUCCESS: Purged 10 old records, kept 5 recent records, execution tracked");
    }

    /**
     * Test: RetrievalRecordPurgeJob handles no records gracefully.
     *
     * Given: No retrieval records exist
     * When: RetrievalRecordPurgeJob is executed
     * Then: Job completes successfully with no errors
     */
    @Test
    void testRetrievalRecordPurgeJobWithNoRecords() throws Exception {
        log.info("TEST: RetrievalRecordPurgeJob with no records");

        // Clear all records
        List<ComicRetrievalRecord> allRecords = retrievalStatusService.getRetrievalRecords(
            null, null, null, null, Integer.MAX_VALUE);
        for (ComicRetrievalRecord record : allRecords) {
            retrievalStatusService.deleteRetrievalRecord(record.getId());
        }

        // Execute the job
        JobExecution jobExecution = retrievalRecordPurgeJobScheduler.runRecordPurgeJob("TEST");

        // Job should complete successfully even with no records
        assertNotNull(jobExecution, "JobExecution should not be null");
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus(),
            "Job should complete successfully even with no old records");

        log.info("SUCCESS: Job handled no records gracefully");
    }

    /**
     * Test: RetrievalRecordPurgeJob is idempotent.
     *
     * Given: Job has already run and purged old records
     * When: Job is run again
     * Then: No errors occur, same records remain
     */
    @Test
    void testRetrievalRecordPurgeJobIsIdempotent() throws Exception {
        log.info("TEST: RetrievalRecordPurgeJob idempotency");

        // Run job first time
        JobExecution execution1 = retrievalRecordPurgeJobScheduler.runRecordPurgeJob("TEST");
        assertNotNull(execution1, "First execution should not be null");
        assertEquals(BatchStatus.COMPLETED, execution1.getStatus(),
            "First execution should complete successfully");

        // Check records after first run
        List<ComicRetrievalRecord> recordsAfterFirst = retrievalStatusService.getRetrievalRecords(
            null, null, null, null, Integer.MAX_VALUE);
        int countAfterFirst = recordsAfterFirst.size();
        assertEquals(5, countAfterFirst, "Should have 5 records after first purge");

        // Run job second time
        JobExecution execution2 = retrievalRecordPurgeJobScheduler.runRecordPurgeJob("TEST");
        assertNotNull(execution2, "Second execution should not be null");
        assertEquals(BatchStatus.COMPLETED, execution2.getStatus(),
            "Second execution should complete successfully");

        // Check records after second run - should be the same
        List<ComicRetrievalRecord> recordsAfterSecond = retrievalStatusService.getRetrievalRecords(
            null, null, null, null, Integer.MAX_VALUE);
        int countAfterSecond = recordsAfterSecond.size();
        assertEquals(countAfterFirst, countAfterSecond,
            "Record count should be same after second purge");

        log.info("SUCCESS: Job is idempotent");
    }
}
