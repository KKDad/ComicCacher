package org.stapledon.engine.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobLauncher;
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
 * Scheduler for the ImageMetadataBackfillJob batch job.
 * Handles scheduling and manual triggering of image metadata backfill.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.image-backfill.enabled", havingValue = "true", matchIfMissing = true)
@SuppressWarnings({ "deprecation", "removal" }) // TODO: Migrate to JobOperator in Spring Batch 6
public class ImageMetadataJobScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier("imageMetadataBackfillJob")
    private final Job imageMetadataBackfillJob;

    @Value("${batch.image-backfill.cron}")
    private String cronExpression;

    @Value("${batch.timezone:America/Toronto}")
    private String timezone;

    /**
     * Log schedule information when the scheduler is initialized
     */
    @PostConstruct
    public void logScheduleInfo() {
        log.warn("======== INITIALIZING SCHEDULER: ImageMetadataJobScheduler ========");
        try {
            log.info("Cron expression: {}", cronExpression);
            log.info("Timezone: {}", timezone);
            CronExpression cron = CronExpression.parse(cronExpression);
            ZonedDateTime nextRun = cron.next(ZonedDateTime.now(ZoneId.of(timezone)));
            log.warn("ImageMetadataJob scheduler SUCCESSFULLY initialized - Next run: {} ({})",
                    nextRun.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    nextRun.getZone());
        } catch (Exception e) {
            log.error("======== FAILED TO INITIALIZE SCHEDULER: ImageMetadataJobScheduler ========", e);
            throw new RuntimeException("ImageMetadataJobScheduler initialization failed", e);
        }
    }

    /**
     * Scheduled execution of ImageMetadataBackfillJob
     * Runs at 6:30 AM EST (America/Toronto timezone) every day
     */
    @Scheduled(cron = "${batch.image-backfill.cron}")
    public void runDailyImageBackfill() {
        log.info("Starting scheduled image metadata backfill");

        try {
            runImageMetadataJob("SCHEDULED");
        } catch (Exception e) {
            log.error("Failed to run scheduled image metadata backfill", e);
        }
    }

    /**
     * Manually run ImageMetadataBackfillJob
     */
    public JobExecution runImageMetadataJob(String trigger) throws Exception {
        log.info("Launching ImageMetadataBackfillJob (triggered by: {})", trigger);

        JobParametersBuilder parametersBuilder = new JobParametersBuilder()
                .addString("trigger", trigger)
                .addString("runId", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")));

        JobExecution execution = jobLauncher.run(imageMetadataBackfillJob, parametersBuilder.toJobParameters());

        log.info("Job launched with execution ID: {}", execution.getId());
        return execution;
    }
}
