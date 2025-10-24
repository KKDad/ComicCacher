package org.stapledon.infrastructure.config;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.stapledon.engine.batch.ComicDownloadJobScheduler;
import org.stapledon.engine.batch.ComicReconciliationJobScheduler;
import org.stapledon.engine.batch.MetricsArchiveJobScheduler;

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
     * Mock ComicReconciliationJobScheduler when batch jobs are disabled
     * This prevents BeanCreationException if any controller tries to inject it
     */
    @Bean
    @ConditionalOnProperty(name = "batch.reconciliation.enabled", havingValue = "false")
    public ComicReconciliationJobScheduler comicReconciliationJobScheduler() {
        return Mockito.mock(ComicReconciliationJobScheduler.class);
    }

    /**
     * Mock MetricsArchiveJobScheduler when batch jobs are disabled
     */
    @Bean
    @ConditionalOnProperty(name = "batch.metrics-archive.enabled", havingValue = "false")
    public MetricsArchiveJobScheduler metricsArchiveJobScheduler() {
        return Mockito.mock(MetricsArchiveJobScheduler.class);
    }
}
