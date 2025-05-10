package org.stapledon.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

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
}
