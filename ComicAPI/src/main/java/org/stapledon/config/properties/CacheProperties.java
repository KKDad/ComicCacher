package org.stapledon.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "comics.cache")
public class CacheProperties {
    private String location;

    private String config;

    private String usersConfig;

    private String preferencesConfig;
}