package org.stapledon.infrastructure.config;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.stapledon.engine.batch.ComicDownloadJobScheduler;
import org.stapledon.engine.batch.ImageMetadataJobScheduler;
import org.stapledon.engine.batch.MetricsArchiveJobScheduler;
import org.stapledon.engine.batch.MetricsUpdateJobScheduler;
import org.stapledon.engine.batch.RetrievalRecordPurgeJobScheduler;

/**
 * Configuration for integration tests.
 * Provides mock beans for schedulers that are conditionally disabled in integration tests.
 */
@Configuration
@Profile("integration")
public class IntegrationTestConfig {

    /**
     * Mock ComicDownloadJobScheduler when batch jobs are disabled
     * This prevents BeanCreationException when BatchJobController tries to inject it
     */
    @Bean
    @ConditionalOnProperty(name = "batch.comic-download.enabled", havingValue = "false")
    public ComicDownloadJobScheduler comicDownloadJobScheduler() {
        return Mockito.mock(ComicDownloadJobScheduler.class);
    }

    /**
     * Mock MetricsArchiveJobScheduler when batch jobs are disabled
     */
    @Bean
    @ConditionalOnProperty(name = "batch.metrics-archive.enabled", havingValue = "false")
    public MetricsArchiveJobScheduler metricsArchiveJobScheduler() {
        return Mockito.mock(MetricsArchiveJobScheduler.class);
    }

    /**
     * Mock ImageMetadataJobScheduler when batch jobs are disabled
     */
    @Bean
    @ConditionalOnProperty(name = "batch.image-backfill.enabled", havingValue = "false")
    public ImageMetadataJobScheduler imageMetadataJobScheduler() {
        return Mockito.mock(ImageMetadataJobScheduler.class);
    }

    /**
     * Mock MetricsUpdateJobScheduler when batch jobs are disabled
     */
    @Bean
    @ConditionalOnProperty(name = "batch.metrics-update.enabled", havingValue = "false")
    public MetricsUpdateJobScheduler metricsUpdateJobScheduler() {
        return Mockito.mock(MetricsUpdateJobScheduler.class);
    }

    /**
     * Mock RetrievalRecordPurgeJobScheduler when batch jobs are disabled
     */
    @Bean
    @ConditionalOnProperty(name = "batch.record-purge.enabled", havingValue = "false")
    public RetrievalRecordPurgeJobScheduler retrievalRecordPurgeJobScheduler() {
        return Mockito.mock(RetrievalRecordPurgeJobScheduler.class);
    }
}

