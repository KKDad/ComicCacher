package org.stapledon.engine.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ComicDownloadResult.FailureKind;
import org.stapledon.common.dto.SaveResult;
import org.stapledon.common.util.LogContext;
import org.stapledon.engine.batch.BackfillStateService;
import org.stapledon.engine.batch.ComicBackfillService;
import org.stapledon.engine.batch.ComicBackfillService.BackfillTask;
import org.stapledon.engine.batch.ComicBackfillService.DateBackfillTask;
import org.stapledon.engine.batch.ComicBackfillService.StripBackfillTask;
import org.stapledon.engine.batch.JsonBatchExecutionTracker;
import org.stapledon.engine.batch.scheduler.DailyJobScheduler;
import org.stapledon.engine.batch.scheduler.JobParameterDefinition;
import org.stapledon.engine.batch.scheduler.JobParameterDefinition.Option;
import org.stapledon.engine.management.ManagementFacade;

/**
 * Spring Batch configuration for comic backfill job. Gradually backfills missing comic strips: recent days first, then older history.
 * <p>
 * The job runs at every cron time (several times a day), but a scheduled run is skipped without any web request when
 * {@link ComicBackfillService#hasMissingStrips(String)} finds nothing to do. Within a run, the first HTTP 429 from a source stops backfill for that source
 * until the next run. Unavailable and duplicate results are recorded in {@link BackfillStateService} so later runs can skip them.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.comic-backfill.enabled", havingValue = "true", matchIfMissing = true)
public class ComicBackfillJobConfig {

    private final ManagementFacade managementFacade;
    private final ComicBackfillService backfillService;
    private final BackfillStateService backfillState;

    @Value("${batch.comic-backfill.chunk-size:10}")
    private int chunkSize;

    @Value("${batch.comic-backfill.delay-between-comics-ms:2000}")
    private long delayBetweenComics;

    @Value("${batch.comic-backfill.cron}")
    private String cronExpression;

    @Value("${batch.timezone:America/Toronto}")
    private String timezone;

    private static final List<JobParameterDefinition> BACKFILL_PARAMETERS = List.of(
            new JobParameterDefinition("source", "Source Filter", "ENUM", false, "ALL",
                    List.of(new Option("ALL", "All Sources"),
                            new Option("gocomics", "GoComics"),
                            new Option("comicskingdom", "Comics Kingdom"),
                            new Option("freefall", "Freefall"))),
            new JobParameterDefinition("resetState", "Forget given-up dates and learned horizons", "ENUM", false, "false",
                    List.of(new Option("false", "No"),
                            new Option("true", "Yes")))
    );

    /**
     * Scheduler for ComicBackfillJob - runs at every configured cron time, skipping runs with nothing to backfill. Triggered by SchedulerTriggers component.
     */
    @Bean
    public DailyJobScheduler comicBackfillJobScheduler(@Qualifier("comicBackfillJob") Job comicBackfillJob, JobOperator jobOperator, JsonBatchExecutionTracker tracker) {
        DailyJobScheduler scheduler = new DailyJobScheduler(comicBackfillJob, cronExpression, timezone, jobOperator, tracker,
                "Backfills missing comic strips: recent days first, then older gaps", BACKFILL_PARAMETERS);
        scheduler.setMultipleRunsPerDay(true);
        scheduler.setPrecondition(() -> backfillService.hasMissingStrips(null), "nothing to backfill");
        return scheduler;
    }

    /**
     * Main job for comic backfill
     */
    @Bean
    @Qualifier("comicBackfillJob")
    public Job comicBackfillJob(JobRepository jobRepository, @Qualifier("comicBackfillStep") Step comicBackfillStep, JsonBatchExecutionTracker jsonBatchExecutionTracker) {

        return new JobBuilder("ComicBackfillJob", jobRepository)
                .listener(jsonBatchExecutionTracker)
                .start(comicBackfillStep).build();
    }

    /**
     * Step for processing comic backfill tasks. Writes the backfill state once more when the step ends, in case the last chunk had nothing to write.
     */
    @Bean
    @Qualifier("comicBackfillStep")
    public Step comicBackfillStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, @Qualifier("backfillTaskReader") ItemReader<BackfillTask> backfillTaskReader,
            @Qualifier("backfillTaskProcessor") ItemProcessor<BackfillTask, ComicDownloadResult> backfillTaskProcessor,
            @Qualifier("backfillTaskWriter") ItemWriter<ComicDownloadResult> backfillTaskWriter) {

        return new StepBuilder("comicBackfillStep", jobRepository).<BackfillTask, ComicDownloadResult>chunk(chunkSize).transactionManager(transactionManager).reader(backfillTaskReader)
                .processor(backfillTaskProcessor).writer(backfillTaskWriter).listener(new StepExecutionListener() {
                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        backfillState.flush();
                        log.info("Backfill step {}: read={}, filtered={} (cached or source stopped), written={}, skipped={}",
                                stepExecution.getExitStatus().getExitCode(), stepExecution.getReadCount(), stepExecution.getFilterCount(),
                                stepExecution.getWriteCount(), stepExecution.getSkipCount());
                        return stepExecution.getExitStatus();
                    }
                }).build();
    }

    /**
     * Reader that provides the list of backfill tasks (comic + date pairs). Uses @StepScope so findMissingStrips() is called when the job runs, not at application startup. Accepts an
     * optional "source" job parameter to filter by comic source, and "resetState=true" to forget what earlier runs learned first.
     */
    @Bean
    @StepScope
    @Qualifier("backfillTaskReader")
    public ItemReader<BackfillTask> backfillTaskReader(@Value("#{jobParameters['source']}") String sourceFilter,
            @Value("#{jobParameters['resetState']}") String resetState) {
        if (Boolean.parseBoolean(resetState)) {
            backfillState.reset();
        }
        log.debug("Building backfill task list for job execution (sourceFilter={})", sourceFilter);
        List<BackfillTask> tasks = backfillService.findMissingStrips(sourceFilter);
        if (tasks.isEmpty()) {
            log.info("No missing strips found - backfill has nothing to process");
        } else {
            log.info("Backfill reader prepared {} tasks (chunk size: {}, delay: {}ms)", tasks.size(), chunkSize, delayBetweenComics);
        }
        return new ListItemReader<>(tasks);
    }

    /**
     * Processor that downloads a comic for a specific date. Uses downloadComicForDate for efficient single-comic downloads - the comic has already been validated and filtered by
     * ComicBackfillService. Step-scoped so each run starts with an empty set of rate-limited sources.
     * <ul>
     * <li>A 429 fails at once (the source still backs off) and stops that source's remaining tasks for this run.</li>
     * <li>Unavailable and duplicate results are recorded so later runs can give up on the date or learn the source's history horizon.</li>
     * </ul>
     */
    @Bean
    @StepScope
    @Qualifier("backfillTaskProcessor")
    public ItemProcessor<BackfillTask, ComicDownloadResult> backfillTaskProcessor() {
        Set<String> stoppedSources = new HashSet<>();
        return task -> {
            String source = task.comic().getSource();
            if (stoppedSources.contains(source)) {
                log.debug("Skipping {} backfill for {} - source was rate limited this run", source, task.comic().getName());
                return null;
            }
            try (var _ = MDC.putCloseable(LogContext.COMIC, task.comic().getName());
                    var _ = MDC.putCloseable(task instanceof StripBackfillTask ? LogContext.STRIP : LogContext.DATE, taskTarget(task))) {
                // Add a small delay between comics to avoid overwhelming sources
                if (delayBetweenComics > 0) {
                    Thread.sleep(delayBetweenComics);
                }

                return switch (task) {
                    case DateBackfillTask dateTask -> {
                        log.info("Backfilling {} for date: {}", dateTask.comic().getName(), dateTask.date());
                        ComicDownloadResult result = managementFacade.downloadComicForDate(dateTask.comic(), dateTask.date(), true).orElse(null);
                        backfillState.recordAttempt(source);
                        recordOutcome(dateTask, result, stoppedSources);
                        yield result;
                    }
                    case StripBackfillTask(var comic, var stripNumber) -> {
                        log.info("Backfilling {} for strip #{}", comic.getName(), stripNumber);
                        ComicDownloadResult result = managementFacade.downloadComicByStripNumber(comic, stripNumber).orElse(null);
                        backfillState.recordAttempt(source);
                        if (result != null && result.isRateLimited()) {
                            stopSource(source, stoppedSources);
                        }
                        yield result;
                    }
                };
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Backfill interrupted for {} ({})", task.comic().getName(), taskTarget(task));
                return failedResult(task, "Backfill interrupted");
            } catch (Exception e) {
                // Return a failure rather than null: null would be counted as filtered and the error would vanish from the step counts
                log.error("Error backfilling {} ({})", task.comic().getName(), taskTarget(task), e);
                return failedResult(task, "Error backfilling: " + e.getMessage());
            }
        };
    }

    private static String taskTarget(BackfillTask task) {
        return switch (task) {
            case DateBackfillTask(var _, var date) -> date.toString();
            case StripBackfillTask(var _, var stripNumber) -> "#" + stripNumber;
        };
    }

    private static ComicDownloadResult failedResult(BackfillTask task, String message) {
        ComicDownloadRequest.ComicDownloadRequestBuilder request = ComicDownloadRequest.builder()
                .comicId(task.comic().getId())
                .comicName(task.comic().getName())
                .source(task.comic().getSource())
                .sourceIdentifier(task.comic().getSourceIdentifier());
        if (task instanceof DateBackfillTask(var _, var date)) {
            request.date(date);
        }
        return ComicDownloadResult.failure(request.build(), message, FailureKind.ERROR);
    }

    /**
     * Feeds one date task's result to the backfill state, and stops the source for this run on a 429.
     */
    private void recordOutcome(DateBackfillTask task, ComicDownloadResult result, Set<String> stoppedSources) {
        if (result == null) {
            // Already cached, or the save failed: nothing learned about the source
            return;
        }
        String source = task.comic().getSource();
        if (result.isRateLimited()) {
            stopSource(source, stoppedSources);
        } else if (result.isSuccessful() && result.getSaveOutcome() == SaveResult.Outcome.DUPLICATE_SKIPPED) {
            backfillState.recordUnavailable(task.comic(), task.date(), BackfillStateService.OUTCOME_DUPLICATE);
        } else if (result.isSuccessful()) {
            backfillState.recordSuccess(task.comic(), task.date());
        } else if (result.getFailureKind() == FailureKind.UNAVAILABLE) {
            backfillState.recordUnavailable(task.comic(), task.date(), BackfillStateService.OUTCOME_UNAVAILABLE);
        }
        // Other failures (network errors, exceptions) are transient: retry next run
    }

    private static void stopSource(String source, Set<String> stoppedSources) {
        stoppedSources.add(source);
        log.warn("Source {} rate limited backfill (HTTP 429); skipping its remaining backfill tasks until the next run", source);
    }

    /**
     * Writer that logs the backfill results and writes what this chunk taught the backfill state.
     */
    @Bean
    @Qualifier("backfillTaskWriter")
    public ItemWriter<ComicDownloadResult> backfillTaskWriter() {
        return chunk -> {
            int successCount = 0;
            int duplicateCount = 0;
            int failureCount = 0;

            for (ComicDownloadResult result : chunk.getItems()) {
                if (result.isSuccessful() && result.getSaveOutcome() == SaveResult.Outcome.DUPLICATE_SKIPPED) {
                    duplicateCount++;
                    log.info("Backfill got a duplicate image: {} for {}", result.getRequest().getComicName(), result.getRequest().getDate());
                } else if (result.isSuccessful()) {
                    successCount++;
                    log.info("Successfully backfilled: {} for {}", result.getRequest().getComicName(), result.getRequest().getDate());
                } else {
                    failureCount++;
                    log.warn("Failed to backfill: {} for {} - {}", result.getRequest().getComicName(), result.getRequest().getDate(), result.getErrorMessage());
                }
            }

            log.info("Backfill chunk complete: {} successful, {} duplicate, {} failed", successCount, duplicateCount, failureCount);
            backfillState.flush();
        };
    }
}
