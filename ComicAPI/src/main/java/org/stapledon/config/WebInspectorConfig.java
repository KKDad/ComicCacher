package org.stapledon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stapledon.web.WebInspector;
import org.stapledon.web.WebInspectorImpl;

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