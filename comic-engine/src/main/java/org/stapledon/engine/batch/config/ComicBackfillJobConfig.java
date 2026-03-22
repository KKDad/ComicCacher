package org.stapledon.engine.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.engine.batch.ComicBackfillService;
import org.stapledon.engine.batch.ComicBackfillService.BackfillTask;
import org.stapledon.engine.batch.ComicBackfillService.DateBackfillTask;
import org.stapledon.engine.batch.ComicBackfillService.StripBackfillTask;
import org.stapledon.engine.batch.JsonBatchExecutionTracker;
import org.stapledon.engine.batch.scheduler.DailyJobScheduler;
import org.stapledon.engine.management.ManagementFacade;

/**
 * Spring Batch configuration for comic backfill job. Gradually backfills missing comic strips for a configurable target year.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.comic-backfill.enabled", havingValue = "true", matchIfMissing = true)
public class ComicBackfillJobConfig {

    private final ManagementFacade managementFacade;
    private final ComicBackfillService backfillService;

    @Value("${batch.comic-backfill.chunk-size:10}")
    private int chunkSize;

    @Value("${batch.comic-backfill.delay-between-comics-ms:2000}")
    private long delayBetweenComics;

    @Value("${batch.comic-backfill.cron}")
    private String cronExpression;

    @Value("${batch.timezone:America/Toronto}")
    private String timezone;

    /**
     * Scheduler for ComicBackfillJob - runs daily at configured cron time. Triggered by SchedulerTriggers component.
     */
    @Bean
    public DailyJobScheduler comicBackfillJobScheduler(@Qualifier("comicBackfillJob") Job comicBackfillJob, JobOperator jobOperator, JsonBatchExecutionTracker tracker) {
        return new DailyJobScheduler(comicBackfillJob, cronExpression, timezone, jobOperator, tracker, "Backfills missing comic strips for gaps in the archive");
    }

    /**
     * Main job for comic backfill
     */
    @Bean
    @Qualifier("comicBackfillJob")
    public Job comicBackfillJob(JobRepository jobRepository, @Qualifier("comicBackfillStep") Step comicBackfillStep, JsonBatchExecutionTracker jsonBatchExecutionTracker) {

        return new JobBuilder("ComicBackfillJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jsonBatchExecutionTracker)
                .start(comicBackfillStep).build();
    }

    /**
     * Step for processing comic backfill tasks
     */
    @Bean
    @Qualifier("comicBackfillStep")
    public Step comicBackfillStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, @Qualifier("backfillTaskReader") ItemReader<BackfillTask> backfillTaskReader,
            @Qualifier("backfillTaskProcessor") ItemProcessor<BackfillTask, ComicDownloadResult> backfillTaskProcessor,
            @Qualifier("backfillTaskWriter") ItemWriter<ComicDownloadResult> backfillTaskWriter) {

        return new StepBuilder("comicBackfillStep", jobRepository).<BackfillTask, ComicDownloadResult>chunk(chunkSize).transactionManager(transactionManager).reader(backfillTaskReader)
                .processor(backfillTaskProcessor).writer(backfillTaskWriter).build();
    }

    /**
     * Reader that provides the list of backfill tasks (comic + date pairs). Uses @StepScope so findMissingStrips() is called when the job runs, not at application startup.
     */
    @Bean
    @StepScope
    @Qualifier("backfillTaskReader")
    public ItemReader<BackfillTask> backfillTaskReader() {
        log.debug("Building backfill task list for job execution");
        List<BackfillTask> tasks = backfillService.findMissingStrips();
        if (tasks.isEmpty()) {
            log.info("No missing strips found - backfill has nothing to process");
        } else {
            log.info("Backfill reader prepared {} tasks (chunk size: {}, delay: {}ms)", tasks.size(), chunkSize, delayBetweenComics);
        }
        return new ListItemReader<>(tasks);
    }

    /**
     * Processor that downloads a comic for a specific date. Uses downloadComicForDate for efficient single-comic downloads - the comic has already been validated and filtered by
     * ComicBackfillService.
     */
    @Bean
    @Qualifier("backfillTaskProcessor")
    public ItemProcessor<BackfillTask, ComicDownloadResult> backfillTaskProcessor() {
        return task -> {
            try {
                // Add a small delay between comics to avoid overwhelming sources
                if (delayBetweenComics > 0) {
                    Thread.sleep(delayBetweenComics);
                }

                if (task instanceof DateBackfillTask dateTask) {
                    log.info("Backfilling {} for date: {}", dateTask.comic().getName(), dateTask.date());
                    return managementFacade.downloadComicForDate(dateTask.comic(), dateTask.date()).orElse(null);
                } else if (task instanceof StripBackfillTask stripTask) {
                    log.info("Backfilling {} for strip #{}", stripTask.comic().getName(), stripTask.stripNumber());
                    return managementFacade.downloadComicByStripNumber(
                            stripTask.comic(), stripTask.stripNumber()).orElse(null);
                } else {
                    log.error("Unknown backfill task type: {}", task.getClass().getName());
                    return null;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Backfill interrupted for {}: {}", task.comic().getName(), e.getMessage());
                return null;
            } catch (Exception e) {
                log.error("Error backfilling {}: {}", task.comic().getName(), e.getMessage(), e);
                return null;
            }
        };
    }

    /**
     * Writer that logs the backfill results
     */
    @Bean
    @Qualifier("backfillTaskWriter")
    public ItemWriter<ComicDownloadResult> backfillTaskWriter() {
        return chunk -> {
            int successCount = 0;
            int failureCount = 0;

            for (ComicDownloadResult result : chunk.getItems()) {
                if (result == null) {
                    failureCount++;
                    continue;
                }

                if (result.isSuccessful()) {
                    successCount++;
                    log.info("Successfully backfilled: {} for {}", result.getRequest().getComicName(), result.getRequest().getDate());
                } else {
                    failureCount++;
                    log.warn("Failed to backfill: {} for {} - {}", result.getRequest().getComicName(), result.getRequest().getDate(), result.getErrorMessage());
                }
            }

            log.info("Backfill chunk complete: {} successful, {} failed", successCount, failureCount);
        };
    }
}
