package org.stapledon.engine.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler for the MetricsArchiveJob batch job.
 * Handles scheduling and manual triggering of metrics archiving.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.metrics-archive.enabled", havingValue = "true", matchIfMissing = true)
@SuppressWarnings({ "deprecation", "removal" }) // TODO: Migrate to JobOperator in Spring Batch 6
public class MetricsArchiveJobScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier("metricsArchiveJob")
    private final Job metricsArchiveJob;

    @Value("${batch.metrics-archive.cron}")
    private String cronExpression;

    @Value("${batch.timezone:America/Toronto}")
    private String timezone;

    /**
     * Log schedule information when the scheduler is initialized
     */
    @PostConstruct
    public void logScheduleInfo() {
        log.warn("======== INITIALIZING SCHEDULER: MetricsArchiveJobScheduler ========");
        try {
            log.info("Cron expression: {}", cronExpression);
            log.info("Timezone: {}", timezone);
            CronExpression cron = CronExpression.parse(cronExpression);
            ZonedDateTime nextRun = cron.next(ZonedDateTime.now(ZoneId.of(timezone)));
            log.warn("MetricsArchiveJob scheduler SUCCESSFULLY initialized - Next run: {} ({})",
                    nextRun.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    nextRun.getZone());
        } catch (Exception e) {
            log.error("======== FAILED TO INITIALIZE SCHEDULER: MetricsArchiveJobScheduler ========", e);
            throw new RuntimeException("MetricsArchiveJobScheduler initialization failed", e);
        }
    }

    /**
     * Scheduled execution of MetricsArchiveJob
     * Runs at 6:30 AM EST (America/Toronto timezone) every day
     */
    @Scheduled(cron = "${batch.metrics-archive.cron}", zone = "${batch.timezone}")
    public void runDailyMetricsArchive() {
        log.info("Starting scheduled metrics archive");

        try {
            runMetricsArchiveJob("SCHEDULED");
        } catch (Exception e) {
            log.error("Failed to run scheduled metrics archive", e);
        }
    }

    /**
     * Manually run MetricsArchiveJob
     */
    public JobExecution runMetricsArchiveJob(String trigger) throws Exception {
        log.info("Launching MetricsArchiveJob (triggered by: {})", trigger);

        JobParametersBuilder parametersBuilder = new JobParametersBuilder()
                .addString("trigger", trigger)
                .addString("runId", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")));

        JobExecution execution = jobLauncher.run(metricsArchiveJob, parametersBuilder.toJobParameters());

        log.info("Job launched with execution ID: {}", execution.getId());
        return execution;
    }
}
