package org.stapledon.metrics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for metrics collection and persistence.
 */
@Configuration
@ConfigurationProperties(prefix = "comics.metrics")
@Getter
@Setter
public class MetricsProperties {

    /**
     * Enable/disable metrics collection and persistence
     */
    private boolean enabled = true;

    /**
     * Interval in seconds for persisting metrics (default: 5 minutes)
     */
    private int persistIntervalSeconds = 300;

    /**
     * Number of days to retain historical metrics archives (default: 90 days)
     */
    private int historyRetentionDays = 90;

    /**
     * Directory name for storing historical metrics archives (relative to cache location)
     */
    private String archiveDirectory = "metrics-history";

    /**
     * Cron expression for daily archiving (default: 3:00 AM)
     */
    private String archiveCron = "0 0 3 * * ?";
}
