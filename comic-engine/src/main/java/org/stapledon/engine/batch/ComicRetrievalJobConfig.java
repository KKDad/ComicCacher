package org.stapledon.engine.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stapledon.api.dto.comic.ComicConfig;
import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.core.comic.dto.ComicDownloadRequest;
import org.stapledon.core.comic.dto.ComicDownloadResult;
import org.stapledon.core.comic.downloader.ComicDownloaderFacade;
import org.stapledon.infrastructure.config.ConfigurationFacade;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Batch configuration for comic retrieval jobs.
 * Provides comprehensive execution tracking, retry logic, and monitoring.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ComicRetrievalJobConfig {

    private final ConfigurationFacade configurationFacade;
    private final ComicDownloaderFacade comicDownloaderFacade;

    /**
     * Main job for daily comic retrieval
     */
    @Bean
    public Job dailyComicRetrievalJob(
            JobRepository jobRepository,
            Step comicRetrievalStep,
            JobExecutionListener comicJobExecutionListener) {
        
        return new JobBuilder("dailyComicRetrievalJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(comicJobExecutionListener)
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
            ItemReader<ComicDownloadRequest> comicRequestReader,
            ItemProcessor<ComicDownloadRequest, ComicDownloadResult> comicProcessor,
            ItemWriter<ComicDownloadResult> comicResultWriter) {

        return new StepBuilder("comicRetrievalStep", jobRepository)
                .<ComicDownloadRequest, ComicDownloadResult>chunk(1, transactionManager)
                .reader(comicRequestReader)
                .processor(comicProcessor)
                .writer(comicResultWriter)
                .faultTolerant()
                .skipLimit(10) // Skip up to 10 failed comics
                .skip(Exception.class)
                .retryLimit(3) // Retry up to 3 times per comic
                .retry(Exception.class)
                .build();
    }

    /**
     * Reader that provides comic download requests
     */
    @Bean
    public ItemReader<ComicDownloadRequest> comicRequestReader() {
        return new ListItemReader<>(createComicRequests());
    }

    /**
     * Processor that handles individual comic downloads
     */
    @Bean
    public ItemProcessor<ComicDownloadRequest, ComicDownloadResult> comicProcessor() {
        return new ItemProcessor<ComicDownloadRequest, ComicDownloadResult>() {
            @Override
            public ComicDownloadResult process(ComicDownloadRequest request) throws Exception {
                log.info("Processing comic: {} for date: {}", request.getComicName(), request.getDate());
                
                long startTime = System.currentTimeMillis();
                ComicDownloadResult result = comicDownloaderFacade.downloadComic(request);
                long duration = System.currentTimeMillis() - startTime;
                
                log.info("Completed comic: {} in {}ms, success: {}", 
                    request.getComicName(), duration, result.isSuccessful());
                
                return result;
            }
        };
    }

    /**
     * Writer that handles the results (mainly for logging and metrics)
     */
    @Bean
    public ItemWriter<ComicDownloadResult> comicResultWriter() {
        return chunk -> {
            for (ComicDownloadResult result : chunk.getItems()) {
                if (result.isSuccessful()) {
                    log.info("Successfully processed: {}", result.getRequest().getComicName());
                } else {
                    log.error("Failed to process: {} - {}",
                        result.getRequest().getComicName(), result.getErrorMessage());
                }
            }
        };
    }

    /**
     * Job execution listener for comprehensive logging and metrics
     */
    @Bean
    public JobExecutionListener comicJobExecutionListener() {
        return new ComicJobExecutionListener();
    }

    /**
     * Create comic download requests for today's date
     */
    private List<ComicDownloadRequest> createComicRequests() {
        LocalDate targetDate = LocalDate.now();
        ComicConfig config = configurationFacade.loadComicConfig();
        
        return config.getComics().stream()
                .map(comic -> ComicDownloadRequest.builder()
                        .comicId(comic.getId())
                        .comicName(comic.getName())
                        .source(comic.getSource())
                        .sourceIdentifier(comic.getSourceIdentifier())
                        .date(targetDate)
                        .build())
                .toList();
    }
}