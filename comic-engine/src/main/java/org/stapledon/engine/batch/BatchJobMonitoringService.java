package org.stapledon.engine.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.stapledon.engine.batch.dto.BatchExecutionSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
     * Get a persisted execution summary by execution ID from JSON history.
     */
    public Optional<BatchExecutionSummary> getExecutionSummary(long executionId) {
        return executionTracker.getExecution(executionId);
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
}
