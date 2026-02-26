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

import org.stapledon.engine.batch.JsonBatchExecutionTracker;
import org.stapledon.engine.batch.scheduler.DailyJobScheduler;
import org.stapledon.engine.management.ManagementFacade;

/**
 * Spring Batch configuration for avatar backfill job.
 * Downloads missing avatar images for all comics that have a source configured.
 *
 * <p>Disabled by default — enable with {@code batch.avatar-backfill.enabled=true}
 * for a one-time run, then disable again.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.avatar-backfill.enabled", havingValue = "true", matchIfMissing = false)
public class AvatarBackfillJobConfig {

    private final ManagementFacade managementFacade;

    @Value("${batch.avatar-backfill.cron}")
    private String cronExpression;

    @Value("${batch.timezone:America/Toronto}")
    private String timezone;

    @Value("${batch.avatar-backfill.delay-between-downloads-ms:2000}")
    private long delayBetweenDownloads;

    /**
     * Scheduler for AvatarBackfillJob - runs at configured cron time.
     */
    @Bean
    public DailyJobScheduler avatarBackfillJobScheduler(
            @Qualifier("avatarBackfillJob") Job avatarBackfillJob,
            JobOperator jobOperator,
            JsonBatchExecutionTracker tracker) {
        return new DailyJobScheduler(avatarBackfillJob, cronExpression, timezone, jobOperator, tracker);
    }

    /**
     * Job for backfilling missing avatar images.
     */
    @Bean
    public Job avatarBackfillJob(
            JobRepository jobRepository,
            @Qualifier("avatarBackfillStep") Step avatarBackfillStep,
            JsonBatchExecutionTracker jsonBatchExecutionTracker) {

        return new JobBuilder("AvatarBackfillJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jsonBatchExecutionTracker)
                .start(avatarBackfillStep)
                .build();
    }

    /**
     * Step for performing avatar backfill.
     */
    @Bean
    public Step avatarBackfillStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {

        return new StepBuilder("avatarBackfillStep", jobRepository)
                .tasklet(avatarBackfillTasklet(), transactionManager)
                .build();
    }

    /**
     * Tasklet that delegates to ManagementFacade.downloadMissingAvatars().
     */
    @Bean
    public Tasklet avatarBackfillTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Starting avatar backfill job");

            long startTime = System.currentTimeMillis();
            int downloaded = managementFacade.downloadMissingAvatars();
            long duration = System.currentTimeMillis() - startTime;

            log.info("Avatar backfill job complete: {} avatars downloaded in {}ms", downloaded, duration);
            return RepeatStatus.FINISHED;
        };
    }
}
