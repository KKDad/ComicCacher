package org.stapledon.metrics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
@ToString(onlyExplicitlyIncluded = true)
public class YearlyStorageMetrics {

    /**
     * Storage bytes for this year.
     */
    @ToString.Include
    @Builder.Default
    private long storageBytes = 0;

    /**
     * Image count for this year.
     */
    @ToString.Include
    @Builder.Default
    private int imageCount = 0;
}
