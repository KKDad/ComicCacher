package org.stapledon.engine.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.stapledon.engine.batch.dto.BatchExecutionSummary;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for monitoring batch job executions.
 *
 * <p>History queries are delegated to {@link JsonBatchExecutionTracker} which
 * persists execution data to JSON on NFS. The H2 {@link JobRepository} is only
 * used for in-flight job lookup (e.g. immediately after triggering a job).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchJobMonitoringService {

    private final JobRepository jobRepository;
    private final JsonBatchExecutionTracker executionTracker;

    /**
     * Get recent job executions for a specific job from persisted JSON history.
     */
    public List<BatchExecutionSummary> getRecentJobExecutions(String jobName, int count) {
        return executionTracker.getExecutionHistory(jobName, count);
    }

    /**
     * Get job executions for a specific date range from persisted JSON history.
     */
    public List<BatchExecutionSummary> getJobExecutionsForDateRange(String jobName, LocalDate startDate, LocalDate endDate) {
        return executionTracker.getExecutionHistoryForDateRange(jobName, startDate, endDate);
    }

    /**
     * Get a specific job execution by ID from H2 (for in-flight jobs).
     */
    public JobExecution getJobExecution(Long executionId) {
        try {
            return jobRepository.getJobExecution(executionId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            log.debug("No job execution found for ID: {}", executionId);
            return null;
        }
    }

    /**
     * Get job execution summary statistics from persisted JSON history.
     */
    public ComicJobSummary getJobSummary(String jobName, int dayCount) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(dayCount - 1);

        List<BatchExecutionSummary> executions = getJobExecutionsForDateRange(jobName, startDate, endDate);

        long successCount = executions.stream()
                .filter(e -> "COMPLETED".equals(e.getStatus()))
                .count();

        long failureCount = executions.stream()
                .filter(e -> "FAILED".equals(e.getStatus()))
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
