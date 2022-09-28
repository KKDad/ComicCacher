package org.stapledon.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
@Slf4j
public class CacheConfiguration {

    @Value("${comics.cache.location:/comics}")
    private String location;

    @Value("${comics.config:comics.json}")
    private String comicsJson;

    @Bean(name = "cacheLocation")
    public String cacheLocation() {
        var directory = new File(location);
        if (!directory.exists() || directory.isDirectory()) {
            directory.mkdirs();
        }
        log.warn("Serving from {}", location);
        return location;
    }

    @Bean(name = "configName")
    public String configName() {
        return comicsJson;
    }
}
