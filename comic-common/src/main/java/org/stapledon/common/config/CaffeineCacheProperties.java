package org.stapledon.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Configuration properties for Caffeine cache settings.
 * Maps to comics.cache.caffeine.* properties in application.properties.
 */
@Getter
@ToString
@Builder
@AllArgsConstructor
@ConfigurationProperties(prefix = "comics.cache.caffeine")
public class CaffeineCacheProperties {

    /** Whether Caffeine caching is enabled globally. */
    private final boolean enabled;

    /** Metadata cache settings for comic configuration data. */
    private final CacheConfig metadata;

    /** Predictive lookahead configuration. */
    private final LookaheadConfig lookahead;

    /** Configuration for an individual cache. */
    @Getter
    @ToString
    @Builder
    @AllArgsConstructor
    public static class CacheConfig {
        /** Maximum number of entries in the cache. */
        private final int maxSize;

        /** Time-to-live in minutes for cache entries. */
        private final int ttlMinutes;
    }

    /** Configuration for predictive lookahead caching. */
    @Getter
    @ToString
    @Builder
    @AllArgsConstructor
    public static class LookaheadConfig {
        /** Whether predictive lookahead is enabled. */
        private final boolean enabled;

        /** Number of comics to prefetch in each direction (N±count). */
        private final int count;
    }
}
