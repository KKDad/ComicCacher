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
import org.stapledon.engine.management.ManagementFacade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Batch configuration for comic reconciliation job.
 * Reconciles comic configuration with bootstrap data sources.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ComicReconciliationJobConfig {

    private final ManagementFacade comicManagementFacade;

    /**
     * Job for reconciling comic configuration
     */
    @Bean
    public Job comicReconciliationJob(
            JobRepository jobRepository,
            @Qualifier("reconciliationStep") Step reconciliationStep,
            JsonBatchExecutionTracker jsonBatchExecutionTracker) {

        return new JobBuilder("ComicReconciliationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jsonBatchExecutionTracker)
                .start(reconciliationStep)
                .build();
    }

    /**
     * Step for performing reconciliation
     */
    @Bean
    public Step reconciliationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {

        return new StepBuilder("reconciliationStep", jobRepository)
                .tasklet(reconciliationTasklet(), transactionManager)
                .build();
    }

    /**
     * Tasklet that performs the actual reconciliation
     */
    @Bean
    public Tasklet reconciliationTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting comic configuration reconciliation");

            long startTime = System.currentTimeMillis();
            boolean success = comicManagementFacade.reconcileWithBootstrap();
            long duration = System.currentTimeMillis() - startTime;

            if (success) {
                log.info("Comic reconciliation completed successfully in {}ms", duration);
            } else {
                log.error("Comic reconciliation failed");
                throw new IllegalStateException("Reconciliation failed");
            }

            return RepeatStatus.FINISHED;
        };
    }
}
