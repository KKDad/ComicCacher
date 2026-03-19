package org.stapledon.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;

/**
 * Integration test verifying that batch job log files land in the correct directory ({jobName}/{executionId}.log)
 * via the composite MDC key (batchLogPath) used by the SiftingAppender.
 */
@Slf4j
class BatchLogMdcIT extends AbstractBatchJobIntegrationTest {

    @Autowired private JobOperator jobOperator;

    @Autowired
    @Qualifier("logVerificationJob")
    private Job logVerificationJob;

    @Test
    void batchLogFileLandsInCorrectDirectory() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("runId", String.valueOf(System.currentTimeMillis()))
                .addString("trigger", "TEST")
                .toJobParameters();

        JobExecution jobExecution = jobOperator.start(logVerificationJob, params);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        long executionId = jobExecution.getId();

        // Verify log file at correct path: batch-logs/LogVerificationJob/{executionId}.log
        Path expectedLogFile = Paths.get(
                cacheProperties.getLocation(), "batch-logs", "LogVerificationJob", executionId + ".log");
        assertThat(Files.exists(expectedLogFile))
                .as("Log file should exist at %s", expectedLogFile)
                .isTrue();
        assertThat(Files.size(expectedLogFile))
                .as("Log file should not be empty")
                .isGreaterThan(0);

        // Verify marker text is in the log
        String logContent = Files.readString(expectedLogFile);
        assertThat(logContent).contains("Log verification marker");

        // Verify no log file at the old broken path
        Path brokenLogFile = Paths.get(
                cacheProperties.getLocation(), "batch-logs", "unknown", executionId + ".log");
        assertThat(Files.exists(brokenLogFile))
                .as("Log file should NOT exist at old broken path %s", brokenLogFile)
                .isFalse();
    }
}
