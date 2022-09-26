package org.stapledon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfiguration {

    @Value("{comics.cache.location:/comics}")
    private String location;

    @Bean(name = "cacheLocation")
    public String cacheLocation() {
        return location;
    }
}
