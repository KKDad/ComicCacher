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

    /** Number of days to retain historical metrics archives. */
    private final int historyRetentionDays;
}
