package org.stapledon.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.stapledon.common.config.CaffeineCacheProperties;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Unit tests for CaffeineCacheConfiguration.
 */
class CaffeineCacheConfigurationTest {

    private CaffeineCacheConfiguration configuration;
    private CaffeineCacheProperties properties;

    @BeforeEach
    void setUp() {
        properties = new CaffeineCacheProperties();
        properties.setEnabled(true);

        // Set up default properties
        CaffeineCacheProperties.CacheConfig metadata = new CaffeineCacheProperties.CacheConfig(60, 60);
        properties.setMetadata(metadata);

        CaffeineCacheProperties.LookaheadConfig lookahead = new CaffeineCacheProperties.LookaheadConfig();
        lookahead.setEnabled(true);
        lookahead.setCount(3);
        properties.setLookahead(lookahead);

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
    void metadataCaffeineBuilder() {
        Caffeine<Object, Object> caffeine = configuration.metadataCaffeine();

        assertThat(caffeine).as("Metadata Caffeine builder should not be null").isNotNull();
    }

    @Test
    void cacheNameConstants() {
        assertThat(CaffeineCacheConfiguration.COMIC_METADATA_CACHE).isEqualTo("comicMetadata");
    }
}
