package org.stapledon.engine.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
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
 * Scheduler for the RetrievalRecordPurgeJob batch job.
 * Handles scheduling and manual triggering of retrieval record purging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.record-purge.enabled", havingValue = "true", matchIfMissing = true)
@SuppressWarnings({ "deprecation", "removal" }) // TODO: Migrate to JobOperator in Spring Batch 6
public class RetrievalRecordPurgeJobScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier("retrievalRecordPurgeJob")
    private final Job retrievalRecordPurgeJob;

    @Value("${batch.record-purge.cron}")
    private String cronExpression;

    @Value("${batch.timezone:America/Toronto}")
    private String timezone;

    /**
     * Log schedule information when the scheduler is initialized
     */
    @PostConstruct
    public void logScheduleInfo() {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            ZonedDateTime nextRun = cron.next(ZonedDateTime.now(ZoneId.of(timezone)));
            log.info("RetrievalRecordPurgeJob scheduler initialized - Next scheduled run: {} ({})",
                    nextRun.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    nextRun.getZone());
        } catch (Exception e) {
            log.warn("Could not parse cron expression '{}': {}", cronExpression, e.getMessage());
        }
    }

    /**
     * Scheduled execution of RetrievalRecordPurgeJob
     * Runs at 6:45 AM EST (America/Toronto timezone) every day
     */
    @Scheduled(cron = "${batch.record-purge.cron}", zone = "${batch.timezone}")
    public void runDailyRecordPurge() {
        log.info("Starting scheduled retrieval record purge");

        try {
            runRecordPurgeJob("SCHEDULED");
        } catch (Exception e) {
            log.error("Failed to run scheduled retrieval record purge", e);
        }
    }

    /**
     * Manually run RetrievalRecordPurgeJob
     */
    public JobExecution runRecordPurgeJob(String trigger) throws Exception {
        log.info("Launching RetrievalRecordPurgeJob (triggered by: {})", trigger);

        JobParametersBuilder parametersBuilder = new JobParametersBuilder()
                .addString("trigger", trigger)
                .addString("runId", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")));

        JobExecution execution = jobLauncher.run(retrievalRecordPurgeJob, parametersBuilder.toJobParameters());

        log.info("Job launched with execution ID: {}", execution.getId());
        return execution;
    }
}
