package org.stapledon.engine.batch;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.engine.batch.dto.BatchExecutionSummary;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Exports Spring Batch job execution data to JSON file for monitoring and
 * history tracking.
 * Implements JobExecutionListener to automatically export after each job
 * completion.
 * Uses gsonWithLocalDate bean for proper LocalDateTime serialization.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonBatchExecutionTracker extends LoggingJobExecutionListener implements JobExecutionListener {

    private final CacheProperties cacheProperties;

    @Qualifier("gsonWithLocalDate")
    private final Gson gson;

    private static final String BATCH_EXECUTIONS_FILENAME = "batch-executions.json";

    @Override
    public void afterJob(JobExecution jobExecution) {
        try {
            // Create execution summary
            BatchExecutionSummary summary = createSummary(jobExecution);

            // Read existing executions
            Map<String, BatchExecutionSummary> executions = readExecutions();

            // Update with current execution
            String jobName = jobExecution.getJobInstance().getJobName();
            executions.put(jobName, summary);

            // Write back to file
            writeExecutions(executions);

            // Log completion
            logExecutionSummary(jobName, summary);

        } catch (Exception e) {
            log.error("Failed to export batch execution to JSON", e);
        }
    }

    /**
     * Create execution summary from JobExecution
     */
    private BatchExecutionSummary createSummary(JobExecution jobExecution) {
        BatchExecutionSummary summary = BatchExecutionSummary.builder()
                .lastExecutionId(jobExecution.getId())
                .lastExecutionTime(jobExecution.getEndTime())
                .status(jobExecution.getStatus().name())
                .exitCode(jobExecution.getExitStatus().getExitCode())
                .exitMessage(jobExecution.getExitStatus().getExitDescription())
                .startTime(jobExecution.getStartTime())
                .endTime(jobExecution.getEndTime())
                .build();

        // Capture error message if failed
        if (jobExecution.getStatus() == BatchStatus.FAILED && !jobExecution.getAllFailureExceptions().isEmpty()) {
            Throwable firstException = jobExecution.getAllFailureExceptions().getFirst();
            summary.setErrorMessage(firstException.getMessage());
        }

        return summary;
    }

    /**
     * Read existing executions from JSON file
     */
    private Map<String, BatchExecutionSummary> readExecutions() {
        Path filePath = getExecutionsFilePath();

        if (!Files.exists(filePath)) {
            return new HashMap<>();
        }

        try {
            String json = Files.readString(filePath);
            Type type = new TypeToken<Map<String, BatchExecutionSummary>>() {
            }.getType();
            Map<String, BatchExecutionSummary> executions = gson.fromJson(json, type);
            return executions != null ? executions : new HashMap<>();
        } catch (IOException e) {
            log.error("Failed to read batch executions from JSON file", e);
            return new HashMap<>();
        }
    }

    /**
     * Write executions to JSON file
     */
    private void writeExecutions(Map<String, BatchExecutionSummary> executions) throws IOException {
        Path filePath = getExecutionsFilePath();

        // Ensure parent directory exists
        Files.createDirectories(filePath.getParent());

        // Write JSON
        String json = gson.toJson(executions);
        Files.writeString(filePath, json);

        log.debug("Batch executions exported to: {}", filePath);
    }

    /**
     * Get path to batch executions JSON file
     */
    private Path getExecutionsFilePath() {
        return Paths.get(cacheProperties.getLocation(), BATCH_EXECUTIONS_FILENAME);
    }

    /**
     * Log execution summary
     */
    private void logExecutionSummary(String jobName, BatchExecutionSummary summary) {
        Duration duration = Duration.between(
                summary.getStartTime(),
                summary.getEndTime());
        log.info("Batch job completed: {} - Status: {}, Duration: {}s,", jobName, summary.getStatus(), duration);

        if (summary.getErrorMessage() != null) {
            log.error("Job failed with error: {}", summary.getErrorMessage());
        }
    }
}
