package org.stapledon.engine.batch.config;

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
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.engine.batch.JsonBatchExecutionTracker;
import org.stapledon.engine.batch.LoggingJobExecutionListener;
import org.stapledon.engine.batch.scheduler.DailyJobScheduler;
import org.stapledon.engine.management.ManagementFacade;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Batch configuration for comic retrieval jobs.
 * Provides comprehensive execution tracking, retry logic, and monitoring.
 */
@Slf4j
@ToString
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.comic-download.enabled", havingValue = "true", matchIfMissing = true)
public class ComicRetrievalJobConfig {

    private final ManagementFacade managementFacade;

    @Value("${batch.comic-download.cron}")
    private String cronExpression;

    @Value("${batch.timezone:America/Toronto}")
    private String timezone;

    /**
     * Scheduler for ComicDownloadJob - runs daily at configured cron time.
     * Triggered by SchedulerTriggers component.
     */
    @Bean
    public DailyJobScheduler comicDownloadJobScheduler(
            JobOperator jobOperator,
            JsonBatchExecutionTracker tracker) {
        return new DailyJobScheduler(
                "ComicDownloadJob", cronExpression, timezone, jobOperator, tracker);
    }

    /**
     * Main job for daily comic retrieval
     */
    @Bean
    @Primary
    public Job comicDownloadJob(
            JobRepository jobRepository,
            @Qualifier("comicRetrievalStep") Step comicRetrievalStep,
            JsonBatchExecutionTracker jsonBatchExecutionTracker) {

        return new JobBuilder("ComicDownloadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jsonBatchExecutionTracker)
                .listener(new LoggingJobExecutionListener())
                .start(comicRetrievalStep)
                .build();
    }

    /**
     * Step for processing comic retrieval
     */
    @Bean
    public Step comicRetrievalStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<LocalDate> dateReader,
            ItemProcessor<LocalDate, List<ComicDownloadResult>> comicProcessor,
            ItemWriter<List<ComicDownloadResult>> comicResultWriter) {

        return new StepBuilder("comicRetrievalStep", jobRepository)
                .<LocalDate, List<ComicDownloadResult>>chunk(1)
                .transactionManager(transactionManager)
                .reader(dateReader)
                .processor(comicProcessor)
                .writer(comicResultWriter)
                .build();
    }

    /**
     * Reader that provides the target date for comic downloads
     */
    @Bean
    public ItemReader<LocalDate> dateReader() {
        return new ListItemReader<>(List.of(LocalDate.now()));
    }

    /**
     * Processor that handles downloading and saving comics for a date
     */
    @Bean
    public ItemProcessor<LocalDate, List<ComicDownloadResult>> comicProcessor() {
        return date -> {
            log.info("Processing comics for date: {}", date);
            long startTime = System.currentTimeMillis();

            // Management facade handles download + save + metadata update
            List<ComicDownloadResult> results = managementFacade.updateComicsForDate(date);

            long duration = System.currentTimeMillis() - startTime;
            long successCount = results.stream().filter(ComicDownloadResult::isSuccessful).count();
            long failureCount = results.size() - successCount;

            log.info("Completed processing {} comics in {}ms: {} successful, {} failed",
                    results.size(), duration, successCount, failureCount);

            return results;
        };
    }

    /**
     * Writer that handles the results (logging summary)
     */
    @Bean
    public ItemWriter<List<ComicDownloadResult>> comicResultWriter() {
        return chunk -> {
            for (List<ComicDownloadResult> results : chunk.getItems()) {
                for (ComicDownloadResult result : results) {
                    if (result.isSuccessful()) {
                        log.debug("Successfully processed: {}", result.getRequest().getComicName());
                    } else {
                        log.error("Failed to process: {} - {}",
                                result.getRequest().getComicName(), result.getErrorMessage());
                    }
                }
            }
        };
    }
}
