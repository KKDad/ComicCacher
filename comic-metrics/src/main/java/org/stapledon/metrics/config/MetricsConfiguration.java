package org.stapledon.metrics.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.metrics.collector.AccessMetricsCollector;
import org.stapledon.metrics.collector.StorageMetricsCollector;
import org.stapledon.metrics.repository.AccessMetricsRepository;
import org.stapledon.metrics.repository.JsonMetricsRepository;
import org.stapledon.metrics.repository.MetricsArchiver;
import org.stapledon.metrics.repository.MetricsRepository;
import org.stapledon.metrics.service.JsonMetricsService;
import org.stapledon.metrics.service.MetricsService;
import org.stapledon.metrics.service.NoOpMetricsService;

import com.google.gson.Gson;
import lombok.ToString;
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
@ToString
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
            MetricsRepository metricsRepository,
            MetricsArchiver metricsArchiver,
            org.stapledon.metrics.service.MetricsUpdateService metricsUpdateService) {
        log.info("Metrics enabled - creating JsonMetricsService");
        return new JsonMetricsService(
                storageMetricsCollector,
                accessMetricsCollector,
                accessMetricsRepository,
                metricsRepository,
                metricsArchiver,
                metricsUpdateService);
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
            @Qualifier("cacheLocation") String cacheLocation) {
        log.debug("Creating StorageMetricsCollector");
        return new StorageMetricsCollector(cacheLocation);
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
     * Creates MetricsRepository when metrics are enabled.
     * Uses JSON file backend by default.
     */
    @Bean
    @ConditionalOnProperty(prefix = "comics.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MetricsRepository metricsRepository(
            @Qualifier("gsonWithLocalDate") Gson gson,
            CacheProperties cacheProperties) {
        log.debug("Creating JsonMetricsRepository");
        return new JsonMetricsRepository(gson, cacheProperties);
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
