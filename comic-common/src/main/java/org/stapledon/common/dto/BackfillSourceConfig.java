package org.stapledon.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for backfill constraints specific to a comic source.
 * Different comic sources may have different rate limits and history depth.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackfillSourceConfig {

    /**
     * Source identifier (e.g., "gocomics", "comicskingdom").
     */
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
     * Checks if this source is enabled for backfill.
     *
     * @return true if enabled, false if disabled
     */
    public boolean isEnabled() {
        return enabled == null || enabled;
    }
}
