package org.stapledon.metrics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Storage metrics for a specific year within a comic.
 * Used for detailed per-year breakdown in combined metrics.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class YearlyStorageMetrics {

    /**
     * Storage bytes for this year.
     */
    @Builder.Default
    private long storageBytes = 0;

    /**
     * Image count for this year.
     */
    @Builder.Default
    private int imageCount = 0;
}
