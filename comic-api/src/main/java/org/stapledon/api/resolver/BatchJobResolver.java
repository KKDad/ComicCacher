package org.stapledon.api.resolver;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.stapledon.api.dto.batch.BatchJobDto;
import org.stapledon.api.dto.batch.BatchJobSummaryDto;
import org.stapledon.api.dto.batch.BatchSchedulerInfoDto;
import org.stapledon.api.dto.batch.BatchStepDto;
import org.stapledon.api.dto.batch.DailyJobStatsDto;
import org.stapledon.api.dto.payload.MutationPayloads.ToggleJobSchedulerPayload;
import org.stapledon.api.dto.payload.MutationPayloads.TriggerBatchJobPayload;
import org.stapledon.api.dto.payload.UserError;
import org.stapledon.engine.batch.BatchJobBaseConfig;
import org.stapledon.engine.batch.BatchJobMonitoringService;
import org.stapledon.engine.batch.logging.BatchJobLogService;
import org.stapledon.engine.batch.scheduler.DailyJobScheduler;
import org.stapledon.engine.batch.scheduler.SchedulerStateService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL resolver for batch job queries and mutations.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class BatchJobResolver {

    private final BatchJobMonitoringService monitoringService;
    private final List<DailyJobScheduler> schedulers;
    private final SchedulerStateService schedulerStateService;
    private final BatchJobLogService batchJobLogService;

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

    /**
     * Get scheduler info for all batch jobs, including runtime pause state.
     */
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<BatchSchedulerInfoDto> batchSchedulers() {
        log.debug("GraphQL: Getting batch scheduler info");
        return schedulers.stream()
                .map(this::mapSchedulerInfo)
                .toList();
    }

    /**
     * Get the execution log for a specific batch job run.
     */
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String batchJobLog(@Argument int executionId, @Argument String jobName) {
        log.debug("GraphQL: Getting log for {} execution {}", jobName, executionId);
        return batchJobLogService.getExecutionLog(executionId, jobName).orElse(null);
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
        return triggerJob("ComicDownloadJob");
    }

    /**
     * Trigger a backfill job to retrieve missing comics.
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TriggerBatchJobPayload triggerBackfillJob() {
        return triggerJob("ComicBackfillJob");
    }

    /**
     * Trigger any batch job by name.
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TriggerBatchJobPayload triggerJob(@Argument String jobName) {
        log.info("GraphQL: Triggering {}", jobName);

        Optional<DailyJobScheduler> schedulerOpt = schedulers.stream()
                .filter(s -> s.getJobName().equals(jobName))
                .findFirst();

        if (schedulerOpt.isEmpty()) {
            return new TriggerBatchJobPayload(null, List.of(new UserError("Job not available (disabled in configuration)", "jobName", null)));
        }

        try {
            Long executionId = schedulerOpt.get().triggerManually();
            if (executionId == null) {
                return new TriggerBatchJobPayload(null, List.of(new UserError("Failed to start job " + jobName, "jobName", null)));
            }
            JobExecution execution = monitoringService.getJobExecution(executionId);
            return new TriggerBatchJobPayload(mapJobExecution(execution), List.of());
        } catch (Exception e) {
            log.error("Error triggering {}: {}", jobName, e.getMessage(), e);
            return new TriggerBatchJobPayload(null, List.of(new UserError(e.getMessage(), "jobName", null)));
        }
    }

    /**
     * Pause or resume a batch job scheduler.
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ToggleJobSchedulerPayload toggleJobScheduler(@Argument String jobName, @Argument boolean paused) {
        log.info("GraphQL: {} job {}", paused ? "Pausing" : "Resuming", jobName);

        Optional<DailyJobScheduler> schedulerOpt = schedulers.stream()
                .filter(s -> s.getJobName().equals(jobName))
                .findFirst();

        if (schedulerOpt.isEmpty()) {
            return new ToggleJobSchedulerPayload(null, List.of(new UserError("Job not available (disabled in configuration)", "jobName", null)));
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        schedulerStateService.setPaused(jobName, paused, username);

        return new ToggleJobSchedulerPayload(mapSchedulerInfo(schedulerOpt.get()), List.of());
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

    private BatchSchedulerInfoDto mapSchedulerInfo(DailyJobScheduler scheduler) {
        String jobName = scheduler.getJobName();
        boolean paused = schedulerStateService.isPaused(jobName);
        var stateOpt = schedulerStateService.getState(jobName);

        OffsetDateTime nextRunTime = computeNextRunTime(scheduler);
        OffsetDateTime lastToggled = stateOpt
                .map(SchedulerStateService.SchedulerState::lastToggled)
                .map(ldt -> ldt.atOffset(ZoneOffset.UTC))
                .orElse(null);

        return new BatchSchedulerInfoDto(
                jobName,
                scheduler.getCronExpression(),
                scheduler.getTimezone(),
                nextRunTime,
                true,
                paused,
                lastToggled,
                stateOpt.map(SchedulerStateService.SchedulerState::toggledBy).orElse(null));
    }

    private OffsetDateTime computeNextRunTime(DailyJobScheduler scheduler) {
        try {
            CronExpression cron = CronExpression.parse(scheduler.getCronExpression());
            return Optional.ofNullable(cron.next(ZonedDateTime.now(ZoneId.of(scheduler.getTimezone()))))
                    .map(ZonedDateTime::toOffsetDateTime)
                    .orElse(null);
        } catch (Exception e) {
            log.error("Failed to compute next run time for {}: {}", scheduler.getJobName(), e.getMessage());
            return null;
        }
    }

    private DailyJobScheduler findScheduler(String jobName) {
        return schedulers.stream()
                .filter(s -> s.getJobName().equals(jobName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Scheduler not found for job: " + jobName));
    }
}
