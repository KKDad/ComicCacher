package org.stapledon.engine.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler for the ComicReconciliationJob batch job.
 * Handles scheduling and manual triggering of comic configuration reconciliation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.reconciliation.enabled", havingValue = "true", matchIfMissing = true)
public class ComicReconciliationJobScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier("comicReconciliationJob")
    private final Job comicReconciliationJob;

    /**
     * Scheduled execution of ComicReconciliationJob
     * Runs at 6:15 AM EST (America/Toronto timezone) every day
     */
    @Scheduled(cron = "${batch.reconciliation.cron}")
    public void runDailyReconciliation() {
        log.info("Starting scheduled comic reconciliation");

        try {
            runReconciliationJob("SCHEDULED");
        } catch (Exception e) {
            log.error("Failed to run scheduled comic reconciliation", e);
        }
    }

    /**
     * Manually run ComicReconciliationJob
     */
    public JobExecution runReconciliationJob(String trigger) throws Exception {
        log.info("Launching ComicReconciliationJob (triggered by: {})", trigger);

        JobParametersBuilder parametersBuilder = new JobParametersBuilder()
                .addString("trigger", trigger)
                .addString("runId", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")));

        JobExecution execution = jobLauncher.run(comicReconciliationJob, parametersBuilder.toJobParameters());

        log.info("Job launched with execution ID: {}", execution.getId());
        return execution;
    }
}
