package org.stapledon.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.stapledon.common.config.CaffeineCacheProperties;

import static org.assertj.core.api.Assertions.assertThat;

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
        CaffeineCacheProperties.CacheConfig navigation = new CaffeineCacheProperties.CacheConfig(300, 10);
        CaffeineCacheProperties.CacheConfig boundary = new CaffeineCacheProperties.CacheConfig(100, 60);
        CaffeineCacheProperties.CacheConfig navigationDates = new CaffeineCacheProperties.CacheConfig(200, 30);
        CaffeineCacheProperties.CacheConfig metadata = new CaffeineCacheProperties.CacheConfig(60, 60);

        properties.setNavigation(navigation);
        properties.setBoundary(boundary);
        properties.setNavigationDates(navigationDates);
        properties.setMetadata(metadata);

        CaffeineCacheProperties.LookaheadConfig lookahead = new CaffeineCacheProperties.LookaheadConfig();
        lookahead.setEnabled(true);
        lookahead.setCount(3);
        properties.setLookahead(lookahead);

        configuration = new CaffeineCacheConfiguration(properties);
    }

    @Test
    void testCacheManagerCreation() {
        CacheManager cacheManager = configuration.cacheManager();

        assertThat(cacheManager).as("Cache manager should not be null").isNotNull();
        assertThat(cacheManager instanceof CaffeineCacheManager).as("Cache manager should be CaffeineCacheManager").isTrue();

        CaffeineCacheManager caffeineCacheManager = (CaffeineCacheManager) cacheManager;

        // Verify all cache names are registered
        assertThat(caffeineCacheManager.getCacheNames().contains(CaffeineCacheConfiguration.COMIC_NAVIGATION_CACHE)).as("Should contain comicNavigation cache").isTrue();
        assertThat(caffeineCacheManager.getCacheNames().contains(CaffeineCacheConfiguration.BOUNDARY_DATES_CACHE)).as("Should contain boundaryDates cache").isTrue();
        assertThat(caffeineCacheManager.getCacheNames().contains(CaffeineCacheConfiguration.NAVIGATION_DATES_CACHE)).as("Should contain navigationDates cache").isTrue();
        assertThat(caffeineCacheManager.getCacheNames().contains(CaffeineCacheConfiguration.COMIC_METADATA_CACHE)).as("Should contain comicMetadata cache").isTrue();
    }

    @Test
    void testNavigationCaffeineBuilder() {
        Caffeine<Object, Object> caffeine = configuration.navigationCaffeine();

        assertThat(caffeine).as("Navigation Caffeine builder should not be null").isNotNull();
    }

    @Test
    void testBoundaryCaffeineBuilder() {
        Caffeine<Object, Object> caffeine = configuration.boundaryCaffeine();

        assertThat(caffeine).as("Boundary Caffeine builder should not be null").isNotNull();
    }

    @Test
    void testNavigationDatesCaffeineBuilder() {
        Caffeine<Object, Object> caffeine = configuration.navigationDatesCaffeine();

        assertThat(caffeine).as("NavigationDates Caffeine builder should not be null").isNotNull();
    }

    @Test
    void testMetadataCaffeineBuilder() {
        Caffeine<Object, Object> caffeine = configuration.metadataCaffeine();

        assertThat(caffeine).as("Metadata Caffeine builder should not be null").isNotNull();
    }

    @Test
    void testCacheNameConstants() {
        assertThat(CaffeineCacheConfiguration.COMIC_NAVIGATION_CACHE).isEqualTo("comicNavigation");
        assertThat(CaffeineCacheConfiguration.BOUNDARY_DATES_CACHE).isEqualTo("boundaryDates");
        assertThat(CaffeineCacheConfiguration.NAVIGATION_DATES_CACHE).isEqualTo("navigationDates");
        assertThat(CaffeineCacheConfiguration.COMIC_METADATA_CACHE).isEqualTo("comicMetadata");
    }
}
