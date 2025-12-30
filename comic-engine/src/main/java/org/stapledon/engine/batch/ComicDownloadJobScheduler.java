package org.stapledon.engine.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.stapledon.common.config.properties.DailyRunnerProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler for the ComicDownloadJob batch job.
 * Handles scheduling, startup execution, and manual triggering of comic
 * downloads.
 * Provides monitoring capabilities and execution history.
 */
@Slf4j
@ToString
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.comic-download.enabled", havingValue = "true", matchIfMissing = true)
@SuppressWarnings({ "deprecation", "removal" }) // TODO: Migrate to JobOperator and JobRepository in Spring Batch 6
public class ComicDownloadJobScheduler implements CommandLineRunner {

    private final JobLauncher jobLauncher;

    @Qualifier("comicDownloadJob")
    private final Job comicDownloadJob;

    private final JobExplorer jobExplorer;
    private final JobRepository jobRepository;
    private final DailyRunnerProperties dailyRunnerProperties;
    private final org.stapledon.common.service.ErrorTrackingService errorTrackingService;

    @Value("${batch.comic-download.cron}")
    private String cronExpression;

    @Value("${batch.timezone:America/Toronto}")
    private String timezone;

    /**
     * Log schedule information when the scheduler is initialized
     */
    @PostConstruct
    public void logScheduleInfo() {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            ZonedDateTime nextRun = cron.next(ZonedDateTime.now(ZoneId.of(timezone)));
            log.info("ComicDownloadJob scheduler initialized - Next scheduled run: {} ({})",
                    nextRun.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    nextRun.getZone());
            log.info("ComicDownloadJob will also run immediately on startup if not yet executed today");
        } catch (Exception e) {
            log.warn("Could not parse cron expression '{}': {}", cronExpression, e.getMessage());
        }
    }

    /**
     * CommandLineRunner implementation - runs immediately when application starts
     * if the job hasn't run today yet.
     */
    @Override
    public void run(String... args) throws Exception {
        if (!dailyRunnerProperties.isEnabled()) {
            log.warn("Daily runner is disabled");
            return;
        }

        // Check if job already ran today
        if (hasJobRunToday()) {
            log.info("ComicDownloadJob already ran today, skipping immediate execution");
        } else {
            log.info("ComicDownloadJob has not run today, executing immediately");
            try {
                runComicDownloadJob(LocalDate.now(), "STARTUP");
            } catch (Exception e) {
                log.error("Failed to run startup comic download", e);
            }
        }
    }

    /**
     * Scheduled execution of ComicDownloadJob
     * Runs at 6:00 AM EST (America/Toronto timezone) every day
     */
    @Scheduled(cron = "${batch.comic-download.cron}", zone = "${batch.timezone}")
    public void runDailyComicDownload() {
        if (!dailyRunnerProperties.isEnabled()) {
            log.info("Comic download job is disabled, skipping scheduled execution");
            return;
        }

        // Check if job already ran today
        if (hasJobRunToday()) {
            log.info("ComicDownloadJob already ran today, skipping");
            return;
        }

        try {
            runComicDownloadJob(LocalDate.now(), "SCHEDULED");
        } catch (Exception e) {
            log.error("Failed to run scheduled comic download", e);
        }
    }

    /**
     * Manually run ComicDownloadJob for a specific date
     */
    public JobExecution runComicDownloadJob(LocalDate targetDate, String trigger) throws Exception {
        log.info("Launching ComicDownloadJob for date: {} (triggered by: {})", targetDate, trigger);

        // Clear old errors before starting new batch run (keep errors from last 48
        // hours)
        try {
            errorTrackingService.clearOldErrors(48);
        } catch (Exception e) {
            log.warn("Failed to clear old errors: {}", e.getMessage());
        }

        JobParametersBuilder parametersBuilder = new JobParametersBuilder()
                .addString("targetDate", targetDate.toString())
                .addString("trigger", trigger)
                .addString("runId", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")));

        JobExecution execution = jobLauncher.run(comicDownloadJob, parametersBuilder.toJobParameters());

        log.info("Job launched with execution ID: {}", execution.getId());
        return execution;
    }

    /**
     * Get recent job executions for monitoring
     */
    public List<JobExecution> getRecentJobExecutions(int count) {
        List<JobInstance> jobInstances = jobExplorer.getJobInstances("ComicDownloadJob", 0, count);

        return jobInstances.stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .sorted((e1, e2) -> e2.getStartTime().compareTo(e1.getStartTime()))
                .limit(count)
                .toList();
    }

    /**
     * Get job executions for a specific date range
     */
    public List<JobExecution> getJobExecutionsForDateRange(LocalDate startDate, LocalDate endDate) {
        List<JobInstance> jobInstances = jobExplorer.getJobInstances("ComicDownloadJob", 0, 100);

        return jobInstances.stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .filter(execution -> {
                    if (execution.getStartTime() == null) {
                        return false;
                    }
                    LocalDate executionDate = execution.getStartTime().toLocalDate();
                    return !executionDate.isBefore(startDate) && !executionDate.isAfter(endDate);
                })
                .sorted((e1, e2) -> e2.getStartTime().compareTo(e1.getStartTime()))
                .toList();
    }

    /**
     * Get job execution by ID
     */
    public JobExecution getJobExecution(Long executionId) {
        return jobExplorer.getJobExecution(executionId);
    }

    /**
     * Check if the job has already run today
     */
    private boolean hasJobRunToday() {
        LocalDate today = LocalDate.now();
        List<JobExecution> todayExecutions = getJobExecutionsForDateRange(today, today);

        return todayExecutions.stream()
                .anyMatch(execution -> execution.getStatus()
                        .isGreaterThan(org.springframework.batch.core.BatchStatus.STARTED));
    }

    /**
     * Get job execution summary statistics
     */
    public ComicJobSummary getJobSummary(int dayCount) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(dayCount - 1);

        List<JobExecution> executions = getJobExecutionsForDateRange(startDate, endDate);

        long successCount = executions.stream()
                .filter(e -> e.getStatus() == org.springframework.batch.core.BatchStatus.COMPLETED)
                .count();

        long failureCount = executions.stream()
                .filter(e -> e.getStatus() == org.springframework.batch.core.BatchStatus.FAILED)
                .count();

        double avgDurationMinutes = executions.stream()
                .filter(e -> e.getStartTime() != null && e.getEndTime() != null)
                .mapToLong(e -> java.time.Duration.between(
                        e.getStartTime(), e.getEndTime()).toMinutes())
                .average()
                .orElse(0.0);

        return ComicJobSummary.builder()
                .totalExecutions(executions.size())
                .successfulExecutions(successCount)
                .failedExecutions(failureCount)
                .successRate(executions.isEmpty() ? 0.0 : (double) successCount / executions.size())
                .averageDurationMinutes(avgDurationMinutes)
                .dateRange(startDate + " to " + endDate)
                .build();
    }
}