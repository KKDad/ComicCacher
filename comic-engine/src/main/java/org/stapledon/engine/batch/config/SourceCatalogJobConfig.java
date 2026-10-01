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

import java.time.Duration;
import java.util.List;

import org.stapledon.engine.batch.JsonBatchExecutionTracker;
import org.stapledon.engine.batch.scheduler.DailyJobScheduler;
import org.stapledon.engine.batch.scheduler.JobParameterDefinition;
import org.stapledon.engine.batch.scheduler.JobParameterDefinition.Option;
import org.stapledon.engine.source.CatalogThumbnailService;
import org.stapledon.engine.source.ComicSource;
import org.stapledon.engine.source.SourceCatalogService;
import org.stapledon.engine.source.SourceCatalogService.RefreshResult;
import org.stapledon.engine.source.SourceCatalogService.RefreshStatus;
import org.stapledon.engine.source.SourceRegistry;

/**
 * Spring Batch configuration for the source catalog job. Reads each source's list of comics (the Sources page's catalog) when it is older than
 * {@code batch.source-catalog.max-age-days}, then detects start dates for a few configured comics that have none, reads comics' details where the
 * catalog doesn't carry them, downloads catalog thumbnails, and deletes stale ones.
 * <p>
 * The cron fires daily, but a scheduled run is skipped without any web request unless a catalog, details or thumbnails are due (read from
 * {@code source-catalog.json} only). Details expire after 30–90 days and thumbnails after about a year, so after the first week or so most days do
 * little. The Sources page's Refresh button runs this job for one source with {@code force=true}.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.source-catalog.enabled", havingValue = "true", matchIfMissing = true)
public class SourceCatalogJobConfig {

    public static final String JOB_NAME = "SourceCatalogJob";

    private final SourceRegistry sourceRegistry;
    private final SourceCatalogService catalogService;
    private final CatalogThumbnailService thumbnails;

    @Value("${batch.source-catalog.cron}")
    private String cronExpression;

    @Value("${batch.timezone:America/Toronto}")
    private String timezone;

    @Value("${batch.source-catalog.max-age-days:7}")
    private int maxAgeDays;

    @Value("${batch.source-catalog.start-detect-per-run:5}")
    private int startDetectPerRun;

    @Value("${batch.source-catalog.details-per-run:100}")
    private int detailsPerRun;

    @Value("${batch.source-catalog.thumbnails-per-run:100}")
    private int thumbnailsPerRun;

    @Value("${comics.catalog.thumbnail-max-age-days:365}")
    private int thumbnailMaxAgeDays;

    /**
     * Scheduler for SourceCatalogJob: runs daily, skipping runs when no catalog is due.
     */
    @Bean
    public DailyJobScheduler sourceCatalogJobScheduler(@Qualifier("sourceCatalogJob") Job sourceCatalogJob, JobOperator jobOperator, JsonBatchExecutionTracker tracker) {
        List<JobParameterDefinition> parameters = List.of(
                new JobParameterDefinition("source", "Source", "ENUM", false, "ALL", sourceRegistry.jobSourceOptions()),
                new JobParameterDefinition("force", "Refresh even if recently read", "ENUM", false, "false",
                        List.of(new Option("false", "No"), new Option("true", "Yes"))));
        DailyJobScheduler scheduler = new DailyJobScheduler(sourceCatalogJob, cronExpression, timezone, jobOperator, tracker,
                "Reads each source's list of comics for the Sources page, with their details and thumbnails, and detects where comics start", parameters);
        scheduler.setPrecondition(this::anyWorkDue, "no source catalog, details or thumbnails are due");
        return scheduler;
    }

    /**
     * True when a scheduled run has something to do: a catalog is due, or details or thumbnails are. Reads only {@code source-catalog.json}.
     */
    public boolean anyWorkDue() {
        return sourceRegistry.all().stream().anyMatch(source -> catalogService.isStale(source.id(), Duration.ofDays(maxAgeDays)))
                || catalogService.hasDueBackgroundWork();
    }

    /**
     * The source catalog job.
     */
    @Bean
    public Job sourceCatalogJob(JobRepository jobRepository, @Qualifier("sourceCatalogStep") Step sourceCatalogStep, JsonBatchExecutionTracker jsonBatchExecutionTracker) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(jsonBatchExecutionTracker)
                .start(sourceCatalogStep)
                .build();
    }

    /**
     * The single step: refresh, detect starts, read details, download thumbnails, tidy thumbnails.
     */
    @Bean
    public Step sourceCatalogStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            @Qualifier("sourceCatalogTasklet") Tasklet sourceCatalogTasklet) {
        return new StepBuilder("sourceCatalogStep", jobRepository)
                .tasklet(sourceCatalogTasklet, transactionManager)
                .build();
    }

    /**
     * Refreshes each due source (or the one named by the "source" parameter; "force=true" ignores the age), then detects missing starts, reads due
     * details, downloads due thumbnails and purges stale ones. Each of those reads a limited number per source and stops a source at its first HTTP 429
     * (the source is backed off; the rest wait for the next run). The step fails when any refresh it tried failed, so the batch history shows it.
     */
    @Bean
    @StepScope
    public Tasklet sourceCatalogTasklet(@Value("#{jobParameters['source']}") String sourceFilter, @Value("#{jobParameters['force']}") String force) {
        return (contribution, chunkContext) -> {
            boolean forced = Boolean.parseBoolean(force);
            boolean all = sourceFilter == null || sourceFilter.isBlank() || "ALL".equalsIgnoreCase(sourceFilter);
            int refreshed = 0;
            int failed = 0;
            for (ComicSource source : sourceRegistry.all()) {
                if (source.catalog().isEmpty() || !all && !source.id().equals(sourceFilter)) {
                    continue;
                }
                if (!forced && !catalogService.isStale(source.id(), Duration.ofDays(maxAgeDays))) {
                    log.info("Catalog for {} is less than {} days old; not refreshing", source.id(), maxAgeDays);
                    continue;
                }
                RefreshResult result = catalogService.refresh(source.id());
                if (result.status() == RefreshStatus.REFRESHED) {
                    refreshed++;
                } else if (result.status() == RefreshStatus.ALREADY_RUNNING) {
                    log.info("Catalog refresh for {} is already running; skipped", source.id());
                } else {
                    failed++;
                }
            }

            int starts = catalogService.detectMissingStarts(startDetectPerRun);
            int details = catalogService.fetchDueDetails(detailsPerRun);
            int purged = thumbnails.purge(Duration.ofDays(thumbnailMaxAgeDays));
            int downloaded = thumbnails.prefetchDue(thumbnailsPerRun);
            log.info("Source catalog job finished: {} catalogs refreshed, {} failed, {} start dates detected, {} details read, {} thumbnails downloaded, {} purged",
                    refreshed, failed, starts, details, downloaded, purged);
            if (failed > 0) {
                throw new IllegalStateException(failed + " source catalog refresh(es) failed; see the WARN lines above");
            }
            return RepeatStatus.FINISHED;
        };
    }
}
