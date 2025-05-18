package org.stapledon.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stapledon.infrastructure.web.WebInspector;
import org.stapledon.infrastructure.web.WebInspectorImpl;

/**
 * Configuration for WebInspector dependency
 */
@Configuration
public class WebInspectorConfig {
    
    /**
     * Creates a WebInspector bean for dependency injection
     * 
     * @return Configured WebInspector instance
     */
    @Bean
    public WebInspector webInspector() {
        return new WebInspectorImpl();
    }
}