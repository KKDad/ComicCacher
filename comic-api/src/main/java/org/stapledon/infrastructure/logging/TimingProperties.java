package org.stapledon.infrastructure.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Thresholds past which request timings are logged at WARN, so slow page loads stand out without turning on DEBUG.
 *
 * @param slowRequestMs a request's completion line is logged at WARN at or above this total
 * @param slowFetcherMs a single GraphQL field (data fetcher) at or above this logs its own WARN line
 * @param slowStorageMs a single storage read (strip image, avatar, JSON file) at or above this logs its own WARN line
 */
@ConfigurationProperties(prefix = "comics.timing")
public record TimingProperties(
        @DefaultValue("1000") long slowRequestMs,
        @DefaultValue("250") long slowFetcherMs,
        @DefaultValue("200") long slowStorageMs) {

    /** The defaults, for tests and code that runs without Spring. */
    public static TimingProperties defaults() {
        return new TimingProperties(1000, 250, 200);
    }
}
