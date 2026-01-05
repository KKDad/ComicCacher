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
import org.stapledon.engine.management.ManagementFacade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Batch configuration for retrieval record purge job.
 * Purges old retrieval records to prevent unbounded growth.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.record-purge.enabled", havingValue = "true", matchIfMissing = true)
public class RetrievalRecordPurgeJobConfig {

    private final ManagementFacade comicManagementFacade;

    @Value("${batch.record-purge.days-to-keep:30}")
    private int daysToKeep;

    @Value("${batch.record-purge.cron}")
    private String cronExpression;

    @Value("${batch.timezone:America/Toronto}")
    private String timezone;

    /**
     * Scheduler for RetrievalRecordPurgeJob - runs daily at configured cron time.
     * Triggered by SchedulerTriggers component.
     */
    @Bean
    public DailyJobScheduler retrievalRecordPurgeJobScheduler(
            JobOperator jobOperator,
            JsonBatchExecutionTracker tracker) {
        return new DailyJobScheduler(
                "RetrievalRecordPurgeJob", cronExpression, timezone, jobOperator, tracker);
    }

    /**
     * Job for purging old retrieval records
     */
    @Bean
    public Job retrievalRecordPurgeJob(
            JobRepository jobRepository,
            @Qualifier("recordPurgeStep") Step recordPurgeStep,
            JsonBatchExecutionTracker jsonBatchExecutionTracker) {

        return new JobBuilder("RetrievalRecordPurgeJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jsonBatchExecutionTracker)
                .start(recordPurgeStep)
                .build();
    }

    /**
     * Step for performing record purge
     */
    @Bean
    public Step recordPurgeStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {

        return new StepBuilder("recordPurgeStep", jobRepository)
                .tasklet(recordPurgeTasklet(), transactionManager)
                .build();
    }

    /**
     * Tasklet that performs the actual record purge
     */
    @Bean
    public Tasklet recordPurgeTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting retrieval record purge (keeping last {} days)", daysToKeep);

            long startTime = System.currentTimeMillis();
            int purgedCount = comicManagementFacade.purgeOldRetrievalRecords(daysToKeep);
            long duration = System.currentTimeMillis() - startTime;

            log.info("Retrieval record purge completed in {}ms. Purged {} records.", duration, purgedCount);

            return RepeatStatus.FINISHED;
        };
    }
}
