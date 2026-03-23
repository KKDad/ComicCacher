package org.stapledon.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Configuration for backfill constraints specific to a comic source.
 * Different comic sources may have different rate limits and history depth.
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackfillSourceConfig {

    /**
     * Source identifier (e.g., "gocomics", "comicskingdom").
     */
    @ToString.Include
    private String source;

    /**
     * Maximum number of comics to backfill per day for this source.
     * Set to 0 or null for unlimited (uses global default).
     */
    private Integer maxPerDay;

    /**
     * Maximum number of days back this source allows.
     * Set to 0 or null for unlimited (uses global default).
     */
    private Integer maxDaysBack;

    /**
     * Whether this source is enabled for backfill at all.
     */
    @Builder.Default
    private Boolean enabled = true;

    /**
     * Whether to prefer color strips over grayscale for indexed comics.
     * When true (default), tries color first and falls back to grayscale.
     * When false, tries grayscale first and falls back to color.
     */
    @Builder.Default
    private Boolean preferColor = true;

    /**
     * Checks if this source is enabled for backfill.
     */
    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    /**
     * Checks if this source prefers color strips.
     */
    public boolean isPreferColor() {
        return preferColor == null || preferColor;
    }
}
