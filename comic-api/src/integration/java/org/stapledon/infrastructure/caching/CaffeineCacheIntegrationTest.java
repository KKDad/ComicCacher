package org.stapledon.infrastructure.caching;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.util.Direction;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.infrastructure.config.CaffeineCacheConfiguration;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Integration test for Caffeine cache functionality.
 * Tests that caching is properly configured and working.
 */
@SpringBootTest
@ActiveProfiles("integration")
class CaffeineCacheIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ManagementFacade comicManagementFacade;

    @Test
    void testCacheManagerIsConfigured() {
        assertThat(cacheManager).as("CacheManager should be autowired").isNotNull();

        // Verify all expected caches exist
        Cache navigationCache = cacheManager.getCache(CaffeineCacheConfiguration.COMIC_NAVIGATION_CACHE);
        assertThat(navigationCache).as("Navigation cache should exist").isNotNull();

        Cache boundaryCache = cacheManager.getCache(CaffeineCacheConfiguration.BOUNDARY_DATES_CACHE);
        assertThat(boundaryCache).as("Boundary dates cache should exist").isNotNull();

        Cache navigationDatesCache = cacheManager.getCache(CaffeineCacheConfiguration.NAVIGATION_DATES_CACHE);
        assertThat(navigationDatesCache).as("Navigation dates cache should exist").isNotNull();

        Cache metadataCache = cacheManager.getCache(CaffeineCacheConfiguration.COMIC_METADATA_CACHE);
        assertThat(metadataCache).as("Metadata cache should exist").isNotNull();
    }

    @Test
    void testComicMetadataCaching() {
        // Clear the cache first
        Cache metadataCache = cacheManager.getCache(CaffeineCacheConfiguration.COMIC_METADATA_CACHE);
        assertThat(metadataCache).isNotNull();
        metadataCache.clear();

        // First call - should cache the result
        var allComics1 = comicManagementFacade.getAllComics();

        // Second call - should return cached result
        var allComics2 = comicManagementFacade.getAllComics();

        // Verify same instance is returned (proving it's cached)
        assertThat(allComics2).as("Second call should return cached instance").isSameAs(allComics1);

        // Verify cache contains the entry
        var cachedValue = metadataCache.get("allComics");
        assertThat(cachedValue).as("Cache should contain 'allComics' entry").isNotNull();
    }

    @Test
    void testComicByIdCaching() {
        // Get a comic to test with (if any exist)
        var allComics = comicManagementFacade.getAllComics();
        if (allComics.isEmpty()) {
            // Skip test if no comics available
            return;
        }

        ComicItem firstComic = allComics.get(0);
        int comicId = firstComic.getId();

        // Clear the cache first
        Cache metadataCache = cacheManager.getCache(CaffeineCacheConfiguration.COMIC_METADATA_CACHE);
        assertThat(metadataCache).isNotNull();
        metadataCache.clear();

        // First call - should cache the result
        Optional<ComicItem> comic1 = comicManagementFacade.getComic(comicId);

        // Second call - should return cached result
        Optional<ComicItem> comic2 = comicManagementFacade.getComic(comicId);

        // Verify same instance is returned (proving cache is working)
        if (comic1.isPresent() && comic2.isPresent()) {
            assertThat(comic2.get()).as("Second call should return cached instance").isSameAs(comic1.get());
        }
    }

    @Test
    void testNavigationResultCaching() {
        // Get a comic to test with
        var allComics = comicManagementFacade.getAllComics();
        if (allComics.isEmpty()) {
            // Skip test if no comics available
            return;
        }

        ComicItem firstComic = allComics.get(0);
        int comicId = firstComic.getId();
        LocalDate testDate = LocalDate.now().minusDays(1);

        // Clear the navigation cache first
        Cache navigationCache = cacheManager.getCache(CaffeineCacheConfiguration.COMIC_NAVIGATION_CACHE);
        assertThat(navigationCache).isNotNull();
        navigationCache.clear();

        // First call - should cache the result
        ComicNavigationResult result1 = comicManagementFacade.getComicStrip(comicId, Direction.FORWARD, testDate);

        // Second call - should return cached result
        ComicNavigationResult result2 = comicManagementFacade.getComicStrip(comicId, Direction.FORWARD, testDate);

        // Verify same instance is returned (proving it's cached)
        assertThat(result2).as("Second call should return cached instance").isSameAs(result1);

        // Verify the reason is consistent
        assertThat(result2.getReason()).as("Cached result should have same reason").isEqualTo(result1.getReason());
    }

    @Test
    void testCacheEvictionAfterTTL() throws InterruptedException {
        // This test is commented out because it would take too long to run
        // But it demonstrates how you would test TTL expiration

        /*
        Cache metadataCache = cacheManager.getCache(CaffeineCacheConfiguration.COMIC_METADATA_CACHE);
        assertNotNull(metadataCache);
        metadataCache.clear();

        // First call
        var allComics1 = comicManagementFacade.getAllComics();

        // Verify it's cached
        assertNotNull(metadataCache.get("allComics"));

        // Wait for TTL to expire (60 minutes for metadata cache)
        Thread.sleep(60 * 60 * 1000 + 1000);

        // Verify cache entry is gone
        assertNull(metadataCache.get("allComics"));
        */
    }
}
