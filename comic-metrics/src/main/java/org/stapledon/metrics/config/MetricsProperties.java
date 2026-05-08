package org.stapledon.metrics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Configuration properties for metrics collection and persistence.
 */
@Getter
@ToString
@Builder
@AllArgsConstructor
@ConfigurationProperties(prefix = "comics.metrics")
public class MetricsProperties {

    /** Enable/disable metrics collection and persistence. */
    private final boolean enabled;

    /** Interval in seconds for persisting metrics. */
    private final int persistIntervalSeconds;

    /** Number of days to retain historical metrics archives. */
    private final int historyRetentionDays;

    /** Directory name for storing historical metrics archives (relative to cache location). */
    private final String archiveDirectory;

    /** Cron expression for daily archiving. */
    private final String archiveCron;
}
