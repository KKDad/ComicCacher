package org.stapledon.common.config.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;


/**
 * Configuration for outbound HTTP downloaders, including User-Agent strings and per-source throttle settings.
 * Maps to {@code downloader.*} properties in application.properties.
 *
 * <p>Example:
 * <pre>
 * downloader.user-agent.default=Mozilla/5.0 ...
 * downloader.sources.gocomics.user-agent=Mozilla/5.0 ...
 * downloader.sources.gocomics.throttle.min-delay-ms=8000
 * downloader.sources.gocomics.throttle.max-delay-ms=20000
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
}
