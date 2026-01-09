package org.stapledon.engine.batch.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Centralized scheduling triggers for all batch jobs. Uses @Scheduled to call the appropriate job scheduler at the configured times.
 *
 * <p>
 * This separation allows:
 * <ul>
 * <li>Job configuration to focus on job/step definitions</li>
 * <li>Scheduler beans to encapsulate execution logic</li>
 * <li>Triggers to be enabled/disabled via properties</li>
 * </ul>
 */
@Slf4j @Component
public class SchedulerTriggers {

    // Daily job schedulers
    @Autowired(required = false)
    private DailyJobScheduler comicDownloadJobScheduler;

    @Autowired(required = false)
    private DailyJobScheduler comicBackfillJobScheduler;

    @Autowired(required = false)
    private DailyJobScheduler imageMetadataBackfillJobScheduler;

    @Autowired(required = false)
    private DailyJobScheduler metricsArchiveJobScheduler;

    @Autowired(required = false)
    private DailyJobScheduler retrievalRecordPurgeJobScheduler;

    // ==================== Daily Job Triggers ====================

    @Scheduled(cron = "${batch.comic-download.cron}", zone = "${batch.timezone}") @ConditionalOnProperty(name = "batch.comic-download.enabled", havingValue = "true", matchIfMissing = true)
    public void triggerComicDownload() {
        if (comicDownloadJobScheduler != null) {
            comicDownloadJobScheduler.executeScheduled();
        }
    }

    @Scheduled(cron = "${batch.comic-backfill.cron}", zone = "${batch.timezone}") @ConditionalOnProperty(name = "batch.comic-backfill.enabled", havingValue = "true", matchIfMissing = true)
    public void triggerComicBackfill() {
        if (comicBackfillJobScheduler != null) {
            comicBackfillJobScheduler.executeScheduled();
        }
    }

    @Scheduled(cron = "${batch.image-backfill.cron}", zone = "${batch.timezone}") @ConditionalOnProperty(name = "batch.image-backfill.enabled", havingValue = "true", matchIfMissing = true)
    public void triggerImageMetadataBackfill() {
        if (imageMetadataBackfillJobScheduler != null) {
            imageMetadataBackfillJobScheduler.executeScheduled();
        }
    }

    @Scheduled(cron = "${batch.metrics-archive.cron}", zone = "${batch.timezone}") @ConditionalOnProperty(name = "batch.metrics-archive.enabled", havingValue = "true", matchIfMissing = true)
    public void triggerMetricsArchive() {
        if (metricsArchiveJobScheduler != null) {
            metricsArchiveJobScheduler.executeScheduled();
        }
    }

    @Scheduled(cron = "${batch.record-purge.cron}", zone = "${batch.timezone}") @ConditionalOnProperty(name = "batch.record-purge.enabled", havingValue = "true", matchIfMissing = true)
    public void triggerRetrievalRecordPurge() {
        if (retrievalRecordPurgeJobScheduler != null) {
            retrievalRecordPurgeJobScheduler.executeScheduled();
        }
    }

}
