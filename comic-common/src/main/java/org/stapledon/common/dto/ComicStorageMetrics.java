package org.stapledon.common.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores detailed metrics about a comic's storage usage and access patterns.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class ComicStorageMetrics {
    private String comicName;
    private long storageBytes;
    private int imageCount;
    private double averageImageSize;
    private String mostRecentAccess;
    private int accessCount;
    private double hitRatio;
    private Map<String, Long> storageByYear;
    private long downloadTime;
}