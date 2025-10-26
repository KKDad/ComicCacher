package org.stapledon.engine.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stapledon.metrics.service.MetricsUpdateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Batch configuration for metrics update job.
 * Periodically persists access metrics and rebuilds combined metrics.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MetricsUpdateJobConfig {

    private final MetricsUpdateService metricsUpdateService;

    /**
     * Job for updating metrics
     */
    @Bean
    public Job metricsUpdateJob(
            JobRepository jobRepository,
            @Qualifier("metricsUpdateStep") Step metricsUpdateStep,
            JsonBatchExecutionTracker jsonBatchExecutionTracker) {

        return new JobBuilder("MetricsUpdateJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jsonBatchExecutionTracker)
                .start(metricsUpdateStep)
                .build();
    }

    /**
     * Step for performing metrics update
     */
    @Bean
    public Step metricsUpdateStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {

        return new StepBuilder("metricsUpdateStep", jobRepository)
                .tasklet(metricsUpdateTasklet(), transactionManager)
                .build();
    }

    /**
     * Tasklet that performs the actual metrics update
     */
    @Bean
    public Tasklet metricsUpdateTasklet() {
        return (contribution, chunkContext) -> {
            log.debug("Starting metrics update");

            long startTime = System.currentTimeMillis();
            metricsUpdateService.updateMetrics();
            long duration = System.currentTimeMillis() - startTime;

            log.info("Metrics update completed in {}ms", duration);

            return RepeatStatus.FINISHED;
        };
    }
}
