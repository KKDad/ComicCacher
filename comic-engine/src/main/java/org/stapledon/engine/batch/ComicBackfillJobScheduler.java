package org.stapledon.engine.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler for the ComicBackfillJob batch job.
 * Handles scheduling and manual triggering of comic backfill operations.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "batch.comic-backfill.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@SuppressWarnings({ "deprecation", "removal" }) // TODO: Migrate to JobOperator in Spring Batch 6
public class ComicBackfillJobScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier("comicBackfillJob")
    private final Job comicBackfillJob;

    @Value("${batch.comic-backfill.cron}")
    private String cronExpression;

    @Value("${batch.comic-backfill.target-year}")
    private int targetYear;

    @PostConstruct
    public void init() {
        log.info("ComicBackfillJob initialized");
        log.info("  Schedule: {}", cronExpression);
        log.info("  Target Year: {}", targetYear);
        log.info("  Status: ENABLED");
    }

    /**
     * Scheduled execution of ComicBackfillJob
     */
    @Scheduled(cron = "${batch.comic-backfill.cron}")
    public void runDailyBackfill() {
        log.info("Starting scheduled comic backfill for year {}", targetYear);
        try {
            runJob("SCHEDULED");
        } catch (Exception e) {
            log.error("Failed to run scheduled comic backfill", e);
        }
    }

    /**
     * Manually run ComicBackfillJob
     */
    public JobExecution runJob(String trigger) throws Exception {
        log.info("Launching ComicBackfillJob (triggered by: {})", trigger);

        JobParametersBuilder parametersBuilder = new JobParametersBuilder();
        parametersBuilder.addLong("timestamp", System.currentTimeMillis());
        parametersBuilder.addString("trigger", trigger);
        parametersBuilder.addLong("targetYear", (long) targetYear);

        JobExecution execution = jobLauncher.run(comicBackfillJob, parametersBuilder.toJobParameters());

        log.info("ComicBackfillJob execution started with ID: {}", execution.getId());
        return execution;
    }
}
