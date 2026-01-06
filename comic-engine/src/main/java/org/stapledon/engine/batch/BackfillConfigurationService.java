package org.stapledon.engine.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.stapledon.common.dto.BackfillSourceConfig;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing backfill configuration including source-specific limits.
 * <p>
 * This service provides centralized access to backfill configuration,
 * supporting:
 * <ul>
 * <li>Global default limits for max comics per day and max days back</li>
 * <li>Source-specific overrides for individual comic sources</li>
 * <li>Per-source enable/disable control</li>
 * </ul>
 *
 * <p>
 * Configuration example in application.properties:
 *
 * <pre>
 * batch.comic-backfill.default-max-per-day=50
 * batch.comic-backfill.default-max-days-back=365
 * batch.comic-backfill.sources.gocomics.max-per-day=20
 * batch.comic-backfill.sources.gocomics.max-days-back=730
 * </pre>
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "batch.comic-backfill")
public class BackfillConfigurationService {

    /**
     * Whether backfill is enabled globally.
     */
    private boolean enabled = true;

    /**
     * Maximum consecutive failures before stopping scan for a comic.
     * This helps detect comics that don't exist as far back as we're scanning.
     */
    private int maxConsecutiveFailures = 3;

    /**
     * Global default for max comics per day per source (can be overridden per
     * source).
     */
    private int defaultMaxPerDay = 50;

    /**
     * Global default for max days back (can be overridden per source).
     */
    private int defaultMaxDaysBack = 365;

    /**
     * Source-specific configurations.
     * Key is the source identifier (e.g., "gocomics", "comicskingdom").
     */
    private Map<String, BackfillSourceConfig> sources = new HashMap<>();

    /**
     * Gets the effective max-per-day limit for a source.
     * Returns the source-specific value if configured, otherwise falls back to the
     * global default.
     *
     * @param source the source identifier
     * @return the max number of comics to backfill per day for this source
     */
    public int getMaxPerDayForSource(String source) {
        return Optional.ofNullable(sources.get(source))
                .map(BackfillSourceConfig::getMaxPerDay)
                .filter(max -> max != null && max > 0)
                .orElse(defaultMaxPerDay);
    }

    /**
     * Gets the effective max-days-back limit for a source.
     * Returns the source-specific value if configured, otherwise falls back to the
     * global default.
     *
     * @param source the source identifier
     * @return the max number of days back this source allows
     */
    public int getMaxDaysBackForSource(String source) {
        return Optional.ofNullable(sources.get(source))
                .map(BackfillSourceConfig::getMaxDaysBack)
                .filter(max -> max != null && max > 0)
                .orElse(defaultMaxDaysBack);
    }

    /**
     * Gets the earliest date allowed for backfill for a source.
     * Calculated as today minus the source's max-days-back limit.
     *
     * @param source the source identifier
     * @return the earliest date we can backfill for this source
     */
    public LocalDate getEarliestAllowedDate(String source) {
        int maxDaysBack = getMaxDaysBackForSource(source);
        return LocalDate.now().minusDays(maxDaysBack);
    }

    /**
     * Checks if a source is enabled for backfill.
     * Returns true if the source has no explicit config (defaults to enabled).
     *
     * @param source the source identifier
     * @return true if backfill is enabled for this source
     */
    public boolean isSourceEnabled(String source) {
        return Optional.ofNullable(sources.get(source))
                .map(BackfillSourceConfig::isEnabled)
                .orElse(true);
    }
}
