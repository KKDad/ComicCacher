package org.stapledon.infrastructure.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.stapledon.infrastructure.config.properties.DailyRunnerProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing comic retrieval batch jobs.
 * Replaces the old DailyRunner with Spring Batch for better tracking and observability.
 * Provides scheduling, manual execution, and monitoring capabilities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComicBatchService implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final Job dailyComicRetrievalJob;
    private final JobExplorer jobExplorer;
    private final JobRepository jobRepository;
    private final DailyRunnerProperties dailyRunnerProperties;

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
            log.info("Daily comic retrieval already ran today, skipping immediate execution");
        } else {
            log.info("Daily comic retrieval has not run today, executing immediately");
            try {
                runComicRetrievalJob(LocalDate.now(), "STARTUP");
            } catch (Exception e) {
                log.error("Failed to run startup comic retrieval", e);
            }
        }
    }

    /**
     * Scheduled execution of daily comic retrieval job
     * Runs at 7:00 AM every day
     */
    @Scheduled(cron = "0 0 7 * * ?")
    public void runDailyComicRetrieval() {
        if (!dailyRunnerProperties.isEnabled()) {
            log.info("Daily runner is disabled, skipping scheduled comic retrieval");
            return;
        }

        // Check if job already ran today
        if (hasJobRunToday()) {
            log.info("Daily comic retrieval already ran today, skipping");
            return;
        }

        try {
            runComicRetrievalJob(LocalDate.now(), "SCHEDULED");
        } catch (Exception e) {
            log.error("Failed to run scheduled daily comic retrieval", e);
        }
    }

    /**
     * Manually run comic retrieval job for a specific date
     */
    public JobExecution runComicRetrievalJob(LocalDate targetDate, String trigger) throws Exception {
        log.info("Launching comic retrieval job for date: {} (triggered by: {})", targetDate, trigger);

        JobParametersBuilder parametersBuilder = new JobParametersBuilder()
                .addString("targetDate", targetDate.toString())
                .addString("trigger", trigger)
                .addString("runId", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")));

        JobExecution execution = jobLauncher.run(dailyComicRetrievalJob, parametersBuilder.toJobParameters());
        
        log.info("Job launched with execution ID: {}", execution.getId());
        return execution;
    }

    /**
     * Get recent job executions for monitoring
     */
    public List<JobExecution> getRecentJobExecutions(int count) {
        List<JobInstance> jobInstances = jobExplorer.getJobInstances("dailyComicRetrievalJob", 0, count);
        
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
        List<JobInstance> jobInstances = jobExplorer.getJobInstances("dailyComicRetrievalJob", 0, 100);
        
        return jobInstances.stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .filter(execution -> {
                    if (execution.getStartTime() == null) return false;
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
                .anyMatch(execution -> execution.getStatus().isGreaterThan(org.springframework.batch.core.BatchStatus.STARTED));
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