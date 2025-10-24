package org.stapledon.engine.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
public class RetrievalRecordPurgeJobScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier("retrievalRecordPurgeJob")
    private final Job retrievalRecordPurgeJob;

    /**
     * Scheduled execution of RetrievalRecordPurgeJob
     * Runs at 6:45 AM EST (America/Toronto timezone) every day
     */
    @Scheduled(cron = "${batch.record-purge.cron}")
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
                .addString("runId", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")));

        JobExecution execution = jobLauncher.run(retrievalRecordPurgeJob, parametersBuilder.toJobParameters());

        log.info("Job launched with execution ID: {}", execution.getId());
        return execution;
    }
}
