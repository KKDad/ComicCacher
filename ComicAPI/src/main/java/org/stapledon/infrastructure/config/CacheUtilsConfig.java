package org.stapledon.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stapledon.infrastructure.caching.CacheUtils;

/**
 * Configuration for CacheUtils dependency
 */
@Configuration
public class CacheUtilsConfig {
    
    /**
     * Creates a CacheUtils bean for dependency injection
     * 
     * @param cacheConfiguration The cache configuration properties
     * @return Configured CacheUtils instance
     */
    @Bean
    public CacheUtils cacheUtils(CacheConfiguration cacheConfiguration) {
        return new CacheUtils(cacheConfiguration.cacheLocation());
    }
}