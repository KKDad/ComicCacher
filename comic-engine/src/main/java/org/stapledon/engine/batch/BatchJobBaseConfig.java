package org.stapledon.engine.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Base configuration for Spring Batch jobs.
 * Provides shared configuration, utilities, and conventions for all batch jobs.
 *
 * <p>
 * All batch jobs in ComicCacher follow these conventions:
 * <ul>
 * <li>Job names end with "Job" suffix (e.g., ComicDownloadJob)</li>
 * <li>All jobs run in America/Toronto timezone</li>
 * <li>All jobs use JsonBatchExecutionTracker for execution history</li>
 * <li>Jobs are scheduled via @Scheduled with cron expressions</li>
 * <li>Jobs can be enabled/disabled via application.properties</li>
 * </ul>
 *
 * <p>
 * Timezone Configuration:
 * All cron expressions should specify timezone explicitly:
 * {@code @Scheduled(cron = "0 0 6 * * ? America/Toronto")}
 *
 * <p>
 * Execution Tracking:
 * All jobs automatically export execution summaries to:
 * {@code ${cacher.cache-location}/batch-executions.json}
 *
 * <p>
 * Example Job Configuration:
 * 
 * <pre>
 * &#64;Bean
 * public Job myJob(JobRepository jobRepository,
 *         Step myStep,
 *         JsonBatchExecutionTracker tracker) {
 *     return new JobBuilder("MyJob", jobRepository)
 *             .incrementer(new RunIdIncrementer())
 *             .listener(tracker)
 *             .start(myStep)
 *             .build();
 * }
 * </pre>
 */
@Slf4j
@Configuration
public class BatchJobBaseConfig {

    /**
     * Standard timezone for all batch jobs
     */
    public static final String BATCH_TIMEZONE = "America/Toronto";

    /**
     * Standard cron schedule components
     */
    public static final class CronSchedules {
        /** Daily at 6:00 AM EST - Comic download */
        public static final String COMIC_DOWNLOAD = "0 0 6 * * ? " + BATCH_TIMEZONE;

        /** Daily at 6:15 AM EST - Configuration reconciliation */
        public static final String RECONCILIATION = "0 15 6 * * ? " + BATCH_TIMEZONE;

        /** Daily at 6:30 AM EST - Metrics archive */
        public static final String METRICS_ARCHIVE = "0 30 6 * * ? " + BATCH_TIMEZONE;

        /** Daily at 6:30 AM EST - Image metadata backfill */
        public static final String IMAGE_BACKFILL = "0 30 6 * * ? " + BATCH_TIMEZONE;

        /** Daily at 6:45 AM EST - Retrieval record purge */
        public static final String RECORD_PURGE = "0 45 6 * * ? " + BATCH_TIMEZONE;

        /** Daily at 7:00 AM EST - Comic backfill */
        public static final String COMIC_BACKFILL = "0 0 7 * * ? " + BATCH_TIMEZONE;

        private CronSchedules() {
            // Utility class
        }
    }

    /**
     * Configuration property keys for batch jobs
     */
    public static final class PropertyKeys {
        public static final String COMIC_DOWNLOAD_ENABLED = "batch.comic-download.enabled";
        public static final String COMIC_DOWNLOAD_CRON = "batch.comic-download.cron";

        public static final String RECONCILIATION_ENABLED = "batch.reconciliation.enabled";
        public static final String RECONCILIATION_CRON = "batch.reconciliation.cron";

        public static final String METRICS_ARCHIVE_ENABLED = "batch.metrics-archive.enabled";
        public static final String METRICS_ARCHIVE_CRON = "batch.metrics-archive.cron";

        public static final String IMAGE_BACKFILL_ENABLED = "batch.image-backfill.enabled";
        public static final String IMAGE_BACKFILL_CRON = "batch.image-backfill.cron";

        public static final String METRICS_UPDATE_ENABLED = "batch.metrics-update.enabled";
        public static final String METRICS_UPDATE_DELAY = "batch.metrics-update.fixed-delay";

        public static final String RECORD_PURGE_ENABLED = "batch.record-purge.enabled";
        public static final String RECORD_PURGE_CRON = "batch.record-purge.cron";

        public static final String COMIC_BACKFILL_ENABLED = "batch.comic-backfill.enabled";
        public static final String COMIC_BACKFILL_CRON = "batch.comic-backfill.cron";

        private PropertyKeys() {
            // Utility class
        }
    }

    public BatchJobBaseConfig() {
        log.info("Initializing Spring Batch configuration with timezone: {}", BATCH_TIMEZONE);
        log.info("Batch execution tracking enabled - summaries will be exported to JSON");
    }
}
