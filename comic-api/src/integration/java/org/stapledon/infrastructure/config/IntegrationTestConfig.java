package org.stapledon.infrastructure.config;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.stapledon.engine.batch.scheduler.DailyJobScheduler;

/**
 * Configuration for integration tests. Provides mock beans for schedulers that are conditionally disabled in integration tests.
 *
 * <p>
 * Since schedulers are now created via JobConfig classes with @ConditionalOnProperty, we only need to mock them when running tests with jobs disabled.
 */
@Configuration @Profile("integration")
public class IntegrationTestConfig {

    /**
     * Mock DailyJobScheduler for ComicDownloadJob when disabled
     */
    @Bean @ConditionalOnProperty(name = "batch.comic-download.enabled", havingValue = "false")
    public DailyJobScheduler comicDownloadJobScheduler() {
        return Mockito.mock(DailyJobScheduler.class);
    }

    /**
     * Mock DailyJobScheduler for MetricsArchiveJob when disabled
     */
    @Bean @ConditionalOnProperty(name = "batch.metrics-archive.enabled", havingValue = "false")
    public DailyJobScheduler metricsArchiveJobScheduler() {
        return Mockito.mock(DailyJobScheduler.class);
    }

    /**
     * Mock DailyJobScheduler for ImageMetadataBackfillJob when disabled
     */
    @Bean @ConditionalOnProperty(name = "batch.image-backfill.enabled", havingValue = "false")
    public DailyJobScheduler imageMetadataBackfillJobScheduler() {
        return Mockito.mock(DailyJobScheduler.class);
    }

    /**
     * Mock DailyJobScheduler for RetrievalRecordPurgeJob when disabled
     */
    @Bean @ConditionalOnProperty(name = "batch.record-purge.enabled", havingValue = "false")
    public DailyJobScheduler retrievalRecordPurgeJobScheduler() {
        return Mockito.mock(DailyJobScheduler.class);
    }
}
