package org.stapledon.api.resolver;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.engine.batch.BatchJobMonitoringService;
import org.stapledon.engine.batch.ComicJobSummary;
import org.stapledon.engine.batch.scheduler.DailyJobScheduler;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL resolver for Batch Job operations.
 * Provides queries for job monitoring and mutations for triggering jobs.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.comic-download.enabled", havingValue = "true", matchIfMissing = true)
public class BatchJobResolver {

    private final BatchJobMonitoringService batchJobMonitoringService;
    private final DailyJobScheduler comicDownloadJobScheduler;

    private static final String COMIC_DOWNLOAD_JOB = "ComicDownloadJob";

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Get recent batch job executions.
     */
    @QueryMapping
    public List<BatchJobRecord> recentBatchJobs(@Argument Integer count) {
        int jobCount = count != null ? count : 10;
        log.debug("Getting {} recent batch jobs", jobCount);

        List<JobExecution> executions = batchJobMonitoringService.getRecentJobExecutions(COMIC_DOWNLOAD_JOB, jobCount);
        return executions.stream()
                .map(this::convertToRecord)
                .toList();
    }

    /**
     * Get batch jobs within a date range.
     */
    @QueryMapping
    public List<BatchJobRecord> batchJobsByDateRange(
            @Argument LocalDate startDate,
            @Argument LocalDate endDate) {
        log.debug("Getting batch jobs from {} to {}", startDate, endDate);

        List<JobExecution> executions = batchJobMonitoringService.getJobExecutionsForDateRange(
                COMIC_DOWNLOAD_JOB, startDate, endDate);
        return executions.stream()
                .map(this::convertToRecord)
                .toList();
    }

    /**
     * Get a specific batch job execution by ID.
     */
    @QueryMapping
    public BatchJobRecord batchJob(@Argument Integer executionId) {
        log.debug("Getting batch job {}", executionId);

        JobExecution execution = batchJobMonitoringService.getJobExecution(executionId.longValue());
        return execution != null ? convertToRecord(execution) : null;
    }

    /**
     * Get summary statistics for batch jobs.
     */
    @QueryMapping
    public ComicJobSummary batchJobSummary(@Argument Integer days) {
        int daysCount = days != null ? days : 7;
        log.debug("Getting batch job summary for {} days", daysCount);

        return batchJobMonitoringService.getJobSummary(COMIC_DOWNLOAD_JOB, daysCount);
    }

    // =========================================================================
    // Schema Mappings - Type Conversions
    // =========================================================================

    /**
     * Convert LocalDateTime to OffsetDateTime for step startTime.
     */
    @SchemaMapping(typeName = "BatchStep", field = "startTime")
    public OffsetDateTime stepStartTime(BatchStepRecord step) {
        return step.startTime();
    }

    /**
     * Convert LocalDateTime to OffsetDateTime for step endTime.
     */
    @SchemaMapping(typeName = "BatchStep", field = "endTime")
    public OffsetDateTime stepEndTime(BatchStepRecord step) {
        return step.endTime();
    }

    // =========================================================================
    // Mutations
    // =========================================================================

    /**
     * Manually trigger a comic download job.
     */
    @MutationMapping
    public TriggerJobResult triggerBatchJob(@Argument LocalDate targetDate) {
        LocalDate date = targetDate != null ? targetDate : LocalDate.now();
        log.info("Triggering batch job for date: {}", date);

        try {
            Long executionId = comicDownloadJobScheduler.triggerManually();
            if (executionId == null) {
                return new TriggerJobResult(null, "FAILED", "Failed to start job");
            }
            return new TriggerJobResult(executionId.intValue(), "STARTED", null);
        } catch (Exception e) {
            log.error("Failed to trigger batch job", e);
            return new TriggerJobResult(null, "FAILED", e.getMessage());
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private BatchJobRecord convertToRecord(JobExecution execution) {
        OffsetDateTime startTime = execution.getStartTime() != null
                ? execution.getStartTime().atOffset(ZoneOffset.UTC)
                : null;
        OffsetDateTime endTime = execution.getEndTime() != null
                ? execution.getEndTime().atOffset(ZoneOffset.UTC)
                : null;

        Long durationMs = null;
        if (startTime != null && endTime != null) {
            durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        }

        List<BatchStepRecord> steps = execution.getStepExecutions().stream()
                .map(this::convertStepToRecord)
                .toList();

        return new BatchJobRecord(
                (int) execution.getId(),
                execution.getJobInstance().getJobName(),
                execution.getStatus().toString(),
                startTime,
                endTime,
                durationMs,
                execution.getExitStatus().getExitCode(),
                execution.getExitStatus().getExitDescription(),
                null, // JobParameters conversion not needed for GraphQL
                steps);
    }

    private BatchStepRecord convertStepToRecord(StepExecution step) {
        OffsetDateTime startTime = step.getStartTime() != null
                ? step.getStartTime().atOffset(ZoneOffset.UTC)
                : null;
        OffsetDateTime endTime = step.getEndTime() != null
                ? step.getEndTime().atOffset(ZoneOffset.UTC)
                : null;

        return new BatchStepRecord(
                step.getStepName(),
                step.getStatus().toString(),
                (int) step.getReadCount(),
                (int) step.getWriteCount(),
                (int) step.getFilterCount(),
                (int) step.getSkipCount(),
                (int) step.getCommitCount(),
                (int) step.getRollbackCount(),
                startTime,
                endTime);
    }

    // =========================================================================
    // Record Types for GraphQL
    // =========================================================================

    public record BatchJobRecord(
            int executionId,
            String jobName,
            String status,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            Long durationMs,
            String exitCode,
            String exitDescription,
            Map<String, ?> parameters,
            List<BatchStepRecord> steps) {
    }

    public record BatchStepRecord(
            String stepName,
            String status,
            int readCount,
            int writeCount,
            int filterCount,
            int skipCount,
            int commitCount,
            int rollbackCount,
            OffsetDateTime startTime,
            OffsetDateTime endTime) {
    }

    public record TriggerJobResult(
            Integer executionId,
            String status,
            String errorMessage) {
    }
}
