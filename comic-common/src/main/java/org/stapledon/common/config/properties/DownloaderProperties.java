package org.stapledon.common.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
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
@ToString
@Getter
@Setter
@ConfigurationProperties(prefix = "downloader")
public class DownloaderProperties {

    private UserAgent userAgent = new UserAgent();

    private Map<String, Source> sources = new HashMap<>();

    /**
     * Returns the throttle config for the given source, or empty defaults (no delay) if the source is not configured.
     */
    public Throttle throttleFor(String source) {
        if (source == null) {
            return new Throttle();
        }
        Source cfg = sources.get(source);
        return cfg == null || cfg.getThrottle() == null ? new Throttle() : cfg.getThrottle();
    }

    /**
     * Returns the per-source User-Agent override, or {@code null} if no override is configured.
     */
    public String userAgentFor(String source) {
        if (source == null) {
            return null;
        }
        Source cfg = sources.get(source);
        return cfg == null ? null : cfg.getUserAgent();
    }

    @ToString
    @Getter
    @Setter
    public static class UserAgent {
        /**
         * Global fallback User-Agent. If blank, {@code UserAgentService} uses its built-in fallback.
         */
        private String defaultValue = "";
    }

    @ToString
    @Getter
    @Setter
    public static class Source {
        /**
         * Optional per-source User-Agent override.
         */
        private String userAgent;

        private Throttle throttle = new Throttle();
    }

    @ToString
    @Getter
    @Setter
    public static class Throttle {
        /**
         * Minimum delay (ms) between consecutive requests to this source. 0 disables throttling.
         */
        private long minDelayMs = 0;

        /**
         * Maximum delay (ms) between consecutive requests to this source. Actual delay is randomized between min and max.
         */
        private long maxDelayMs = 0;
    }
}
