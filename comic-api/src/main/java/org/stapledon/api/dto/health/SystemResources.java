package org.stapledon.api.dto.health;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for system resource information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemResources {
    
    /**
     * Available processors
     */
    private int availableProcessors;
    
    /**
     * Memory usage in MB
     */
    private MemoryInfo memory;
    
    /**
     * Disk space information in MB
     */
    private DiskSpace diskSpace;
    
    /**
     * Memory metrics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryInfo {
        private long totalMemory;
        private long freeMemory;
        private long maxMemory;
        private double usedPercentage;
    }
    
    /**
     * Disk space metrics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiskSpace {
        private long total;
        private long free;
        private long usable;
        private double usedPercentage;
    }
}