package org.stapledon.batch;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.stapledon.engine.batch.ComicDownloadJobScheduler;
import org.stapledon.engine.batch.ComicReconciliationJobScheduler;
import org.stapledon.engine.batch.MetricsArchiveJobScheduler;
import org.stapledon.engine.batch.MetricsUpdateJobScheduler;
import org.stapledon.engine.batch.RetrievalRecordPurgeJobScheduler;

/**
 * Configuration for batch integration tests.
 * Provides mock beans for schedulers that are disabled in batch integration tests.
 *
 * Note: ImageMetadataJobScheduler is NOT mocked because it's enabled for testing.
 */
@Configuration
@Profile("batch-integration")
public class BatchIntegrationTestConfig {

    /**
     * Mock ComicDownloadJobScheduler when batch job is disabled
     */
    @Bean
    @ConditionalOnProperty(name = "batch.comic-download.enabled", havingValue = "false")
    public ComicDownloadJobScheduler comicDownloadJobScheduler() {
        return Mockito.mock(ComicDownloadJobScheduler.class);
    }

    /**
     * Mock ComicReconciliationJobScheduler when batch job is disabled
     */
    @Bean
    @ConditionalOnProperty(name = "batch.reconciliation.enabled", havingValue = "false")
    public ComicReconciliationJobScheduler comicReconciliationJobScheduler() {
        return Mockito.mock(ComicReconciliationJobScheduler.class);
    }

    /**
     * Mock MetricsArchiveJobScheduler when batch job is disabled
     */
    @Bean
    @ConditionalOnProperty(name = "batch.metrics-archive.enabled", havingValue = "false")
    public MetricsArchiveJobScheduler metricsArchiveJobScheduler() {
        return Mockito.mock(MetricsArchiveJobScheduler.class);
    }

    /**
     * Mock MetricsUpdateJobScheduler when batch job is disabled
     */
    @Bean
    @ConditionalOnProperty(name = "batch.metrics-update.enabled", havingValue = "false")
    public MetricsUpdateJobScheduler metricsUpdateJobScheduler() {
        return Mockito.mock(MetricsUpdateJobScheduler.class);
    }

    /**
     * Mock RetrievalRecordPurgeJobScheduler when batch job is disabled
     */
    @Bean
    @ConditionalOnProperty(name = "batch.record-purge.enabled", havingValue = "false")
    public RetrievalRecordPurgeJobScheduler retrievalRecordPurgeJobScheduler() {
        return Mockito.mock(RetrievalRecordPurgeJobScheduler.class);
    }
}
