package org.stapledon.engine.batch.scheduler;

import org.springframework.batch.core.launch.JobOperator;

import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for job schedulers.
 * Provides common functionality for all batch job schedulers using the modern
 * JobOperator API.
 *
 * <p>
 * Subclasses must implement:
 * <ul>
 * <li>{@link #getJobName()} - Returns the Spring Batch job name</li>
 * <li>{@link #buildJobProperties(String)} - Builds job parameters for
 * execution</li>
 * </ul>
 *
 * <p>
 * All schedulers automatically:
 * <ul>
 * <li>Use JobOperator for job execution (Spring Batch 6 compliant)</li>
 * <li>Log execution start/completion</li>
 * <li>Handle exceptions gracefully</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractJobScheduler {

    protected final JobOperator jobOperator;

    /**
     * Schedule type for categorization and health checks.
     */
    public enum ScheduleType {
        /** Runs once per day at a scheduled cron time */
        DAILY,
        /** Runs periodically with a fixed delay between executions */
        PERIODIC
    }

    /**
     * Returns the Spring Batch job name this scheduler manages.
     *
     * @return the job name (must match the @Bean name in job configuration)
     */
    public abstract String getJobName();

    /**
     * Returns the scheduling type for this scheduler.
     *
     * @return DAILY for cron-based or PERIODIC for fixed-delay
     */
    public abstract ScheduleType getScheduleType();

    /**
     * Builds job properties for execution.
     * Subclasses should include trigger type and any job-specific parameters.
     *
     * @param trigger the trigger source (e.g., "SCHEDULED", "MANUAL", "STARTUP")
     * @return properties to pass to the job
     */
    protected abstract Properties buildJobProperties(String trigger);

    /**
     * Executes the job with the given trigger source.
     * Uses JobOperator.start() which is the modern Spring Batch 6 approach.
     *
     * @param trigger source of the trigger ("SCHEDULED", "MANUAL", "STARTUP")
     * @return the job execution ID, or null if execution failed to start
     */
    protected Long runJob(String trigger) {
        log.info("Launching {} (triggered by: {})", getJobName(), trigger);

        try {
            Properties properties = buildJobProperties(trigger);
            Long executionId = jobOperator.start(getJobName(), properties);
            log.info("{} started with execution ID: {}", getJobName(), executionId);
            return executionId;
        } catch (Exception e) {
            log.error("Failed to launch {}: {}", getJobName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Logs scheduler initialization details.
     * Called by subclasses in their @PostConstruct methods.
     *
     * @param scheduleDescription human-readable schedule description
     */
    protected void logInitialization(String scheduleDescription) {
        log.warn("======== INITIALIZING SCHEDULER: {} ========", getClass().getSimpleName());
        log.info("  Job: {}", getJobName());
        log.info("  Type: {}", getScheduleType());
        log.info("  Schedule: {}", scheduleDescription);
        log.warn("{} scheduler SUCCESSFULLY initialized", getJobName());
    }
}
