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

/**
 * Container for all access metrics data.
 * This is persisted to access-metrics.json for durability.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class AccessMetricsData {
    private LocalDateTime lastUpdated;

    @Builder.Default
    private Map<String, ComicAccessMetrics> comicMetrics = new HashMap<>();

    /**
     * Individual comic access metrics
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Builder
    public static class ComicAccessMetrics {
        private String comicName;

        @Builder.Default
        private int accessCount = 0;

        @Builder.Default
        private String lastAccess = "";

        @Builder.Default
        private long totalAccessTimeMs = 0;

        @Builder.Default
        private int cacheHits = 0;

        @Builder.Default
        private int cacheMisses = 0;

        /**
         * Calculate average access time
         *
         * @return Average access time in milliseconds
         */
        public double getAverageAccessTime() {
            return accessCount > 0 ? (double) totalAccessTimeMs / accessCount : 0.0;
        }

        /**
         * Calculate hit ratio
         *
         * @return Hit ratio as a value between 0.0 and 1.0
         */
        public double getHitRatio() {
            int total = cacheHits + cacheMisses;
            return total > 0 ? (double) cacheHits / total : 0.0;
        }
    }
}
