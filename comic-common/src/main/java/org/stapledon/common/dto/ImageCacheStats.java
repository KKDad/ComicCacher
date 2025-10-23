package org.stapledon.common.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
@Builder
public class ImageCacheStats {
    private String oldestImage;
    private String newestImage;
    private List<String> years;

    // New fields for enhanced metrics
    private long totalStorageBytes;
    private Map<String, ComicStorageMetrics> perComicMetrics;

    // Year-based aggregated metrics
    private Map<String, Integer> imageCountByYear;
    private Map<String, Long> storageBytesByYear;
}
