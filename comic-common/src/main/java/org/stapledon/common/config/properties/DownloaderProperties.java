package org.stapledon.common.config.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;


/**
 * Configuration for outbound HTTP downloaders, including User-Agent strings, per-source throttle settings and HTTP 429 retry settings.
 * Maps to {@code downloader.*} properties in application.properties.
 *
 * <p>Example:
 * <pre>
 * downloader.user-agent.default-value=Mozilla/5.0 ...
 * downloader.sources.gocomics.user-agent=Mozilla/5.0 ...
 * downloader.sources.gocomics.throttle.min-delay-ms=8000
 * downloader.sources.gocomics.throttle.max-delay-ms=20000
 * downloader.sources.gocomics.retry.max-attempts=4
 * downloader.sources.gocomics.retry.initial-backoff-ms=60000
 * downloader.sources.gocomics.retry.max-backoff-ms=600000
 * </pre>
 */
@Getter
@ToString
@Builder
@AllArgsConstructor
@ConfigurationProperties(prefix = "downloader")
public class DownloaderProperties {

    private final UserAgent userAgent;

    private final Map<String, Source> sources;

    /**
     * Returns the throttle config for the given source, or empty defaults (no delay) if the source is not configured.
     */
    public Throttle throttleFor(String source) {
        if (source == null || sources == null) {
            return Throttle.builder().build();
        }
        Source cfg = sources.get(source);
        return cfg == null || cfg.getThrottle() == null ? Throttle.builder().build() : cfg.getThrottle();
    }

    /**
     * Returns the rate-limit retry config for the given source, or empty defaults (no retries) if the source is not configured.
     */
    public Retry retryFor(String source) {
        if (source == null || sources == null) {
            return Retry.builder().build();
        }
        Source cfg = sources.get(source);
        return cfg == null || cfg.getRetry() == null ? Retry.builder().build() : cfg.getRetry();
    }

    /**
     * Returns the per-source User-Agent override, or {@code null} if no override is configured.
     */
    public String userAgentFor(String source) {
        if (source == null || sources == null) {
            return null;
        }
        Source cfg = sources.get(source);
        return cfg == null ? null : cfg.getUserAgent();
    }

    @Getter
    @ToString
    @Builder
    @AllArgsConstructor
    public static class UserAgent {
        /** Global fallback User-Agent. If blank, {@code UserAgentService} uses its built-in fallback. */
        private final String defaultValue;
    }

    @Getter
    @ToString
    @Builder
    @AllArgsConstructor
    public static class Source {
        /** Optional per-source User-Agent override. */
        private final String userAgent;

        private final Throttle throttle;

        private final Retry retry;
    }

    @Getter
    @ToString
    @Builder
    @AllArgsConstructor
    public static class Throttle {
        /** Minimum delay (ms) between consecutive requests to this source. 0 disables throttling. */
        private final long minDelayMs;

        /** Maximum delay (ms) between consecutive requests to this source. Actual delay is randomized between min and max. */
        private final long maxDelayMs;
    }

    @Getter
    @ToString
    @Builder
    @AllArgsConstructor
    public static class Retry {
        /** Total attempts per download when the source answers HTTP 429 (1 or less disables retries). */
        private final int maxAttempts;

        /** Backoff (ms) after the first 429 when no Retry-After header is sent. Doubles on each further attempt. */
        private final long initialBackoffMs;

        /** Upper bound (ms) on any single backoff, including one requested by a Retry-After header. */
        private final long maxBackoffMs;
    }
}
