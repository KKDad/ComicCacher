package org.stapledon.engine.batch.scheduler;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobOperator;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler for periodic batch jobs that run at fixed intervals.
 *
 * <p>
 * Features:
 * <ul>
 * <li>Fixed-delay scheduling (time between end of one execution and start of next)</li>
 * <li>No missed execution logic (runs continuously)</li>
 * </ul>
 *
 * <p>
 * Usage: Configure as a Spring bean in job configuration classes:
 *
 * <pre>
 * &#64;Bean
 * public PeriodicJobScheduler myJobScheduler(Job myJob, JobOperator operator) {
 *     return new PeriodicJobScheduler(myJob, 300000L, operator); // 5 minutes
 * }
 * </pre>
 */
@Slf4j
public class PeriodicJobScheduler extends AbstractJobScheduler {

    private final long fixedDelayMs;

    /**
     * Creates a new PeriodicJobScheduler.
     *
     * @param job the Spring Batch Job bean
     * @param fixedDelayMs fixed delay in milliseconds between executions
     * @param jobOperator Spring Batch JobOperator
     */
    public PeriodicJobScheduler(Job job, long fixedDelayMs, JobOperator jobOperator) {
        super(job, jobOperator);
        this.fixedDelayMs = fixedDelayMs;
    }

    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.PERIODIC;
    }

    /**
     * Initializes the scheduler with logging.
     */
    @PostConstruct
    public void init() {
        long minutes = fixedDelayMs / 60000;
        long seconds = (fixedDelayMs % 60000) / 1000;

        String scheduleDesc;
        if (seconds > 0) {
            scheduleDesc = String.format("Every %d minutes %d seconds", minutes, seconds);
        } else {
            scheduleDesc = String.format("Every %d minutes", minutes);
        }

        logInitialization(scheduleDesc);
    }

    /**
     * Scheduled execution method - to be called by @Scheduled in config beans.
     */
    public void executeScheduled() {
        log.debug("Starting periodic execution of {}", getJobName());
        runJob("SCHEDULED");
    }

    /**
     * Manually triggers the job (e.g., via API).
     *
     * @return the execution ID, or null if failed
     */
    public Long triggerManually() {
        return runJob("MANUAL");
    }

    /**
     * Returns the fixed delay in milliseconds.
     */
    public long getFixedDelayMs() {
        return fixedDelayMs;
    }
}
