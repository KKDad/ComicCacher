package org.stapledon.metrics.config;

import com.google.gson.Gson;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stapledon.common.infrastructure.config.StatsWriter;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.metrics.collector.AccessMetricsCollector;
import org.stapledon.metrics.collector.StorageMetricsCollector;
import org.stapledon.metrics.repository.AccessMetricsRepository;
import org.stapledon.metrics.repository.CombinedMetricsRepository;
import org.stapledon.metrics.repository.MetricsArchiver;
import org.stapledon.metrics.service.MetricsService;
import org.stapledon.metrics.service.MetricsServiceImpl;
import org.stapledon.metrics.service.NoOpMetricsService;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for metrics system.
 * Conditionally creates metrics beans based on comics.metrics.enabled property.
 *
 * When metrics are enabled (default):
 * - Creates collectors, repositories, and services
 * - Enables scheduled metrics updates and archiving
 *
 * When metrics are disabled:
 * - Creates NoOpMetricsService that returns empty data
 * - Skips all collector and repository beans
 */
@Slf4j
@Configuration
public class MetricsConfiguration {

    /**
     * Creates MetricsService when metrics are enabled.
     * This is the main facade for all metrics operations.
     */
    @Bean
    @ConditionalOnProperty(prefix = "comics.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MetricsService metricsService(
            StorageMetricsCollector storageMetricsCollector,
            AccessMetricsCollector accessMetricsCollector,
            AccessMetricsRepository accessMetricsRepository,
            CombinedMetricsRepository combinedMetricsRepository,
            MetricsArchiver metricsArchiver,
            org.stapledon.metrics.service.MetricsUpdateService metricsUpdateService) {
        log.info("Metrics enabled - creating MetricsServiceImpl");
        return new MetricsServiceImpl(
                storageMetricsCollector,
                accessMetricsCollector,
                accessMetricsRepository,
                combinedMetricsRepository,
                metricsArchiver,
                metricsUpdateService
        );
    }

    /**
     * Creates NoOpMetricsService when metrics are disabled.
     * Returns empty data for all operations.
     */
    @Bean
    @ConditionalOnProperty(prefix = "comics.metrics", name = "enabled", havingValue = "false")
    public MetricsService noOpMetricsService() {
        log.info("Metrics disabled - creating NoOpMetricsService");
        return new NoOpMetricsService();
    }

    /**
     * Creates StorageMetricsCollector when metrics are enabled.
     * Scans filesystem to compute storage statistics.
     */
    @Bean
    @ConditionalOnProperty(prefix = "comics.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public StorageMetricsCollector storageMetricsCollector(
            @Qualifier("cacheLocation") String cacheLocation,
            StatsWriter statsWriter) {
        log.debug("Creating StorageMetricsCollector");
        return new StorageMetricsCollector(cacheLocation, statsWriter);
    }

    /**
     * Creates AccessMetricsCollector when metrics are enabled.
     * Tracks comic access patterns and cache hit/miss rates.
     */
    @Bean
    @ConditionalOnProperty(prefix = "comics.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AccessMetricsCollector accessMetricsCollector(
            @Qualifier("cacheLocation") String cacheLocation,
            ComicStorageFacade storageFacade,
            AccessMetricsRepository accessMetricsRepository) {
        log.debug("Creating AccessMetricsCollector");
        return new AccessMetricsCollector(cacheLocation, storageFacade, accessMetricsRepository);
    }

    /**
     * Creates AccessMetricsRepository when metrics are enabled.
     * Persists access metrics to JSON files.
     */
    @Bean
    @ConditionalOnProperty(prefix = "comics.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AccessMetricsRepository accessMetricsRepository(
            @Qualifier("gsonWithLocalDate") Gson gson,
            CacheProperties cacheProperties) {
        log.debug("Creating AccessMetricsRepository");
        return new AccessMetricsRepository(gson, cacheProperties);
    }

    /**
     * Creates CombinedMetricsRepository when metrics are enabled.
     * Persists combined metrics to JSON files.
     */
    @Bean
    @ConditionalOnProperty(prefix = "comics.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CombinedMetricsRepository combinedMetricsRepository(
            @Qualifier("gsonWithLocalDate") Gson gson,
            CacheProperties cacheProperties) {
        log.debug("Creating CombinedMetricsRepository");
        return new CombinedMetricsRepository(gson, cacheProperties);
    }

    /**
     * Creates MetricsArchiver when metrics are enabled.
     * Archives daily metrics snapshots for historical analysis.
     */
    @Bean
    @ConditionalOnProperty(prefix = "comics.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MetricsArchiver metricsArchiver(
            @Qualifier("gsonWithLocalDate") Gson gson,
            CacheProperties cacheProperties) {
        log.debug("Creating MetricsArchiver");
        return new MetricsArchiver(gson, cacheProperties);
    }
}
