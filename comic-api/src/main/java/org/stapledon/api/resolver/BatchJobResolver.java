package org.stapledon.api.resolver;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.api.dto.batch.BatchJobDto;
import org.stapledon.api.dto.batch.BatchJobSummaryDto;
import org.stapledon.api.dto.batch.BatchStepDto;
import org.stapledon.api.dto.batch.DailyJobStatsDto;
import org.stapledon.api.dto.payload.MutationPayloads.TriggerBatchJobPayload;
import org.stapledon.engine.batch.BatchJobBaseConfig;
import org.stapledon.engine.batch.BatchJobMonitoringService;
import org.stapledon.engine.batch.scheduler.DailyJobScheduler;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * GraphQL resolver for batch job queries and mutations.
 */
@Slf4j
@Controller
public class BatchJobResolver {

    private final BatchJobMonitoringService monitoringService;
    private final List<DailyJobScheduler> schedulers;

    /**
     * Constructs a BatchJobResolver with required dependencies.
     */
    public BatchJobResolver(BatchJobMonitoringService monitoringService, @Autowired(required = false) List<DailyJobScheduler> schedulers) {
        this.monitoringService = monitoringService;
        this.schedulers = schedulers != null ? schedulers : List.of();
    }

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Get recent batch job executions across all known jobs.
     */
    @QueryMapping
    public List<BatchJobDto> recentBatchJobs(@Argument int count) {
        log.debug("GraphQL: Getting {} recent batch jobs", count);
        return BatchJobBaseConfig.KNOWN_JOBS.stream()
                .flatMap(jobName -> monitoringService.getRecentJobExecutions(jobName, count).stream())
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .limit(count)
                .map(this::mapJobExecution)
                .toList();
    }

    /**
     * Get batch jobs within a date range across all known jobs.
     */
    @QueryMapping
    public List<BatchJobDto> batchJobsByDateRange(@Argument LocalDate startDate, @Argument LocalDate endDate) {
        log.debug("GraphQL: Getting batch jobs from {} to {}", startDate, endDate);
        return BatchJobBaseConfig.KNOWN_JOBS.stream()
                .flatMap(jobName -> monitoringService.getJobExecutionsForDateRange(jobName, startDate, endDate).stream())
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .map(this::mapJobExecution)
                .toList();
    }

    /**
     * Get a specific batch job execution by ID.
     */
    @QueryMapping
    public BatchJobDto batchJob(@Argument int executionId) {
        log.debug("GraphQL: Getting batch job execution {}", executionId);
        JobExecution execution = monitoringService.getJobExecution((long) executionId);
        return execution != null ? mapJobExecution(execution) : null;
    }

    /**
     * Get summary statistics for batch jobs over a number of days.
     */
    @QueryMapping
    public BatchJobSummaryDto batchJobSummary(@Argument int days) {
        log.debug("GraphQL: Getting batch job summary for {} days", days);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<JobExecution> allExecutions = BatchJobBaseConfig.KNOWN_JOBS.stream()
                .flatMap(jobName -> monitoringService.getJobExecutionsForDateRange(jobName, startDate, endDate).stream())
                .toList();

        int total = allExecutions.size();
        int success = (int) allExecutions.stream()
                .filter(e -> e.getStatus() == BatchStatus.COMPLETED).count();
        int failure = (int) allExecutions.stream()
                .filter(e -> e.getStatus() == BatchStatus.FAILED).count();
        int running = (int) allExecutions.stream()
                .filter(e -> e.getStatus().isRunning()).count();

        OptionalDouble avgOpt = allExecutions.stream()
                .filter(e -> e.getStartTime() != null && e.getEndTime() != null)
                .mapToLong(e -> Duration.between(e.getStartTime(), e.getEndTime()).toMillis())
                .average();
        Double averageDurationMs = avgOpt.isPresent() ? avgOpt.getAsDouble() : null;

        int totalItemsProcessed = allExecutions.stream()
                .flatMap(e -> e.getStepExecutions().stream())
                .mapToInt(s -> (int) s.getReadCount())
                .sum();

        Map<LocalDate, List<JobExecution>> byDate = allExecutions.stream()
                .filter(e -> e.getStartTime() != null)
                .collect(Collectors.groupingBy(e -> e.getStartTime().toLocalDate()));

        List<DailyJobStatsDto> dailyBreakdown = byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DailyJobStatsDto(
                        entry.getKey(),
                        entry.getValue().size(),
                        (int) entry.getValue().stream().filter(e -> e.getStatus() == BatchStatus.COMPLETED).count(),
                        (int) entry.getValue().stream().filter(e -> e.getStatus() == BatchStatus.FAILED).count()))
                .toList();

        return new BatchJobSummaryDto(days, total, success, failure, running, averageDurationMs, totalItemsProcessed, dailyBreakdown);
    }

    // =========================================================================
    // Mutations
    // =========================================================================

    /**
     * Trigger a batch job for comic retrieval.
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TriggerBatchJobPayload triggerBatchJob() {
        log.info("GraphQL: Triggering ComicDownloadJob");
        DailyJobScheduler scheduler = findScheduler("ComicDownloadJob");
        Long executionId = scheduler.triggerManually();
        if (executionId == null) {
            throw new IllegalStateException("Failed to trigger ComicDownloadJob");
        }
        JobExecution execution = monitoringService.getJobExecution(executionId);
        return new TriggerBatchJobPayload(mapJobExecution(execution), List.of());
    }

    /**
     * Trigger a backfill job to retrieve missing comics.
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TriggerBatchJobPayload triggerBackfillJob() {
        log.info("GraphQL: Triggering ComicBackfillJob");
        DailyJobScheduler scheduler = findScheduler("ComicBackfillJob");
        Long executionId = scheduler.triggerManually();
        if (executionId == null) {
            throw new IllegalStateException("Failed to trigger ComicBackfillJob");
        }
        JobExecution execution = monitoringService.getJobExecution(executionId);
        return new TriggerBatchJobPayload(mapJobExecution(execution), List.of());
    }

    // =========================================================================
    // Mapping Helpers
    // =========================================================================

    private BatchJobDto mapJobExecution(JobExecution execution) {
        Double durationMs = null;
        if (execution.getStartTime() != null && execution.getEndTime() != null) {
            durationMs = (double) Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis();
        }

        Map<String, Object> params = new HashMap<>();
        execution.getJobParameters().parameters()
                .forEach(p -> params.put(p.name(), p.value()));

        List<BatchStepDto> steps = execution.getStepExecutions().stream()
                .map(step -> new BatchStepDto(
                        step.getStepName(),
                        step.getStatus().name(),
                        (int) step.getReadCount(),
                        (int) step.getWriteCount(),
                        (int) step.getFilterCount(),
                        (int) step.getSkipCount(),
                        (int) step.getCommitCount(),
                        (int) step.getRollbackCount(),
                        step.getStartTime(),
                        step.getEndTime()))
                .toList();

        return new BatchJobDto(
                execution.getId(),
                execution.getJobInstance().getJobName(),
                execution.getStatus().name(),
                execution.getStartTime(),
                execution.getEndTime(),
                durationMs,
                execution.getExitStatus().getExitCode(),
                execution.getExitStatus().getExitDescription(),
                params,
                steps);
    }

    private DailyJobScheduler findScheduler(String jobName) {
        return schedulers.stream()
                .filter(s -> s.getJobName().equals(jobName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Scheduler not found for job: " + jobName));
    }
}
