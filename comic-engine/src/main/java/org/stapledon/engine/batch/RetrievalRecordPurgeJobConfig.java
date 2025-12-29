package org.stapledon.engine.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
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
public class RetrievalRecordPurgeJobConfig {

    private final ManagementFacade comicManagementFacade;

    @Value("${batch.record-purge.days-to-keep:30}")
    private int daysToKeep;

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
