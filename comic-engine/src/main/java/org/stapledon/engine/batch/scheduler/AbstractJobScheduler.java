package org.stapledon.engine.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract base class for job schedulers. Provides common functionality for all batch job schedulers using the modern JobOperator API.
 *
 * <p>
 * Subclasses must implement:
 * <ul>
 * <li>{@link #getJobName()} - Returns the Spring Batch job name</li>
 * <li>{@link #buildJobParameters(String)} - Builds job parameters for execution</li>
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

    protected final Job job;
    protected final JobOperator jobOperator;

    // JobOperator.start runs the job on the calling thread, so this is held for the whole run
    private final AtomicBoolean running = new AtomicBoolean();

    /**
     * Schedule type for categorization and health checks.
     */
    public enum ScheduleType {
        /**
         * Runs once per day at a scheduled cron time
         */
        DAILY,
        /**
         * Runs periodically with a fixed delay between executions
         */
        PERIODIC
    }

    /**
     * Returns the Spring Batch job name this scheduler manages.
     *
     * @return the job name (must match the @Bean name in job configuration)
     */
    public String getJobName() {
        return job.getName();
    }

    /**
     * Returns the scheduling type for this scheduler.
     *
     * @return DAILY for cron-based or PERIODIC for fixed-delay
     */
    public abstract ScheduleType getScheduleType();

    /**
     * Builds job parameters for execution. Subclasses can override to add custom parameters.
     *
     * @param trigger the trigger source (e.g., "SCHEDULED", "MANUAL", "STARTUP")
     * @return JobParameters to pass to the job
     */
    protected JobParameters buildJobParameters(String trigger) {
        return buildJobParameters(trigger, Map.of());
    }

    /**
     * Builds job parameters for execution with additional user-supplied parameters.
     */
    protected JobParameters buildJobParameters(String trigger, Map<String, String> extraParams) {
        JobParametersBuilder builder = new JobParametersBuilder()
                .addString("trigger", trigger)
                .addString("runId", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")));
        extraParams.forEach(builder::addString);
        return builder.toJobParameters();
    }

    /**
     * Executes the job with the given trigger source. Uses JobOperator.start(Job, JobParameters) which is the modern Spring Batch 6 approach.
     *
     * @param trigger source of the trigger ("SCHEDULED", "MANUAL", "STARTUP")
     * @return the job execution ID, or null if execution failed to start or the job was already running
     */
    protected Long runJob(String trigger) {
        return runJob(trigger, Map.of());
    }

    /**
     * Executes the job with the given trigger source and extra parameters. Returns null without launching when a run of this job is already in progress.
     */
    protected Long runJob(String trigger, Map<String, String> extraParams) {
        if (!running.compareAndSet(false, true)) {
            log.warn("{} is already running, not launching another run (triggered by: {})", getJobName(), trigger);
            return null;
        }
        log.info("Launching {} (triggered by: {}, params: {})", getJobName(), trigger, extraParams);

        try {
            JobParameters parameters = buildJobParameters(trigger, extraParams);
            org.springframework.batch.core.job.JobExecution execution = jobOperator.start(job, parameters);
            Long executionId = execution.getId();
            // jobOperator.start runs the job synchronously, so the execution has finished here
            log.info("{} execution {} ended: {} (exit code {})", getJobName(), executionId, execution.getStatus(),
                    execution.getExitStatus().getExitCode());
            return executionId;
        } catch (Exception e) {
            log.error("Failed to launch {}", getJobName(), e);
            return null;
        } finally {
            running.set(false);
        }
    }

    /**
     * Logs scheduler initialization details. Called by subclasses in their @PostConstruct methods.
     *
     * @param scheduleDescription human-readable schedule description
     */
    protected void logInitialization(String scheduleDescription) {
        log.info("======== INITIALIZING SCHEDULER: {} ========", getClass().getSimpleName());
        log.info("  Job: {}", getJobName());
        log.info("  Type: {}", getScheduleType());
        log.info("  Schedule: {}", scheduleDescription);
        log.info("{} scheduler initialized", getJobName());
    }
}
