package org.stapledon.infrastructure.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration for CacheUtils dependency
 * Note: The actual CacheUtils bean is defined in CacheConfiguration
 */
@Configuration
public class CacheUtilsConfig {
    // The CacheUtils bean is now defined in CacheConfiguration with @Primary
}