package org.stapledon.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.infrastructure.web.JsoupInspectorService;

/**
 * Configuration for InspectorService dependency
 */
@Configuration
public class InspectorServiceConfig {

    /**
     * Creates a InspectorService bean for dependency injection
     *
     * @return Configured InspectorService instance
     */
    @Bean
    public InspectorService webInspector() {
        return new JsoupInspectorService();
    }
}