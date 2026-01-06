package org.stapledon.api.dto.health;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO for system resource information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString(onlyExplicitlyIncluded = true)
public class SystemResources {

    /**
     * Available processors
     */
    @ToString.Include
    private int availableProcessors;

    /**
     * Memory usage in MB
     */
    @ToString.Include
    private MemoryInfo memory;

    /**
     * Disk space information in MB
     */
    @ToString.Include
    private DiskSpace diskSpace;

    /**
     * Memory metrics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString(onlyExplicitlyIncluded = true)
    public static class MemoryInfo {
        @ToString.Include
        private long totalMemory;

        @ToString.Include
        private long freeMemory;

        private long maxMemory;

        @ToString.Include
        private double usedPercentage;
    }

    /**
     * Disk space metrics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString(onlyExplicitlyIncluded = true)
    public static class DiskSpace {
        @ToString.Include
        private long total;

        @ToString.Include
        private long free;

        private long usable;

        @ToString.Include
        private double usedPercentage;
    }
}