package org.stapledon.batch;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.stapledon.engine.batch.scheduler.DailyJobScheduler;

/**
 * Configuration for batch integration tests. Provides mock beans for schedulers that are disabled in batch integration tests.
 *
 * <p>
 * Note: Since schedulers are now created via JobConfig classes with @ConditionalOnProperty, we only need to mock them when running tests with jobs disabled.
 */
@Configuration @Profile("batch-integration")
public class BatchIntegrationTestConfig {

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
     * Mock DailyJobScheduler for RetrievalRecordPurgeJob when disabled
     */
    @Bean @ConditionalOnProperty(name = "batch.record-purge.enabled", havingValue = "false")
    public DailyJobScheduler retrievalRecordPurgeJobScheduler() {
        return Mockito.mock(DailyJobScheduler.class);
    }
}
