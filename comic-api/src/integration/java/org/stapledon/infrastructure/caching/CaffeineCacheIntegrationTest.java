package org.stapledon.infrastructure.caching;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.infrastructure.config.CaffeineCacheConfiguration;

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
    void cacheManagerIsConfigured() {
        assertThat(cacheManager).as("CacheManager should be autowired").isNotNull();

        // Verify expected cache exists
        Cache metadataCache = cacheManager.getCache(CaffeineCacheConfiguration.COMIC_METADATA_CACHE);
        assertThat(metadataCache).as("Metadata cache should exist").isNotNull();
    }

    @Test
    void comicMetadataCaching() {
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
    void comicByIdCaching() {
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
}
