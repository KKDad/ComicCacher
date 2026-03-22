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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * Integration test verifying that batch job log files land in the correct directory
 * ({jobName}/{jobName}-{date}-{hash}.log) via the composite MDC key (batchLogPath)
 * used by the SiftingAppender.
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

        // Verify log file matching LogVerificationJob-{date}-{hash}.log exists in the job directory
        Path jobLogDir = Paths.get(
                cacheProperties.getLocation(), "batch-logs", "LogVerificationJob");
        Path logFile = findLogFile(jobLogDir, "LogVerificationJob-");
        assertThat(logFile)
                .as("Log file matching LogVerificationJob-*.log should exist in %s", jobLogDir)
                .isNotNull();
        assertThat(Files.size(logFile))
                .as("Log file should not be empty")
                .isGreaterThan(0);

        // Verify marker text is in the log
        String logContent = Files.readString(logFile);
        assertThat(logContent).contains("Log verification marker");

        // Verify no log file at the old broken path
        Path brokenLogDir = Paths.get(
                cacheProperties.getLocation(), "batch-logs", "unknown");
        assertThat(Files.exists(brokenLogDir))
                .as("No log files should exist at old broken path %s", brokenLogDir)
                .isFalse();
    }

    private Path findLogFile(Path dir, String prefix) throws IOException {
        if (!Files.isDirectory(dir)) {
            return null;
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith(prefix) && name.endsWith(".log");
                    })
                    .findFirst()
                    .orElse(null);
        }
    }
}
