package org.stapledon.api.service;

import org.springframework.stereotype.Service;
import org.stapledon.api.dto.comic.ImageCacheStats;
import org.stapledon.api.dto.health.BuildInfo;
import org.stapledon.api.dto.health.CacheStatus;
import org.stapledon.api.dto.health.ComponentHealth;
import org.stapledon.api.dto.health.HealthStatus;
import org.stapledon.api.dto.health.SystemResources;
import org.stapledon.metrics.collector.AccessMetricsCollector;
import org.stapledon.metrics.collector.StorageMetricsCollector;
import org.stapledon.infrastructure.config.BuildVersion;
import org.stapledon.infrastructure.config.properties.CacheProperties;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the health service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthServiceImpl implements HealthService {

    private final BuildVersion buildVersion;
    private final StorageMetricsCollector cacheStatsUpdater;
    private final AccessMetricsCollector accessMetricsCollector;
    private final CacheProperties cacheProperties;
    
    private final long startTime = System.currentTimeMillis();
    
    @Override
    public HealthStatus getHealthStatus() {
        // Basic health status without detailed metrics
        return HealthStatus.builder()
                .status(determineStatus())
                .timestamp(LocalDateTime.now())
                .uptime(getUptime())
                .buildInfo(getBuildInfo())
                .build();
    }
    
    @Override
    public HealthStatus getDetailedHealthStatus() {
        HealthStatus.Status overallStatus = determineStatus();
        
        return HealthStatus.builder()
                .status(overallStatus)
                .timestamp(LocalDateTime.now())
                .uptime(getUptime())
                .buildInfo(getBuildInfo())
                .systemResources(getSystemResources())
                .cacheStatus(getCacheStatus())
                .components(getComponentsHealth())
                .build();
    }
    
    private long getUptime() {
        return System.currentTimeMillis() - startTime;
    }
    
    private BuildInfo getBuildInfo() {
        return BuildInfo.builder()
                .name(buildVersion.getBuildProperty("build.name"))
                .artifact(buildVersion.getBuildProperty("build.artifact"))
                .group(buildVersion.getBuildProperty("build.group"))
                .version(buildVersion.getBuildProperty("build.version"))
                .buildTime(buildVersion.getBuildProperty("build.time"))
                .javaVersion(System.getProperty("java.version"))
                .build();
    }
    
    private SystemResources getSystemResources() {
        // Memory info
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long totalMemory = memoryBean.getHeapMemoryUsage().getCommitted() / (1024 * 1024);
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long freeMemory = totalMemory - usedMemory;
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
        double memoryUsedPercentage = ((double) usedMemory / totalMemory) * 100;
        
        SystemResources.MemoryInfo memoryInfo = SystemResources.MemoryInfo.builder()
                .totalMemory(totalMemory)
                .freeMemory(freeMemory)
                .maxMemory(maxMemory)
                .usedPercentage(memoryUsedPercentage)
                .build();
        
        // Disk space
        String cacheLocation = cacheProperties.getLocation();
        File cacheDir = new File(cacheLocation);
        
        long totalSpace = cacheDir.getTotalSpace() / (1024 * 1024);
        long freeSpace = cacheDir.getFreeSpace() / (1024 * 1024);
        long usableSpace = cacheDir.getUsableSpace() / (1024 * 1024);
        double diskUsedPercentage = ((double) (totalSpace - freeSpace) / totalSpace) * 100;
        
        SystemResources.DiskSpace diskSpace = SystemResources.DiskSpace.builder()
                .total(totalSpace)
                .free(freeSpace)
                .usable(usableSpace)
                .usedPercentage(diskUsedPercentage)
                .build();
        
        return SystemResources.builder()
                .availableProcessors(Runtime.getRuntime().availableProcessors())
                .memory(memoryInfo)
                .diskSpace(diskSpace)
                .build();
    }
    
    private CacheStatus getCacheStatus() {
        ImageCacheStats cacheStats = cacheStatsUpdater.cacheStats();
        
        return CacheStatus.builder()
                .totalComics(cacheStats.getPerComicMetrics() != null ? cacheStats.getPerComicMetrics().size() : 0)
                .totalImages(cacheStats.getPerComicMetrics() != null ? 
                        cacheStats.getPerComicMetrics().values().stream()
                                .mapToInt(metric -> metric.getImageCount())
                                .sum() : 0)
                .totalStorageBytes(cacheStats.getTotalStorageBytes())
                .oldestImage(cacheStats.getOldestImage())
                .newestImage(cacheStats.getNewestImage())
                .cacheLocation(cacheProperties.getLocation())
                .build();
    }
    
    private Map<String, ComponentHealth> getComponentsHealth() {
        Map<String, ComponentHealth> components = new HashMap<>();
        
        // Cache component
        ComponentHealth cacheHealth = checkCacheHealth();
        components.put("cache", cacheHealth);
        
        // Additional components can be added here
        
        return components;
    }
    
    private ComponentHealth checkCacheHealth() {
        try {
            File cacheDir = new File(cacheProperties.getLocation());
            
            // Check if cache directory exists and is writable
            boolean cacheAccessible = cacheDir.exists() && cacheDir.isDirectory() && cacheDir.canWrite();
            
            // Check if sufficient disk space (at least 100MB free)
            boolean sufficientSpace = cacheDir.getUsableSpace() > 100 * 1024 * 1024;
            
            HealthStatus.Status status = HealthStatus.Status.UP;
            String message = "Cache is functioning normally";
            
            if (!cacheAccessible) {
                status = HealthStatus.Status.DOWN;
                message = "Cache directory is not accessible or writable";
            } else if (!sufficientSpace) {
                status = HealthStatus.Status.DEGRADED;
                message = "Cache directory is running low on disk space";
            }
            
            Map<String, Object> details = new HashMap<>();
            details.put("accessible", cacheAccessible);
            details.put("sufficientSpace", sufficientSpace);
            details.put("path", cacheProperties.getLocation());
            
            return ComponentHealth.builder()
                    .status(status)
                    .message(message)
                    .details(details)
                    .build();
        } catch (Exception e) {
            log.error("Error checking cache health", e);
            return ComponentHealth.builder()
                    .status(HealthStatus.Status.DOWN)
                    .message("Error checking cache health: " + e.getMessage())
                    .build();
        }
    }
    
    private HealthStatus.Status determineStatus() {
        // Determine overall status based on component checks
        try {
            // Start with UP status
            HealthStatus.Status status = HealthStatus.Status.UP;
            
            // Check cache health
            ComponentHealth cacheHealth = checkCacheHealth();
            
            // If any critical component is DOWN, overall status is DOWN
            if (cacheHealth.getStatus() == HealthStatus.Status.DOWN) {
                return HealthStatus.Status.DOWN;
            }
            
            // If any component is DEGRADED, overall status is DEGRADED
            if (cacheHealth.getStatus() == HealthStatus.Status.DEGRADED) {
                return HealthStatus.Status.DEGRADED;
            }
            
            return status;
        } catch (Exception e) {
            log.error("Error determining health status", e);
            return HealthStatus.Status.DOWN;
        }
    }
}