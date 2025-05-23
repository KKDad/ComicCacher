package org.stapledon.api.dto.comic;

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
}
