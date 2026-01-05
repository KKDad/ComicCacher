package org.stapledon.engine.batch.scheduler;

import org.springframework.batch.core.launch.JobOperator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler for periodic batch jobs that run at fixed intervals.
 *
 * <p>
 * Features:
 * <ul>
 * <li>Fixed-delay scheduling (time between end of one execution and start of
 * next)</li>
 * <li>No missed execution logic (runs continuously)</li>
 * </ul>
 *
 * <p>
 * Usage: Configure as a Spring bean in job configuration classes:
 *
 * <pre>
 * &#64;Bean
 * public PeriodicJobScheduler myJobScheduler(JobOperator operator) {
 *     return new PeriodicJobScheduler("MyJob", 300000L, operator); // 5 minutes
 * }
 * </pre>
 */
@Slf4j
public class PeriodicJobScheduler extends AbstractJobScheduler {

    private final String jobName;
    private final long fixedDelayMs;

    /**
     * Creates a new PeriodicJobScheduler.
     *
     * @param jobName      the Spring Batch job name
     * @param fixedDelayMs fixed delay in milliseconds between executions
     * @param jobOperator  Spring Batch JobOperator
     */
    public PeriodicJobScheduler(String jobName, long fixedDelayMs, JobOperator jobOperator) {
        super(jobOperator);
        this.jobName = jobName;
        this.fixedDelayMs = fixedDelayMs;
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.PERIODIC;
    }

    @Override
    protected Properties buildJobProperties(String trigger) {
        Properties props = new Properties();
        props.setProperty("trigger", trigger);
        props.setProperty("runId", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")));
        return props;
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
        log.debug("Starting periodic execution of {}", jobName);
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
