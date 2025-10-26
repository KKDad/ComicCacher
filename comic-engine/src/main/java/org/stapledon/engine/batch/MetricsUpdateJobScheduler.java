package org.stapledon.engine.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler for the MetricsUpdateJob batch job.
 * Handles scheduling and manual triggering of metrics updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.metrics-update.enabled", havingValue = "true", matchIfMissing = true)
public class MetricsUpdateJobScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier("metricsUpdateJob")
    private final Job metricsUpdateJob;

    @Value("${batch.metrics-update.fixed-delay}")
    private long fixedDelay;

    /**
     * Log schedule information when the scheduler is initialized
     */
    @PostConstruct
    public void logScheduleInfo() {
        long minutes = fixedDelay / 60000;
        long seconds = (fixedDelay % 60000) / 1000;
        if (seconds > 0) {
            log.info("MetricsUpdateJob scheduler initialized - Runs every {} minutes {} seconds after previous completion",
                     minutes, seconds);
        } else {
            log.info("MetricsUpdateJob scheduler initialized - Runs every {} minutes after previous completion", minutes);
        }
    }

    /**
     * Scheduled execution of MetricsUpdateJob
     * Runs every 5 minutes (fixed delay, configured via batch.metrics-update.fixed-delay)
     */
    @Scheduled(fixedDelayString = "${batch.metrics-update.fixed-delay}")
    public void runPeriodicMetricsUpdate() {
        log.debug("Starting periodic metrics update");

        try {
            runMetricsUpdateJob("SCHEDULED");
        } catch (Exception e) {
            log.error("Failed to run periodic metrics update", e);
        }
    }

    /**
     * Manually run MetricsUpdateJob
     */
    public JobExecution runMetricsUpdateJob(String trigger) throws Exception {
        log.debug("Launching MetricsUpdateJob (triggered by: {})", trigger);

        JobParametersBuilder parametersBuilder = new JobParametersBuilder()
                .addString("trigger", trigger)
                .addString("runId", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")));

        JobExecution execution = jobLauncher.run(metricsUpdateJob, parametersBuilder.toJobParameters());

        log.debug("Job launched with execution ID: {}", execution.getId());
        return execution;
    }
}
