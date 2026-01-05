package org.stapledon.engine.batch;

import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.job.JobExecution;

import java.time.Duration;
import java.util.Objects;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Comprehensive job execution listener that provides detailed logging
 * and metrics for comic retrieval batch jobs.
 */
@Slf4j
@ToString
public class LoggingJobExecutionListener implements JobExecutionListener {

    public static final String MSG_SEPARATOR = "==========================================";

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(MSG_SEPARATOR);
        log.info("Starting Comic Retrieval Job ID: {}, Job: {}", jobExecution.getId(), jobExecution.getJobInstance().getJobName());
        log.info("Job Parameters: {}", jobExecution.getJobParameters());
        log.info(MSG_SEPARATOR);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Objects.requireNonNull(jobExecution.getStartTime());
        Duration duration = Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime());

        log.info(MSG_SEPARATOR);
        log.info("Finished Comic Retrieval Job ID: {}, Job: {}", jobExecution.getId(), jobExecution.getJobInstance().getJobName());
        log.info("--> Status: {}", jobExecution.getStatus());
        log.info("--> Exit Code: {}", jobExecution.getExitStatus().getExitCode());
        log.info("--> Exit Description: {}", jobExecution.getExitStatus().getExitDescription());
        log.info("--> Duration: {} seconds", duration.getSeconds());

        // Log any failures
        if (!jobExecution.getAllFailureExceptions().isEmpty()) {
            log.error("Job had {} failures:", jobExecution.getAllFailureExceptions().size());
            jobExecution.getAllFailureExceptions().forEach(throwable -> log.error("Failure: {}", throwable.getMessage(), throwable));
        }
        log.info(MSG_SEPARATOR);
    }
}
