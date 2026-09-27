package org.stapledon.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.stapledon.common.config.CaffeineCacheProperties;

/**
 * Unit tests for CaffeineCacheConfiguration.
 */
class CaffeineCacheConfigurationTest {

    private CaffeineCacheConfiguration configuration;
    private CaffeineCacheProperties properties;

    @BeforeEach
    void setUp() {
        properties = CaffeineCacheProperties.builder()
                .enabled(true)
                .metadata(CaffeineCacheProperties.CacheConfig.builder().maxSize(60).ttlMinutes(60).build())
                .build();

        configuration = new CaffeineCacheConfiguration(properties);
    }

    @Test
    void cacheManagerCreation() {
        CacheManager cacheManager = configuration.cacheManager();

        assertThat(cacheManager).as("Cache manager should not be null").isNotNull();
        assertThat(cacheManager instanceof CaffeineCacheManager).as("Cache manager should be CaffeineCacheManager")
                .isTrue();

        CaffeineCacheManager caffeineCacheManager = (CaffeineCacheManager) cacheManager;

        // Verify metadata cache is registered
        assertThat(caffeineCacheManager.getCacheNames().contains(CaffeineCacheConfiguration.COMIC_METADATA_CACHE))
                .as("Should contain comicMetadata cache").isTrue();
    }

    @Test
    void cacheNameConstants() {
        assertThat(CaffeineCacheConfiguration.COMIC_METADATA_CACHE).isEqualTo("comicMetadata");
    }
}
