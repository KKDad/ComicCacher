package org.stapledon.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Configuration properties for Caffeine cache settings.
 * Maps to comics.cache.caffeine.* properties in application.properties.
 */
@ToString
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "comics.cache.caffeine")
public class CaffeineCacheProperties {

    /**
     * Whether Caffeine caching is enabled globally.
     */
    private boolean enabled = true;

    /**
     * Navigation cache settings for comic strip navigation results.
     */
    private CacheConfig navigation = new CacheConfig(300, 10);

    /**
     * Boundary dates cache settings for newest/oldest dates.
     */
    private CacheConfig boundary = new CacheConfig(100, 60);

    /**
     * Navigation dates cache settings for next/previous date lookups.
     */
    private CacheConfig navigationDates = new CacheConfig(200, 30);

    /**
     * Metadata cache settings for comic configuration data.
     */
    private CacheConfig metadata = new CacheConfig(60, 60);

    /**
     * Predictive lookahead configuration.
     */
    private LookaheadConfig lookahead = new LookaheadConfig();

    /**
     * Configuration for an individual cache.
     */
    @ToString
    @Getter
    @Setter
    public static class CacheConfig {
        /**
         * Maximum number of entries in the cache.
         */
        private int maxSize;

        /**
         * Time-to-live in minutes for cache entries.
         */
        private int ttlMinutes;

        public CacheConfig() {
        }

        public CacheConfig(int maxSize, int ttlMinutes) {
            this.maxSize = maxSize;
            this.ttlMinutes = ttlMinutes;
        }
    }

    /**
     * Configuration for predictive lookahead caching.
     */
    @ToString
    @Getter
    @Setter
    public static class LookaheadConfig {
        /**
         * Whether predictive lookahead is enabled.
         */
        private boolean enabled = true;

        /**
         * Number of comics to prefetch in each direction (N±count).
         */
        private int count = 3;
    }
}
