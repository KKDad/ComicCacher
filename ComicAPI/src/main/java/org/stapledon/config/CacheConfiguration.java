package org.stapledon.config;

import com.google.j2objc.annotations.J2ObjCIncompatible;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.stapledon.config.properties.CacheProperties;

import java.io.File;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheConfiguration {
    private final CacheProperties cacheProperties;

    @Bean(name = "cacheLocation")
    public String cacheLocation() {
        var directory = new File(cacheProperties.getLocation());
        if (!directory.exists() || directory.isDirectory()) {
            directory.mkdirs();
        }
        log.warn("Serving from {}", cacheProperties.getLocation());
        return cacheProperties.getLocation();
    }

    @Bean(name = "configName")
    public String configName() {
        return cacheProperties.getConfig();
    }
}
