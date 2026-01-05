package org.stapledon.metrics.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Aggregate metrics across all comics in the cache.
 * Provides a high-level overview of the entire comic collection.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class GlobalMetrics {

    /**
     * Path to the oldest cached comic image.
     */
    private String oldestImage;

    /**
     * Path to the newest cached comic image.
     */
    private String newestImage;

    /**
     * List of all years that have cached content.
     */
    private List<String> years;

    /**
     * Total storage bytes across all comics.
     */
    @Builder.Default
    private long totalStorageBytes = 0;

    /**
     * Total image count across all comics.
     */
    @Builder.Default
    private int totalImageCount = 0;

    /**
     * Storage bytes aggregated by year.
     */
    private Map<String, Long> storageByYear;

    /**
     * Image count aggregated by year.
     */
    private Map<String, Integer> imageCountByYear;
}
