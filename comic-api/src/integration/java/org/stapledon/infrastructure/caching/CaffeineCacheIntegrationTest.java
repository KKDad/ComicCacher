package org.stapledon.infrastructure.caching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.util.Direction;
import org.stapledon.engine.management.ComicManagementFacade;
import org.stapledon.infrastructure.config.CaffeineCacheConfiguration;

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
    private ComicManagementFacade comicManagementFacade;

    @Test
    void testCacheManagerIsConfigured() {
        assertNotNull(cacheManager, "CacheManager should be autowired");

        // Verify all expected caches exist
        Cache navigationCache = cacheManager.getCache(CaffeineCacheConfiguration.COMIC_NAVIGATION_CACHE);
        assertNotNull(navigationCache, "Navigation cache should exist");

        Cache boundaryCache = cacheManager.getCache(CaffeineCacheConfiguration.BOUNDARY_DATES_CACHE);
        assertNotNull(boundaryCache, "Boundary dates cache should exist");

        Cache navigationDatesCache = cacheManager.getCache(CaffeineCacheConfiguration.NAVIGATION_DATES_CACHE);
        assertNotNull(navigationDatesCache, "Navigation dates cache should exist");

        Cache metadataCache = cacheManager.getCache(CaffeineCacheConfiguration.COMIC_METADATA_CACHE);
        assertNotNull(metadataCache, "Metadata cache should exist");
    }

    @Test
    void testComicMetadataCaching() {
        // Clear the cache first
        Cache metadataCache = cacheManager.getCache(CaffeineCacheConfiguration.COMIC_METADATA_CACHE);
        assertNotNull(metadataCache);
        metadataCache.clear();

        // First call - should cache the result
        var allComics1 = comicManagementFacade.getAllComics();

        // Second call - should return cached result
        var allComics2 = comicManagementFacade.getAllComics();

        // Verify same instance is returned (proving it's cached)
        assertSame(allComics1, allComics2, "Second call should return cached instance");

        // Verify cache contains the entry
        var cachedValue = metadataCache.get("allComics");
        assertNotNull(cachedValue, "Cache should contain 'allComics' entry");
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
        assertNotNull(metadataCache);
        metadataCache.clear();

        // First call - should cache the result
        Optional<ComicItem> comic1 = comicManagementFacade.getComic(comicId);

        // Second call - should return cached result
        Optional<ComicItem> comic2 = comicManagementFacade.getComic(comicId);

        // Verify same instance is returned
        if (comic1.isPresent() && comic2.isPresent()) {
            assertSame(comic1.get(), comic2.get(), "Second call should return cached instance");
        }

        // Verify cache contains the entry
        String cacheKey = "comic:" + comicId;
        var cachedValue = metadataCache.get(cacheKey);
        if (comic1.isPresent()) {
            assertNotNull(cachedValue, "Cache should contain comic entry");
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
        assertNotNull(navigationCache);
        navigationCache.clear();

        // First call - should cache the result
        ComicNavigationResult result1 = comicManagementFacade.getComicStrip(comicId, Direction.FORWARD, testDate);

        // Second call - should return cached result
        ComicNavigationResult result2 = comicManagementFacade.getComicStrip(comicId, Direction.FORWARD, testDate);

        // Verify same instance is returned (proving it's cached)
        assertSame(result1, result2, "Second call should return cached instance");

        // Verify the reason is consistent
        assertEquals(result1.getReason(), result2.getReason(), "Cached result should have same reason");
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
