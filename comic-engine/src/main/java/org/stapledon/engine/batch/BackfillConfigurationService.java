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
import java.util.function.Function;

import org.stapledon.common.dto.BackfillSourceConfig;

/**
 * Service for managing backfill configuration including source-specific limits.
 * <p>
 * This service provides centralized access to backfill configuration,
 * supporting:
 * <ul>
 * <li>Global default limits for strips per run, strips per day, the recent window and max days back</li>
 * <li>Source-specific overrides for individual comic sources</li>
 * <li>Per-source enable/disable control</li>
 * <li>Thresholds for giving up on dates and learning how far back a source serves strips</li>
 * </ul>
 * Unset (zero) numeric settings fall back to the built-in defaults below.
 *
 * <p>
 * Configuration example in application.properties:
 *
 * <pre>
 * batch.comic-backfill.default-max-per-run=30
 * batch.comic-backfill.default-recent-days=7
 * batch.comic-backfill.default-max-days-back=365
 * batch.comic-backfill.sources.gocomics.max-per-run=30
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

    static final int FALLBACK_MAX_PER_RUN = 30;
    static final int FALLBACK_RECENT_DAYS = 7;
    static final int FALLBACK_GIVE_UP_AFTER = 2;
    static final int FALLBACK_HORIZON_CONSECUTIVE_FAILURES = 3;
    static final int FALLBACK_HORIZON_MIN_COMICS = 3;
    static final int FALLBACK_HORIZON_TOLERANCE_DAYS = 2;
    static final int FALLBACK_RETRY_GIVEN_UP_AFTER_DAYS = 30;

    /** Whether backfill is enabled globally. */
    private final boolean enabled;

    /**
     * Maximum consecutive failures before stopping scan for a comic.
     * This helps detect comics that don't exist as far back as we're scanning.
     */
    private final int maxConsecutiveFailures;

    /** Global default for max strips per run per source (can be overridden per source). */
    private final int defaultMaxPerRun;

    /** Global default ceiling on strips per day per source across all runs; 0 means no ceiling. */
    private final int defaultMaxPerDay;

    /** Global default for how many recent days are backfilled first (can be overridden per source). */
    private final int defaultRecentDays;

    /** Global default for max days back (can be overridden per source). */
    private final int defaultMaxDaysBack;

    /** A date that comes back unavailable (or a duplicate of another date) this many times is skipped from then on. */
    private final int giveUpAfter;

    /** Unavailable results in a row, older than the recent window, before a comic's history horizon is set. */
    private final int horizonConsecutiveFailures;

    /** Comics on one source that must reach about the same horizon before it becomes the source's horizon. */
    private final int horizonMinComics;

    /** How close (in days) comic horizons must be to count as the same source horizon. */
    private final int horizonToleranceDays;

    /** Given-up dates and learned horizons are forgotten after this many days, so they get another try. */
    private final int retryGivenUpAfterDays;

    /**
     * Whether task selection remembers, for the rest of the day, which strips it found on disk, so repeated scans only recheck the gaps. It only steers which
     * dates are scanned, never which image is served. Off when unset.
     */
    private final boolean rememberCachedStrips;

    /**
     * Source-specific configurations.
     * Key is the source identifier (e.g., "gocomics", "comicskingdom").
     */
    private final Map<String, BackfillSourceConfig> sources;

    /**
     * Gets the effective max-per-run budget for a source.
     * Returns the source-specific value if configured, otherwise the global default, otherwise {@value #FALLBACK_MAX_PER_RUN}.
     */
    public int getMaxPerRunForSource(String source) {
        return sourceValue(source, BackfillSourceConfig::getMaxPerRun)
                .orElse(orFallback(defaultMaxPerRun, FALLBACK_MAX_PER_RUN));
    }

    /**
     * Gets the effective per-day ceiling for a source across all runs, or 0 when there is none.
     */
    public int getMaxPerDayForSource(String source) {
        return sourceValue(source, BackfillSourceConfig::getMaxPerDay)
                .orElse(Math.max(0, defaultMaxPerDay));
    }

    /**
     * Gets how many recent days are backfilled first for a source.
     */
    public int getRecentDaysForSource(String source) {
        return sourceValue(source, BackfillSourceConfig::getRecentDays)
                .orElse(orFallback(defaultRecentDays, FALLBACK_RECENT_DAYS));
    }

    /**
     * How many unavailable or duplicate results for one date make backfill give up on it.
     */
    public int getGiveUpAfter() {
        return orFallback(giveUpAfter, FALLBACK_GIVE_UP_AFTER);
    }

    /**
     * How many different old dates in a row must come back unavailable before a comic's history horizon is learned.
     */
    public int getHorizonConsecutiveFailures() {
        return orFallback(horizonConsecutiveFailures, FALLBACK_HORIZON_CONSECUTIVE_FAILURES);
    }

    /**
     * How many comics on a source must reach a horizon of about the same age before it becomes the source's horizon.
     */
    public int getHorizonMinComics() {
        return orFallback(horizonMinComics, FALLBACK_HORIZON_MIN_COMICS);
    }

    /**
     * How many days apart comic horizons may be and still count as the same source horizon.
     */
    public int getHorizonToleranceDays() {
        return orFallback(horizonToleranceDays, FALLBACK_HORIZON_TOLERANCE_DAYS);
    }

    /**
     * How many days a given-up date or learned horizon is remembered before backfill tries again.
     */
    public int getRetryGivenUpAfterDays() {
        return orFallback(retryGivenUpAfterDays, FALLBACK_RETRY_GIVEN_UP_AFTER_DAYS);
    }

    private Optional<Integer> sourceValue(String source, Function<BackfillSourceConfig, Integer> getter) {
        return Optional.ofNullable(sources)
                .map(s -> s.get(source))
                .map(getter)
                .filter(value -> value > 0);
    }

    private static int orFallback(int value, int fallback) {
        return value > 0 ? value : fallback;
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
