package org.stapledon.engine.batch;

import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;

import java.time.Duration;
import java.time.LocalDateTime;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Comprehensive job execution listener that provides detailed logging
 * and metrics for comic retrieval batch jobs.
 */
@Slf4j
@ToString
public class ComicJobExecutionListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("==========================================");
        log.info("Starting Comic Retrieval Job: {}", jobExecution.getJobInstance().getJobName());
        log.info("Job ID: {}", jobExecution.getId());
        log.info("Job Parameters: {}", jobExecution.getJobParameters());
        log.info("Start Time: {}", jobExecution.getStartTime());
        log.info("==========================================");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Duration duration = Duration.between(
                jobExecution.getStartTime(),
                jobExecution.getEndTime());

        log.info("==========================================");
        log.info("Comic Retrieval Job Completed: {}", jobExecution.getJobInstance().getJobName());
        log.info("Job ID: {}", jobExecution.getId());
        log.info("Status: {}", jobExecution.getStatus());
        log.info("Exit Code: {}", jobExecution.getExitStatus().getExitCode());
        log.info("Exit Description: {}", jobExecution.getExitStatus().getExitDescription());
        log.info("Duration: {} seconds", duration.getSeconds());
        log.info("Start Time: {}", jobExecution.getStartTime());
        log.info("End Time: {}", jobExecution.getEndTime());

        // Log step execution details
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            logStepDetails(stepExecution);
        }

        // Log any failures
        if (!jobExecution.getAllFailureExceptions().isEmpty()) {
            log.error("Job had {} failures:", jobExecution.getAllFailureExceptions().size());
            jobExecution.getAllFailureExceptions()
                    .forEach(throwable -> log.error("Failure: {}", throwable.getMessage(), throwable));
        }

        log.info("==========================================");
    }

    private void logStepDetails(StepExecution stepExecution) {
        Duration stepDuration = Duration.between(
                stepExecution.getStartTime(),
                stepExecution.getEndTime() != null ? stepExecution.getEndTime() : LocalDateTime.now());

        log.info("--- Step Details ---");
        log.info("Step Name: {}", stepExecution.getStepName());
        log.info("Step Status: {}", stepExecution.getStatus());
        log.info("Step Duration: {} seconds", stepDuration.getSeconds());
        log.info("Read Count: {}", stepExecution.getReadCount());
        log.info("Write Count: {}", stepExecution.getWriteCount());
        log.info("Commit Count: {}", stepExecution.getCommitCount());
        log.info("Skip Count: {}", stepExecution.getSkipCount());
        log.info("Process Skip Count: {}", stepExecution.getProcessSkipCount());
        log.info("Read Skip Count: {}", stepExecution.getReadSkipCount());
        log.info("Write Skip Count: {}", stepExecution.getWriteSkipCount());
        log.info("Rollback Count: {}", stepExecution.getRollbackCount());

        if (stepExecution.getReadSkipCount() > 0
                || stepExecution.getProcessSkipCount() > 0
                || stepExecution.getWriteSkipCount() > 0) {
            log.warn("Step had skipped items - check for comic retrieval failures");
        }

        if (!stepExecution.getFailureExceptions().isEmpty()) {
            log.error("Step had {} failures:", stepExecution.getFailureExceptions().size());
            stepExecution.getFailureExceptions()
                    .forEach(throwable -> log.error("Step failure: {}", throwable.getMessage(), throwable));
        }
    }
}