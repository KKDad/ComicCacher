package org.stapledon.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.stapledon.infrastructure.caching.CacheUtils;
import org.stapledon.infrastructure.config.properties.CacheProperties;
import org.stapledon.infrastructure.storage.ComicStorageFacade;

import java.io.File;
import java.nio.file.Paths;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CacheConfiguration {

    private final CacheProperties cacheProperties;
    private final ComicStorageFacade storageFacade;

    @Bean(name = "cacheLocation")
    public String cacheLocation() {
        String configuredLocation = cacheProperties.getLocation();
        
        // Normalize the path based on OS
        String normalizedPath = normalizePathForOS(configuredLocation);
        
        // Create the directory
        var directory = new File(normalizedPath);
        if (!directory.exists() || directory.isDirectory()) {
            directory.mkdirs();
        }
        
        log.warn("Serving from {}", normalizedPath);
        return normalizedPath;
    }
    
    @Bean
    @Primary
    public CacheUtils cacheUtils() {
        // Create the enhanced version that delegates to the storage facade
        return new CacheUtils(cacheLocation(), storageFacade);
    }
    
    /**
     * Normalizes a path for the current OS to prevent issues like "C:/" on non-Windows systems
     */
    private String normalizePathForOS(String path) {
        // Get OS name
        String os = System.getProperty("os.name").toLowerCase();
        
        // For Windows, use the path as-is if it's a valid Windows path
        if (os.contains("win")) {
            // If it's a valid Windows path, return it normalized
            if (path.matches("^[A-Za-z]:.*")) {
                return path;
            }
            // If no drive letter, create a path in user home
            return Paths.get(System.getProperty("user.home"), "comics").toString();
        }
        
        // For Unix-like systems (Linux, macOS), ensure the path doesn't contain Windows-specific formats
        if (path.matches("^[A-Za-z]:.*")) {
            // Windows-style path on non-Windows OS, create a proper path in user home
            return Paths.get(System.getProperty("user.home"), "comics").toString();
        }
        
        // For any other cases, use the path as configured
        return path;
    }

    @Bean(name = "configName")
    public String configName() {
        return cacheProperties.getConfig();
    }
}