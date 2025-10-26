package org.stapledon.common.infrastructure.config;

import java.time.LocalDate;

/**
 * Interface for tracking execution of scheduled tasks.
 * Used to ensure tasks like DailyRunner and StartupReconciler only run once per day.
 */
public interface ExecutionTracker {

    /**
     * Check if a task can run today.
     */
    boolean canRunToday(String taskName);

    /**
     * Mark a task as having been executed today.
     */
    boolean markTaskExecuted(String taskName);

    /**
     * Get the last execution date for a task, or null if never executed.
     */
    LocalDate getLastExecutionDate(String taskName);
}
