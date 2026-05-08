package org.stapledon.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.stapledon.common.dto.HashAlgorithm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ToString
@Builder
@AllArgsConstructor
@ConfigurationProperties(prefix = "comics.cache")
public class CacheProperties {
    private final String location;

    private final String config;

    private final String usersConfig;

    private final String preferencesConfig;

    /**
     * Whether to run Chrome in headless mode (without GUI).
     * Default is true for better performance and CI/CD compatibility.
     */
    private final boolean chromeHeadless;

    /**
     * Whether duplicate image detection is enabled.
     * When enabled, prevents saving the same comic strip multiple times within the same year.
     */
    private final boolean duplicateDetectionEnabled;

    /**
     * The hash algorithm to use for duplicate detection.
     * DIFFERENCE_HASH (default) - Fast perceptual hash, detects visually similar images.
     * AVERAGE_HASH - Simpler perceptual hash, faster but less accurate.
     * MD5 - Fast byte-exact matching, won't catch re-encoded duplicates.
     * SHA256 - Secure byte-exact matching, slower than MD5.
     */
    private final HashAlgorithm hashAlgorithm;
}
