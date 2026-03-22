package org.stapledon.engine.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

import org.stapledon.engine.batch.JsonBatchExecutionTracker;
import org.stapledon.engine.batch.logging.BatchJobLogService;
import org.stapledon.engine.batch.scheduler.DailyJobScheduler;
import org.stapledon.engine.management.ManagementFacade;

/**
 * Spring Batch configuration for retrieval record purge job. Purges old retrieval records and batch log files to prevent unbounded growth.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.record-purge.enabled", havingValue = "true", matchIfMissing = true)
public class RetrievalRecordPurgeJobConfig {

    private final BatchJobLogService batchJobLogService;
    private final ManagementFacade comicManagementFacade;

    @Value("${batch.record-purge.days-to-keep:30}")
    private int daysToKeep;

    @Value("${batch.record-purge.cron}")
    private String cronExpression;

    @Value("${batch.timezone:America/Toronto}")
    private String timezone;

    /**
     * Scheduler for RetrievalRecordPurgeJob - runs daily at configured cron time. Triggered by SchedulerTriggers component.
     */
    @Bean
    public DailyJobScheduler retrievalRecordPurgeJobScheduler(@Qualifier("retrievalRecordPurgeJob") Job retrievalRecordPurgeJob, JobOperator jobOperator, JsonBatchExecutionTracker tracker) {
        return new DailyJobScheduler(retrievalRecordPurgeJob, cronExpression, timezone, jobOperator, tracker, "Purges old retrieval records and batch log files beyond the retention window");
    }

    /**
     * Job for purging old retrieval records and batch log files
     */
    @Bean
    public Job retrievalRecordPurgeJob(JobRepository jobRepository, @Qualifier("recordPurgeStep") Step recordPurgeStep, @Qualifier("logPurgeStep") Step logPurgeStep,
                                       JsonBatchExecutionTracker jsonBatchExecutionTracker) {

        return new JobBuilder("RetrievalRecordPurgeJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jsonBatchExecutionTracker)
                .start(recordPurgeStep)
                .next(logPurgeStep)
                .build();
    }

    /**
     * Step for performing record purge
     */
    @Bean
    public Step recordPurgeStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {

        return new StepBuilder("recordPurgeStep", jobRepository).tasklet(recordPurgeTasklet(), transactionManager).build();
    }

    /**
     * Tasklet that performs the actual record purge
     */
    @Bean
    public Tasklet recordPurgeTasklet() {
        return (contribution, chunkContext) -> {
            LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);
            log.info("Starting retrieval record purge (keeping last {} days, cutoff date: {})", daysToKeep, cutoffDate);

            long startTime = System.currentTimeMillis();
            int purgedCount = comicManagementFacade.purgeOldRetrievalRecords(daysToKeep);
            long duration = System.currentTimeMillis() - startTime;

            if (purgedCount > 0) {
                log.info("Retrieval record purge completed in {}ms: purged {} records older than {}", duration, purgedCount, cutoffDate);
            } else {
                log.info("Retrieval record purge completed in {}ms: no records older than {} to purge", duration, cutoffDate);
            }

            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Step for purging old batch log files
     */
    @Bean
    public Step logPurgeStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {

        return new StepBuilder("logPurgeStep", jobRepository).tasklet(logPurgeTasklet(), transactionManager).build();
    }

    /**
     * Tasklet that purges old batch log files
     */
    @Bean
    public Tasklet logPurgeTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting batch log file purge (keeping last {} days)", daysToKeep);

            long startTime = System.currentTimeMillis();
            int purgedCount = batchJobLogService.purgeOldLogFiles(daysToKeep);
            long duration = System.currentTimeMillis() - startTime;

            if (purgedCount > 0) {
                log.info("Batch log file purge completed in {}ms: deleted {} log files", duration, purgedCount);
            } else {
                log.info("Batch log file purge completed in {}ms: no old log files to delete", duration);
            }

            return RepeatStatus.FINISHED;
        };
    }
}
