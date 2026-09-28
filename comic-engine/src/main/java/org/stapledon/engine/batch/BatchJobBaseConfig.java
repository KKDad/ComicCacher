package org.stapledon.engine.batch;

/**
 * Base configuration for Spring Batch jobs. Provides shared configuration, utilities, and conventions for all batch jobs.
 *
 * <p>
 * All batch jobs in ComicCacher follow these conventions:
 * <ul>
 * <li>Job names end with "Job" suffix (e.g., ComicDownloadJob)</li>
 * <li>All jobs run in {@code batch.timezone} (cron schedules, and what "today" is via the application Clock)</li>
 * <li>All jobs use JsonBatchExecutionTracker for execution history</li>
 * <li>Jobs are scheduled via @Scheduled with cron expressions</li>
 * <li>Jobs can be enabled/disabled via application.properties</li>
 * </ul>
 *
 * <p>
 * Timezone Configuration: {@code @Scheduled} triggers set {@code zone = "${batch.timezone}"}; never rely on the JVM's default zone.
 *
 * <p>
 * Execution Tracking: All jobs automatically export execution summaries to: {@code ${cacher.cache-location}/batch-executions.json}
 *
 * <p>
 * Example Job Configuration:
 *
 * <pre>
 * &#64;Bean
 * public Job myJob(JobRepository jobRepository, Step myStep, JsonBatchExecutionTracker tracker) {
 *     return new JobBuilder("MyJob", jobRepository).listener(tracker).start(myStep).build();
 * }
 * </pre>
 */
public final class BatchJobBaseConfig {

    private BatchJobBaseConfig() {
        // Utility class
    }

    /**
     * Canonical set of known batch job names. Used by SchedulerHealthCheck to detect missing or unexpected schedulers. When adding a new batch job, add its name here.
     */
    public static final java.util.Set<String> KNOWN_JOBS = java.util.Set.of("AvatarBackfillJob", "ComicBackfillJob", "ComicDownloadJob", "ImageMetadataBackfillJob", "MetricsArchiveJob",
            "RetrievalRecordPurgeJob");

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

        public static final String RECORD_PURGE_ENABLED = "batch.record-purge.enabled";
        public static final String RECORD_PURGE_CRON = "batch.record-purge.cron";

        public static final String COMIC_BACKFILL_ENABLED = "batch.comic-backfill.enabled";
        public static final String COMIC_BACKFILL_CRON = "batch.comic-backfill.cron";

        public static final String AVATAR_BACKFILL_ENABLED = "batch.avatar-backfill.enabled";
        public static final String AVATAR_BACKFILL_CRON = "batch.avatar-backfill.cron";

        private PropertyKeys() {
            // Utility class
        }
    }

}
