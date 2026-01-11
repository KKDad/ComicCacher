package org.stapledon.engine.batch.scheduler;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.support.CronExpression;
import org.stapledon.engine.batch.JsonBatchExecutionTracker;

import jakarta.annotation.PostConstruct;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler for daily batch jobs that run once per day on a cron schedule.
 *
 * <p>
 * Features:
 * <ul>
 * <li>Cron-based scheduling with timezone support</li>
 * <li>Missed execution detection on startup using batch-executions.json</li>
 * <li>Automatic job execution if missed scheduled time</li>
 * </ul>
 *
 * <p>
 * Usage: Configure as a Spring bean in job configuration classes:
 *
 * <pre>
 * &#64;Bean
 * public DailyJobScheduler myJobScheduler(Job myJob, JobOperator operator, JsonBatchExecutionTracker tracker) {
 *     return new DailyJobScheduler(myJob, "${batch.my-job.cron}", "${batch.timezone}", operator, tracker);
 * }
 * </pre>
 */
@Slf4j
public class DailyJobScheduler extends AbstractJobScheduler {

    private final String cronExpression;
    private final String timezone;
    private final JsonBatchExecutionTracker executionTracker;

    /**
     * Creates a new DailyJobScheduler.
     *
     * @param job the Spring Batch Job bean
     * @param cronExpression cron expression for scheduling
     * @param timezone timezone for cron evaluation (e.g., "America/Toronto")
     * @param jobOperator Spring Batch JobOperator
     * @param executionTracker tracker for reading/writing execution history
     */
    public DailyJobScheduler(Job job, String cronExpression, String timezone, JobOperator jobOperator, JsonBatchExecutionTracker executionTracker) {
        super(job, jobOperator);
        this.cronExpression = cronExpression;
        this.timezone = timezone;
        this.executionTracker = executionTracker;
    }

    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.DAILY;
    }

    /**
     * Initializes the scheduler with logging. Note: Missed execution checks are handled by StartupJobRunner after ApplicationReadyEvent to ensure all dependencies are ready.
     */
    @PostConstruct
    public void init() {
        logInitializationWithNextRun();
    }

    /**
     * Scheduled execution method - to be called by @Scheduled in config beans. Checks if already run today to prevent duplicate executions.
     */
    public void executeScheduled() {
        if (executionTracker.hasJobRunToday(getJobName())) {
            log.info("{} already ran today, skipping scheduled execution", getJobName());
            return;
        }

        log.info("Starting scheduled execution of {}", getJobName());
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
     * Checks if the job missed its scheduled execution time and runs if needed. Called by StartupJobRunner after ApplicationReadyEvent ensures all beans are ready.
     */
    public void runMissedExecutionIfNeeded() {
        if (executionTracker.hasJobRunToday(getJobName())) {
            log.info("{} has already run today, no makeup run needed", getJobName());
            return;
        }

        // Check if we're past the scheduled time for today
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
        ZonedDateTime todayScheduledTime = getNextScheduledTime(now.minusDays(1));

        if (todayScheduledTime != null && now.isAfter(todayScheduledTime)) {
            log.warn("{} missed scheduled time ({}), running now", getJobName(), todayScheduledTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            runJob("STARTUP_MAKEUP");
        }
    }

    /**
     * Logs initialization with next scheduled run time.
     */
    private void logInitializationWithNextRun() {
        try {
            ZonedDateTime nextRun = getNextScheduledTime(ZonedDateTime.now(ZoneId.of(timezone)));
            log.warn("======== INITIALIZING SCHEDULER: {} ========", getClass().getSimpleName());
            log.info("  Job: {}", getJobName());
            log.info("  Type: {}", getScheduleType());
            log.info("  Cron: {} ({})", cronExpression, timezone);
            if (nextRun != null) {
                log.warn("{} scheduler SUCCESSFULLY initialized - Next run: {} ({})", getJobName(), nextRun.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), nextRun.getZone());
            }
        } catch (Exception e) {
            log.error("======== FAILED TO INITIALIZE SCHEDULER: {} ========", getClass().getSimpleName(), e);
            throw new IllegalStateException("Failed to initialize " + getJobName() + " scheduler", e);
        }
    }

    /**
     * Calculates the next scheduled execution time.
     */
    private ZonedDateTime getNextScheduledTime(ZonedDateTime from) {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            return cron.next(from);
        } catch (Exception e) {
            log.error("Failed to parse cron expression '{}': {}", cronExpression, e.getMessage());
            return null;
        }
    }

    /**
     * Returns the cron expression used for scheduling.
     */
    public String getCronExpression() {
        return cronExpression;
    }

    /**
     * Returns the timezone used for cron evaluation.
     */
    public String getTimezone() {
        return timezone;
    }
}
