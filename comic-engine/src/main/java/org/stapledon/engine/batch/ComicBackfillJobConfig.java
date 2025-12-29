package org.stapledon.engine.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.engine.batch.ComicBackfillService.BackfillTask;
import org.stapledon.engine.management.ManagementFacade;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Batch configuration for comic backfill job.
 * Gradually backfills missing comic strips for a configurable target year.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ComicBackfillJobConfig {

    private final ManagementFacade managementFacade;
    private final ComicBackfillService backfillService;

    @Value("${batch.comic-backfill.chunk-size:10}")
    private int chunkSize;

    @Value("${batch.comic-backfill.delay-between-comics-ms:2000}")
    private long delayBetweenComics;

    /**
     * Main job for comic backfill
     */
    @Bean
    @Qualifier("comicBackfillJob")
    public Job comicBackfillJob(
            JobRepository jobRepository,
            @Qualifier("comicBackfillStep") Step comicBackfillStep,
            JsonBatchExecutionTracker jsonBatchExecutionTracker) {

        return new JobBuilder("ComicBackfillJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jsonBatchExecutionTracker)
                .listener(new ComicJobExecutionListener())
                .start(comicBackfillStep)
                .build();
    }

    /**
     * Step for processing comic backfill tasks
     */
    @Bean
    @Qualifier("comicBackfillStep")
    public Step comicBackfillStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("backfillTaskReader") ItemReader<BackfillTask> backfillTaskReader,
            @Qualifier("backfillTaskProcessor") ItemProcessor<BackfillTask, ComicDownloadResult> backfillTaskProcessor,
            @Qualifier("backfillTaskWriter") ItemWriter<ComicDownloadResult> backfillTaskWriter) {

        return new StepBuilder("comicBackfillStep", jobRepository)
                .<BackfillTask, ComicDownloadResult>chunk(chunkSize, transactionManager)
                .reader(backfillTaskReader)
                .processor(backfillTaskProcessor)
                .writer(backfillTaskWriter)
                .build();
    }

    /**
     * Reader that provides the list of backfill tasks (comic + date pairs)
     */
    @Bean
    @Qualifier("backfillTaskReader")
    public ItemReader<BackfillTask> backfillTaskReader() {
        return new ListItemReader<>(backfillService.findMissingStrips());
    }

    /**
     * Processor that downloads a comic for a specific date
     */
    @Bean
    @Qualifier("backfillTaskProcessor")
    public ItemProcessor<BackfillTask, ComicDownloadResult> backfillTaskProcessor() {
        return task -> {
            log.debug("Backfilling {} for date: {}", task.comic().getName(), task.date());

            try {
                // Add a small delay between comics to avoid overwhelming sources
                if (delayBetweenComics > 0) {
                    Thread.sleep(delayBetweenComics);
                }

                // Use the existing updateComicsForDate method but for a specific date
                List<ComicDownloadResult> results = managementFacade.updateComicsForDate(task.date());

                // Find the result for this specific comic
                return results.stream()
                        .filter(r -> r.getRequest().getComicName().equals(task.comic().getName()))
                        .findFirst()
                        .orElse(null);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Backfill interrupted for {}: {}", task.comic().getName(), e.getMessage());
                return null;
            } catch (Exception e) {
                log.error("Error backfilling {} for {}: {}",
                        task.comic().getName(), task.date(), e.getMessage());
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
                    log.debug("Successfully backfilled: {} for {}",
                            result.getRequest().getComicName(),
                            result.getRequest().getDate());
                } else {
                    failureCount++;
                    log.warn("Failed to backfill: {} for {} - {}",
                            result.getRequest().getComicName(),
                            result.getRequest().getDate(),
                            result.getErrorMessage());
                }
            }

            log.info("Backfill chunk complete: {} successful, {} failed", successCount, failureCount);
        };
    }
}
