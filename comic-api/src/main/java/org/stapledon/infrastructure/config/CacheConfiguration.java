package org.stapledon.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stapledon.common.config.CacheProperties;

import java.io.File;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for cache directory setup.
 * Note: Metrics-related beans have been moved to MetricsConfiguration.
 */
@Slf4j
@ToString
@Configuration
@RequiredArgsConstructor
public class CacheConfiguration {

    private final CacheProperties cacheProperties;

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

    /**
     * Normalizes a path for the current OS to prevent issues like "C:/" on
     * non-Windows systems.
     * <p>
     * OS Detection:
     * - Windows: os.name contains "win" (e.g., "Windows 10", "Windows 11")
     * - macOS: os.name contains "mac" (e.g., "Mac OS X")
     * - Linux: os.name contains "nux" or "nix" (e.g., "Linux")
     * </p>
     */
    private String normalizePathForOS(String path) {
        String osName = System.getProperty("os.name").toLowerCase();

        boolean isWindows = osName.contains("win");
        boolean isMacOS = osName.contains("mac");
        boolean isLinux = osName.contains("nux") || osName.contains("nix");

        // Check if path is a Windows-style path (starts with drive letter like C:)
        boolean isWindowsStylePath = path.matches("^[A-Za-z]:.*");

        if (isWindows) {
            // On Windows, accept Windows-style paths as-is
            if (isWindowsStylePath) {
                return path;
            }
            // If no drive letter on Windows, use user home
            return Paths.get(System.getProperty("user.home"), "comics").toString();
        }

        if (isMacOS || isLinux) {
            // On macOS or Linux, convert Windows-style paths to user home
            if (isWindowsStylePath) {
                log.info("Converting Windows-style path '{}' to user home on {}", path, osName);
                return Paths.get(System.getProperty("user.home"), "comics").toString();
            }
            // Use the configured path as-is for Unix-style paths
            return path;
        }

        // For any other OS (fallback), handle Windows-style paths
        if (isWindowsStylePath) {
            log.warn("Unknown OS '{}', converting Windows-style path to user home", osName);
            return Paths.get(System.getProperty("user.home"), "comics").toString();
        }

        return path;
    }

    @Bean(name = "configName")
    public String configName() {
        return cacheProperties.getConfig();
    }
}