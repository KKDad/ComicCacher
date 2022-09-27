package org.stapledon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfiguration {

    @Value("${comics.cache.location:/comics}")
    private String location;

    @Value("${comics.config:comics.json}")
    private String comicsJson;

    @Bean(name = "cacheLocation")
    public String cacheLocation() {
        return location;
    }

    @Bean(name = "configName")
    public String configName() {
        return comicsJson;
    }
}
