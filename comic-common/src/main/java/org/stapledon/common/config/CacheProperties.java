package org.stapledon.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.stapledon.common.dto.HashAlgorithm;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "comics.cache")
public class CacheProperties {
    private String location;

    private String config;

    private String usersConfig;

    private String preferencesConfig;

    /**
     * Whether to run Chrome in headless mode (without GUI).
     * Default is true for better performance and CI/CD compatibility.
     */
    private boolean chromeHeadless = true;

    /**
     * Whether duplicate image detection is enabled.
     * When enabled, prevents saving the same comic strip multiple times within the same year.
     * Default is true.
     */
    private boolean duplicateDetectionEnabled = true;

    /**
     * The hash algorithm to use for duplicate detection.
     * DIFFERENCE_HASH (default) - Fast perceptual hash, detects visually similar images.
     * AVERAGE_HASH - Simpler perceptual hash, faster but less accurate.
     * MD5 - Fast byte-exact matching, won't catch re-encoded duplicates.
     * SHA256 - Secure byte-exact matching, slower than MD5.
     */
    private HashAlgorithm hashAlgorithm = HashAlgorithm.DIFFERENCE_HASH;
}