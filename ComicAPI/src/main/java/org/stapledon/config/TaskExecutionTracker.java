package org.stapledon.config;

import java.time.LocalDate;

/**
 * Interface for tracking execution of scheduled tasks
 * Used to ensure tasks like DailyRunner and StartupReconciler only run once per day
 */
public interface TaskExecutionTracker {

    /**
     * Check if a task can run today
     * 
     * @param taskName The name of the task to check
     * @return true if the task has not run today and can run, false otherwise
     */
    boolean canRunToday(String taskName);

    /**
     * Mark a task as having been executed today
     * 
     * @param taskName The name of the task to mark as executed
     * @return true if the task was successfully marked as executed, false otherwise
     */
    boolean markTaskExecuted(String taskName);
    
    /**
     * Get the last execution date for a task
     * 
     * @param taskName The name of the task to check
     * @return The LocalDate when the task was last executed, or null if the task has never run
     */
    LocalDate getLastExecutionDate(String taskName);
}