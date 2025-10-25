package org.stapledon.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertNotNull(cacheManager, "Cache manager should not be null");
        assertTrue(cacheManager instanceof CaffeineCacheManager, "Cache manager should be CaffeineCacheManager");

        CaffeineCacheManager caffeineCacheManager = (CaffeineCacheManager) cacheManager;

        // Verify all cache names are registered
        assertTrue(caffeineCacheManager.getCacheNames().contains(CaffeineCacheConfiguration.COMIC_NAVIGATION_CACHE),
            "Should contain comicNavigation cache");
        assertTrue(caffeineCacheManager.getCacheNames().contains(CaffeineCacheConfiguration.BOUNDARY_DATES_CACHE),
            "Should contain boundaryDates cache");
        assertTrue(caffeineCacheManager.getCacheNames().contains(CaffeineCacheConfiguration.NAVIGATION_DATES_CACHE),
            "Should contain navigationDates cache");
        assertTrue(caffeineCacheManager.getCacheNames().contains(CaffeineCacheConfiguration.COMIC_METADATA_CACHE),
            "Should contain comicMetadata cache");
    }

    @Test
    void testNavigationCaffeineBuilder() {
        Caffeine<Object, Object> caffeine = configuration.navigationCaffeine();

        assertNotNull(caffeine, "Navigation Caffeine builder should not be null");
    }

    @Test
    void testBoundaryCaffeineBuilder() {
        Caffeine<Object, Object> caffeine = configuration.boundaryCaffeine();

        assertNotNull(caffeine, "Boundary Caffeine builder should not be null");
    }

    @Test
    void testNavigationDatesCaffeineBuilder() {
        Caffeine<Object, Object> caffeine = configuration.navigationDatesCaffeine();

        assertNotNull(caffeine, "NavigationDates Caffeine builder should not be null");
    }

    @Test
    void testMetadataCaffeineBuilder() {
        Caffeine<Object, Object> caffeine = configuration.metadataCaffeine();

        assertNotNull(caffeine, "Metadata Caffeine builder should not be null");
    }

    @Test
    void testCacheNameConstants() {
        assertEquals("comicNavigation", CaffeineCacheConfiguration.COMIC_NAVIGATION_CACHE);
        assertEquals("boundaryDates", CaffeineCacheConfiguration.BOUNDARY_DATES_CACHE);
        assertEquals("navigationDates", CaffeineCacheConfiguration.NAVIGATION_DATES_CACHE);
        assertEquals("comicMetadata", CaffeineCacheConfiguration.COMIC_METADATA_CACHE);
    }
}
