package org.stapledon.metrics.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Container for pre-computed combined metrics.
 * This is persisted to combined-metrics.json and served by the controller.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class CombinedMetricsData {
    @ToString.Include
    private LocalDateTime lastUpdated;

    /**
     * Global aggregate metrics across all comics.
     */
    @ToString.Include
    private GlobalMetrics globalMetrics;

    /**
     * Per-comic combined metrics.
     */
    @ToString.Include
    @Builder.Default
    private Map<String, ComicCombinedMetrics> perComicMetrics = new HashMap<>();

    /**
     * Combined storage and access metrics for a single comic
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Builder
    @ToString(onlyExplicitlyIncluded = true)
    public static class ComicCombinedMetrics {
        @ToString.Include
        private String comicName;

        // Storage metrics
        @Builder.Default
        private long storageBytes = 0;

        @Builder.Default
        private int imageCount = 0;

        @Builder.Default
        private double averageImageSize = 0.0;

        /**
         * Yearly storage breakdown with bytes and image count per year.
         */
        @Builder.Default
        private Map<String, YearlyStorageMetrics> yearlyStorage = new HashMap<>();

        // Access metrics
        @Builder.Default
        private int accessCount = 0;

        @Builder.Default
        private String lastAccess = "";

        @Builder.Default
        private double averageAccessTime = 0.0;

        @Builder.Default
        private double hitRatio = 0.0;

        @Builder.Default
        private int cacheHits = 0;

        @Builder.Default
        private int cacheMisses = 0;
    }
}
