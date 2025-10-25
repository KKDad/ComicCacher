package org.stapledon.engine.batch;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.stapledon.common.config.CacheProperties;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Exports Spring Batch job execution data to JSON file for monitoring and history tracking.
 * Implements JobExecutionListener to automatically export after each job completion.
 * Uses gsonWithLocalDate bean for proper LocalDateTime serialization.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonBatchExecutionTracker implements JobExecutionListener {

    private final CacheProperties cacheProperties;

    @Qualifier("gsonWithLocalDate")
    private final Gson gson;

    private static final String BATCH_EXECUTIONS_FILENAME = "batch-executions.json";

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Starting batch job: {} (executionId: {})",
            jobExecution.getJobInstance().getJobName(),
            jobExecution.getId());
    }

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
        BatchExecutionSummary summary = new BatchExecutionSummary();

        summary.lastExecutionId = jobExecution.getId();
        summary.lastExecutionTime = jobExecution.getEndTime();
        summary.status = jobExecution.getStatus().name();
        summary.exitCode = jobExecution.getExitStatus().getExitCode();
        summary.exitMessage = jobExecution.getExitStatus().getExitDescription();
        summary.startTime = jobExecution.getStartTime();
        summary.endTime = jobExecution.getEndTime();

        // Calculate duration
        if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
            Duration duration = Duration.between(
                jobExecution.getStartTime(),
                jobExecution.getEndTime()
            );
            summary.durationSeconds = duration.getSeconds();
        }

        // Aggregate step execution metrics
        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        for (StepExecution step : stepExecutions) {
            summary.itemsRead += step.getReadCount();
            summary.itemsProcessed += step.getReadCount() - step.getReadSkipCount();
            summary.itemsWritten += step.getWriteCount();
            summary.itemsSkipped += step.getReadSkipCount() + step.getWriteSkipCount() + step.getProcessSkipCount();
        }

        // Capture error message if failed
        if (jobExecution.getStatus() == BatchStatus.FAILED && !jobExecution.getAllFailureExceptions().isEmpty()) {
            Throwable firstException = jobExecution.getAllFailureExceptions().get(0);
            summary.errorMessage = firstException.getMessage();
        }

        summary.itemsFailed = summary.itemsSkipped;

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
            Type type = new TypeToken<Map<String, BatchExecutionSummary>>(){}.getType();
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
        log.info("Batch job completed: {} - Status: {}, Duration: {}s, Items: read={}, processed={}, written={}, skipped={}",
            jobName,
            summary.status,
            summary.durationSeconds,
            summary.itemsRead,
            summary.itemsProcessed,
            summary.itemsWritten,
            summary.itemsSkipped);

        if (summary.errorMessage != null) {
            log.error("Job failed with error: {}", summary.errorMessage);
        }
    }

    /**
     * Data structure for batch execution summary
     */
    @SuppressWarnings("unused")
    private static class BatchExecutionSummary {
        Long lastExecutionId;
        LocalDateTime lastExecutionTime;
        String status;
        String exitCode;
        String exitMessage;
        LocalDateTime startTime;
        LocalDateTime endTime;
        Long durationSeconds;
        int itemsRead;
        int itemsProcessed;
        int itemsWritten;
        int itemsSkipped;
        int itemsFailed;
        String errorMessage;
    }
}
