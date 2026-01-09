package org.stapledon.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stapledon.common.config.CaffeineCacheProperties;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for Caffeine cache manager.
 * Provides caches with configurable settings:
 * - comicMetadata: Caches ComicItem configuration data
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "comics.cache.caffeine.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class CaffeineCacheConfiguration {

    private final CaffeineCacheProperties cacheProperties;

    /**
     * Cache names used throughout the application.
     */
    public static final String COMIC_METADATA_CACHE = "comicMetadata";

    /**
     * Creates and configures the Caffeine cache manager with multiple caches.
     */
    @Bean
    public CacheManager cacheManager() {
        log.info("Initializing Caffeine cache manager with properties: {}", cacheProperties);

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                COMIC_METADATA_CACHE);

        // Configure cache builder
        cacheManager.setCacheSpecification(buildCacheSpec());

        log.info("Caffeine cache manager initialized with caches: {}",
                COMIC_METADATA_CACHE);

        return cacheManager;
    }

    /**
     * Builds the default cache specification string.
     * Individual cache configurations will be applied via @Cacheable annotations.
     */
    private String buildCacheSpec() {
        // Use metadata cache settings as default
        return String.format("maximumSize=%d,expireAfterWrite=%dm",
                cacheProperties.getMetadata().getMaxSize(),
                cacheProperties.getMetadata().getTtlMinutes());
    }

    /**
     * Creates a Caffeine cache builder for metadata cache.
     */
    @Bean(name = "metadataCaffeine")
    public Caffeine<Object, Object> metadataCaffeine() {
        return buildCaffeine(cacheProperties.getMetadata());
    }

    /**
     * Builds a Caffeine cache instance with the given configuration.
     */
    private Caffeine<Object, Object> buildCaffeine(CaffeineCacheProperties.CacheConfig config) {
        log.debug("Building Caffeine cache with maxSize={}, ttlMinutes={}",
                config.getMaxSize(), config.getTtlMinutes());

        return Caffeine.newBuilder()
                .maximumSize(config.getMaxSize())
                .expireAfterWrite(config.getTtlMinutes(), TimeUnit.MINUTES)
                .recordStats();
    }
}
