package org.stapledon.engine.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
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

import java.util.List;

import org.stapledon.engine.batch.JsonBatchExecutionTracker;
import org.stapledon.engine.batch.scheduler.CatchUpWeight;
import org.stapledon.engine.batch.scheduler.DailyJobScheduler;
import org.stapledon.engine.batch.scheduler.JobParameterDefinition;
import org.stapledon.engine.promotion.DevPromotionService;
import org.stapledon.engine.promotion.DevPromotionService.PromotionResult;
import org.stapledon.engine.source.SourceRegistry;

/**
 * Spring Batch configuration for the promote-from-dev job: copies the strips the dev instance downloaded in the last {@code days} days (1 by
 * default) that this instance is missing, so prod doesn't download them a second time. Runs after dev's download and before prod's. Only prod
 * sets {@code comics.promotion.source-url}; everywhere else scheduled and startup makeup runs are skipped and a manual one does nothing. The job stays registered
 * there rather than being switched off, since {@code SchedulerHealthCheck} reports a known job without a scheduler as down.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.promote-from-dev.enabled", havingValue = "true", matchIfMissing = true)
public class PromoteFromDevJobConfig {

    public static final String JOB_NAME = "PromoteFromDevJob";

    private final DevPromotionService promotionService;
    private final SourceRegistry sourceRegistry;

    @Value("${batch.promote-from-dev.cron}")
    private String cronExpression;

    @Value("${batch.timezone:America/Toronto}")
    private String timezone;

    @Value("${batch.promote-from-dev.days:1}")
    private int defaultDays;

    /**
     * Scheduler for PromoteFromDevJob: runs daily at the configured cron time, skipping runs when promotion isn't configured. Triggered by
     * SchedulerTriggers component. Its startup makeup run goes before ComicDownloadJob's, so a restart that missed both copies dev's strips
     * before the download fetches them from the sources.
     */
    @Bean
    @CatchUpWeight(-10)
    public DailyJobScheduler promoteFromDevJobScheduler(@Qualifier("promoteFromDevJob") Job promoteFromDevJob, JobOperator jobOperator,
            JsonBatchExecutionTracker tracker) {
        List<JobParameterDefinition> parameters = List.of(
                new JobParameterDefinition("days", "Days back, including today", "INTEGER", false, String.valueOf(defaultDays), null),
                new JobParameterDefinition("source", "Source Filter", "ENUM", false, "ALL", sourceRegistry.jobSourceOptions()),
                new JobParameterDefinition("comic", "Comic ID (blank for all)", "INTEGER", false, null, null));
        DailyJobScheduler scheduler = new DailyJobScheduler(promoteFromDevJob, cronExpression, timezone, jobOperator, tracker,
                "Copies the strips dev already downloaded that this instance is missing", parameters);
        scheduler.setPrecondition(promotionService::isConfigured, "promotion isn't configured (comics.promotion.source-url and token)");
        return scheduler;
    }

    /**
     * The promote-from-dev job.
     */
    @Bean
    public Job promoteFromDevJob(JobRepository jobRepository, @Qualifier("promoteFromDevStep") Step promoteFromDevStep,
            JsonBatchExecutionTracker jsonBatchExecutionTracker) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(jsonBatchExecutionTracker)
                .start(promoteFromDevStep)
                .build();
    }

    /**
     * The single step: read dev's manifest and copy the missing strips.
     */
    @Bean
    public Step promoteFromDevStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            @Qualifier("promoteFromDevTasklet") Tasklet promoteFromDevTasklet) {
        return new StepBuilder("promoteFromDevStep", jobRepository)
                .tasklet(promoteFromDevTasklet, transactionManager)
                .build();
    }

    /**
     * Promotes the last "days" days (default {@code batch.promote-from-dev.days}), optionally only one "source" or one "comic". Fails when
     * "days" is out of range, when dev can't be read, or when any strip failed, so the batch history shows it.
     */
    @Bean
    @StepScope
    public Tasklet promoteFromDevTasklet(@Value("#{jobParameters['days']}") String days, @Value("#{jobParameters['source']}") String source,
            @Value("#{jobParameters['comic']}") String comic) {
        return (contribution, chunkContext) -> {
            int dayCount = days == null || days.isBlank() ? defaultDays : Integer.parseInt(days.strip());
            Integer comicId = comic == null || comic.isBlank() ? null : Integer.valueOf(comic.strip());
            PromotionResult result = promotionService.promote(dayCount, source, comicId);
            if (result.failed() > 0) {
                throw new IllegalStateException(result.failed() + " strip(s) couldn't be promoted; see the WARN and ERROR lines above");
            }
            return RepeatStatus.FINISHED;
        };
    }
}
