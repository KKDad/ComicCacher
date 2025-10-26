package org.stapledon.engine.validation;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.HashAlgorithm;
import org.stapledon.common.service.ImageHasher;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating the appropriate ImageHasher implementation based on configuration.
 * Uses Spring's bean injection to select the correct hasher algorithm at runtime.
 */
@Slf4j
@ToString
@Component
@RequiredArgsConstructor
public class ImageHasherFactory {

    private final CacheProperties cacheProperties;

    @Qualifier("md5ImageHasher")
    private final ImageHasher md5ImageHasher;

    @Qualifier("sha256ImageHasher")
    private final ImageHasher sha256ImageHasher;

    @Qualifier("averageImageHasher")
    private final ImageHasher averageImageHasher;

    @Qualifier("differenceImageHasher")
    private final ImageHasher differenceImageHasher;

    /**
     * Gets the configured ImageHasher implementation based on the hash algorithm setting.
     *
     * @return The appropriate ImageHasher implementation
     */
    public ImageHasher getImageHasher() {
        HashAlgorithm algorithm = cacheProperties.getHashAlgorithm();

        log.debug("Selecting ImageHasher for algorithm: {}", algorithm);

        return switch (algorithm) {
            case MD5 -> md5ImageHasher;
            case SHA256 -> sha256ImageHasher;
            case AVERAGE_HASH -> averageImageHasher;
            case DIFFERENCE_HASH -> differenceImageHasher;
        };
    }
}
