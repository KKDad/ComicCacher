package org.stapledon.engine.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.stapledon.common.dto.BackfillSourceConfig;

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
@Getter
@ToString
@EqualsAndHashCode
@Builder
@AllArgsConstructor
@ConfigurationProperties(prefix = "batch.comic-backfill")
public class BackfillConfigurationService {

    /** Whether backfill is enabled globally. */
    private final boolean enabled;

    /**
     * Maximum consecutive failures before stopping scan for a comic.
     * This helps detect comics that don't exist as far back as we're scanning.
     */
    private final int maxConsecutiveFailures;

    /** Global default for max comics per day per source (can be overridden per source). */
    private final int defaultMaxPerDay;

    /** Global default for max days back (can be overridden per source). */
    private final int defaultMaxDaysBack;

    /**
     * Source-specific configurations.
     * Key is the source identifier (e.g., "gocomics", "comicskingdom").
     */
    private final Map<String, BackfillSourceConfig> sources;

    /**
     * Gets the effective max-per-day limit for a source.
     * Returns the source-specific value if configured, otherwise falls back to the
     * global default.
     */
    public int getMaxPerDayForSource(String source) {
        return Optional.ofNullable(sources)
                .map(s -> s.get(source))
                .map(BackfillSourceConfig::getMaxPerDay)
                .filter(max -> max != null && max > 0)
                .orElse(defaultMaxPerDay);
    }

    /**
     * Gets the effective max-days-back limit for a source.
     */
    public int getMaxDaysBackForSource(String source) {
        return Optional.ofNullable(sources)
                .map(s -> s.get(source))
                .map(BackfillSourceConfig::getMaxDaysBack)
                .filter(max -> max != null && max > 0)
                .orElse(defaultMaxDaysBack);
    }

    /**
     * Gets the earliest date allowed for backfill for a source.
     */
    public LocalDate getEarliestAllowedDate(String source) {
        return LocalDate.now().minusDays(getMaxDaysBackForSource(source));
    }

    /**
     * Checks if a source is enabled for backfill. True if no explicit config (defaults to enabled).
     */
    public boolean isSourceEnabled(String source) {
        return Optional.ofNullable(sources)
                .map(s -> s.get(source))
                .map(BackfillSourceConfig::isEnabled)
                .orElse(true);
    }

    /**
     * Checks if a source prefers color strips over grayscale. True if no explicit config.
     */
    public boolean getPreferColorForSource(String source) {
        return Optional.ofNullable(sources)
                .map(s -> s.get(source))
                .map(BackfillSourceConfig::isPreferColor)
                .orElse(true);
    }
}
