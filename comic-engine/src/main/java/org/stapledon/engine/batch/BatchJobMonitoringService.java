package org.stapledon.engine.batch;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for monitoring batch job executions.
 * Provides access to job execution history and statistics.
 *
 * <p>
 * This service extracts JobExplorer query functionality that was previously
 * part of individual scheduler classes, providing a centralized API for
 * job monitoring and reporting.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchJobMonitoringService {

    private final JobExplorer jobExplorer;

    /**
     * Get recent job executions for a specific job.
     *
     * @param jobName the job name to query
     * @param count   maximum number of executions to return
     * @return list of recent job executions, sorted by start time descending
     */
    public List<JobExecution> getRecentJobExecutions(String jobName, int count) {
        List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, count);

        return jobInstances.stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .sorted((e1, e2) -> e2.getStartTime().compareTo(e1.getStartTime()))
                .limit(count)
                .toList();
    }

    /**
     * Get job executions for a specific date range.
     *
     * @param jobName   the job name to query
     * @param startDate start date (inclusive)
     * @param endDate   end date (inclusive)
     * @return list of job executions within the date range
     */
    public List<JobExecution> getJobExecutionsForDateRange(String jobName, LocalDate startDate, LocalDate endDate) {
        List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, 100);

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
     * Get a specific job execution by ID.
     *
     * @param executionId the execution ID
     * @return the job execution, or null if not found
     */
    public JobExecution getJobExecution(Long executionId) {
        return jobExplorer.getJobExecution(executionId);
    }

    /**
     * Get job execution summary statistics.
     *
     * @param jobName  the job name to query
     * @param dayCount number of days to include in summary
     * @return summary statistics
     */
    public ComicJobSummary getJobSummary(String jobName, int dayCount) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(dayCount - 1);

        List<JobExecution> executions = getJobExecutionsForDateRange(jobName, startDate, endDate);

        long successCount = executions.stream()
                .filter(e -> e.getStatus() == org.springframework.batch.core.BatchStatus.COMPLETED)
                .count();

        long failureCount = executions.stream()
                .filter(e -> e.getStatus() == org.springframework.batch.core.BatchStatus.FAILED)
                .count();

        double avgDurationMinutes = executions.stream()
                .filter(e -> e.getStartTime() != null && e.getEndTime() != null)
                .mapToLong(e -> Duration.between(e.getStartTime(), e.getEndTime()).toMinutes())
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
