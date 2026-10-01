package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class ManualJobLauncherTest {

    private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    @AfterEach
    void shutDown() {
        executor.shutdown();
    }

    @Test
    void returnsTheExecutionWhileTheJobRunsAndCallsBackWhenItEnds() throws Exception {
        executor.setThreadNamePrefix("manual-job-test-");
        executor.initialize();
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch ended = new CountDownLatch(1);
        Job job = execution -> {
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            execution.setStatus(BatchStatus.COMPLETED);
        };

        JobExecution execution = new ManualJobLauncher(new ResourcelessJobRepository(), executor)
                .start(job, new JobParametersBuilder().addString("runId", "1").toJobParameters(), ended::countDown);

        assertThat(execution.getId()).isNotNull();
        assertThat(ended.getCount()).isEqualTo(1);
        release.countDown();
        assertThat(ended.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void callsBackWhenTheJobCannotBeQueued() throws Exception {
        CountDownLatch ended = new CountDownLatch(1);
        ManualJobLauncher launcher = new ManualJobLauncher(new ResourcelessJobRepository(), task -> {
            throw new TaskRejectedException("full");
        });

        // The operator catches the rejection and returns the execution marked FAILED
        JobExecution execution = launcher.start(job -> { }, new JobParametersBuilder().addString("runId", "2").toJobParameters(), ended::countDown);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(ended.getCount()).isZero();
    }
}
