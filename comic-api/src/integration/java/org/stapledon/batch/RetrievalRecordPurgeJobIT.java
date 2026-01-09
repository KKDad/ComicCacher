package org.stapledon.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.service.RetrievalStatusService;

import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

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
        private JobOperator jobOperator;

        @Autowired
        private JobExplorer jobExplorer;

        @Autowired
        @Qualifier("retrievalRecordPurgeJob")
        private Job retrievalRecordPurgeJob;

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
                                        50000L);
                        retrievalStatusService.recordRetrievalResult(recentRecord);
                }

                // Create old records (older than 30 days) - should be purged
                for (int i = 1; i <= 10; i++) {
                        ComicRetrievalRecord oldRecord = ComicRetrievalRecord.success(
                                        "OldComic" + i,
                                        LocalDate.now().minusDays(30 + i),
                                        "test",
                                        100L,
                                        50000L);
                        retrievalStatusService.recordRetrievalResult(oldRecord);
                }

                log.info("Created {} test retrieval records", 15);
        }

        /**
         * Runs the job manually for testing.
         */
        private JobExecution runJob() throws Exception {
                Properties params = new Properties();
                params.put("runId", String.valueOf(System.currentTimeMillis()));
                params.put("trigger", "TEST");

                Long executionId = jobOperator.start(retrievalRecordPurgeJob.getName(), params);
                return jobExplorer.getJobExecution(executionId);
        }

        /**
         * Test: RetrievalRecordPurgeJob purges old records while keeping recent ones.
         *
         * Given: 15 retrieval records exist (5 recent, 10 old)
         * When: RetrievalRecordPurgeJob is executed with 30-day retention
         * Then: Old records are purged, recent records remain
         */
        @Test
        void retrievalRecordPurgeJobPurgesOldRecords() throws Exception {
                log.info("TEST: RetrievalRecordPurgeJob purges old records");

                // Verify before state - all records exist
                List<ComicRetrievalRecord> allRecordsBefore = retrievalStatusService.getRetrievalRecords(
                                null, null, null, null, Integer.MAX_VALUE);
                assertThat(allRecordsBefore.size()).as("Should have 15 total records before purge").isEqualTo(15);

                long recentCountBefore = allRecordsBefore.stream()
                                .filter(r -> r.getComicDate().isAfter(LocalDate.now().minusDays(30)))
                                .count();
                assertThat(recentCountBefore).as("Should have 5 recent records before purge").isEqualTo(5);

                long oldCountBefore = allRecordsBefore.stream()
                                .filter(r -> r.getComicDate().isBefore(LocalDate.now().minusDays(30)))
                                .count();
                assertThat(oldCountBefore).as("Should have 10 old records before purge").isEqualTo(10);

                // Execute the job
                log.info("Executing RetrievalRecordPurgeJob");
                JobExecution jobExecution = runJob();

                // Assert job completed successfully
                assertThat(jobExecution).as("JobExecution should not be null").isNotNull();
                assertThat(jobExecution.getStatus()).as("Job should complete successfully")
                                .isEqualTo(BatchStatus.COMPLETED);
                assertThat(jobExecution.getExitStatus()).as("Exit status should not be null").isNotNull();
                assertThat(jobExecution.getExitStatus().getExitCode()).as("Exit code should be COMPLETED")
                                .isEqualTo("COMPLETED");
                log.info("Job completed with status: {}", jobExecution.getStatus());

                // Verify after state - only recent records remain
                List<ComicRetrievalRecord> allRecordsAfter = retrievalStatusService.getRetrievalRecords(
                                null, null, null, null, Integer.MAX_VALUE);
                assertThat(allRecordsAfter.size()).as("Should have 5 records after purge").isEqualTo(5);

                // All remaining records should be recent
                for (ComicRetrievalRecord record : allRecordsAfter) {
                        assertThat(record.getComicDate().isAfter(LocalDate.now().minusDays(31)))
                                        .as("All remaining records should be recent (within 30 days)").isTrue();
                        assertThat(record.getComicName().startsWith("RecentComic"))
                                        .as("Remaining records should be recent comics")
                                        .isTrue();
                }

                // Verify no old records remain
                boolean hasOldRecords = allRecordsAfter.stream()
                                .anyMatch(r -> r.getComicName().startsWith("OldComic"));
                assertThat(hasOldRecords).as("No old records should remain after purge").isFalse();

                // Verify JsonBatchExecutionTracker recorded the execution
                assertBatchExecutionTracked("RetrievalRecordPurgeJob");
                assertBatchExecutionValid("RetrievalRecordPurgeJob", "COMPLETED");

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
        void retrievalRecordPurgeJobWithNoRecords() throws Exception {
                log.info("TEST: RetrievalRecordPurgeJob with no records");

                // Clear all records
                List<ComicRetrievalRecord> allRecords = retrievalStatusService.getRetrievalRecords(
                                null, null, null, null, Integer.MAX_VALUE);
                for (ComicRetrievalRecord record : allRecords) {
                        retrievalStatusService.deleteRetrievalRecord(record.getId());
                }

                // Execute the job
                JobExecution jobExecution = runJob();

                // Job should complete successfully even with no records
                assertThat(jobExecution).as("JobExecution should not be null").isNotNull();
                assertThat(jobExecution.getStatus()).as("Job should complete successfully even with no old records")
                                .isEqualTo(BatchStatus.COMPLETED);

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
        void retrievalRecordPurgeJobIsIdempotent() throws Exception {
                log.info("TEST: RetrievalRecordPurgeJob idempotency");

                // Run job first time
                JobExecution execution1 = runJob();
                assertThat(execution1).as("First execution should not be null").isNotNull();
                assertThat(execution1.getStatus()).as("First execution should complete successfully")
                                .isEqualTo(BatchStatus.COMPLETED);

                // Check records after first run
                List<ComicRetrievalRecord> recordsAfterFirst = retrievalStatusService.getRetrievalRecords(
                                null, null, null, null, Integer.MAX_VALUE);
                int countAfterFirst = recordsAfterFirst.size();
                assertThat(countAfterFirst).as("Should have 5 records after first purge").isEqualTo(5);

                // Run job second time
                JobExecution execution2 = runJob();
                assertThat(execution2).as("Second execution should not be null").isNotNull();
                assertThat(execution2.getStatus()).as("Second execution should complete successfully")
                                .isEqualTo(BatchStatus.COMPLETED);

                // Check records after second run - should be the same
                List<ComicRetrievalRecord> recordsAfterSecond = retrievalStatusService.getRetrievalRecords(
                                null, null, null, null, Integer.MAX_VALUE);
                int countAfterSecond = recordsAfterSecond.size();
                assertThat(countAfterSecond).as("Record count should be same after second purge")
                                .isEqualTo(countAfterFirst);

                log.info("SUCCESS: Job is idempotent");
        }
}
