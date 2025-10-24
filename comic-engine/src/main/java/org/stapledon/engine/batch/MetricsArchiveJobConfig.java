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
import org.stapledon.metrics.service.MetricsArchiveService;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Batch configuration for metrics archive job.
 * Archives yesterday's metrics to JSON for historical analysis.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MetricsArchiveJobConfig {

    private final MetricsArchiveService metricsArchiveService;

    /**
     * Job for archiving metrics
     */
    @Bean
    public Job metricsArchiveJob(
            JobRepository jobRepository,
            @Qualifier("metricsArchiveStep") Step metricsArchiveStep,
            JsonBatchExecutionTracker jsonBatchExecutionTracker) {

        return new JobBuilder("MetricsArchiveJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jsonBatchExecutionTracker)
                .start(metricsArchiveStep)
                .build();
    }

    /**
     * Step for performing metrics archiving
     */
    @Bean
    public Step metricsArchiveStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {

        return new StepBuilder("metricsArchiveStep", jobRepository)
                .tasklet(metricsArchiveTasklet(), transactionManager)
                .build();
    }

    /**
     * Tasklet that performs the actual metrics archiving
     */
    @Bean
    public Tasklet metricsArchiveTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting metrics archiving");

            long startTime = System.currentTimeMillis();
            LocalDate yesterday = LocalDate.now().minusDays(1);
            boolean success = metricsArchiveService.archiveMetricsForDate(yesterday);
            long duration = System.currentTimeMillis() - startTime;

            if (success) {
                log.info("Metrics archiving completed successfully in {}ms", duration);
            } else {
                log.error("Metrics archiving failed");
                throw new IllegalStateException("Metrics archiving failed");
            }

            return RepeatStatus.FINISHED;
        };
    }
}
