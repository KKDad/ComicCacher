package org.stapledon.api.dto.health;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for cache status information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CacheStatus {

    /**
     * Total comics cached
     */
    private int totalComics;

    /**
     * Total cached images
     */
    private int totalImages;

    /**
     * Total storage used in bytes
     */
    private long totalStorageBytes;

    /**
     * Age of oldest image in cache
     */
    private String oldestImage;

    /**
     * Age of newest image in cache
     */
    private String newestImage;

    /**
     * Directory where cache is stored
     */
    private String cacheLocation;
}