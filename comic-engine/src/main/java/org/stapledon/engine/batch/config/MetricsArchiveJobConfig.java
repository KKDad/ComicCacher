package org.stapledon.engine.batch.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stapledon.engine.batch.JsonBatchExecutionTracker;
import org.stapledon.engine.batch.scheduler.DailyJobScheduler;
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
@ConditionalOnProperty(name = "batch.metrics-archive.enabled", havingValue = "true", matchIfMissing = true)
public class MetricsArchiveJobConfig {

    private final MetricsArchiveService metricsArchiveService;

    @Value("${batch.metrics-archive.cron}")
    private String cronExpression;

    @Value("${batch.timezone:America/Toronto}")
    private String timezone;

    /**
     * Scheduler for MetricsArchiveJob - runs daily at configured cron time.
     * Triggered by SchedulerTriggers component.
     */
    @Bean
    public DailyJobScheduler metricsArchiveJobScheduler(
            JobOperator jobOperator,
            JsonBatchExecutionTracker tracker) {
        return new DailyJobScheduler(
                "MetricsArchiveJob", cronExpression, timezone, jobOperator, tracker);
    }

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
