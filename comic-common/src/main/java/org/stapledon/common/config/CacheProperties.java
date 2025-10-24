package org.stapledon.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "comics.cache")
public class CacheProperties {
    private String location;

    private String config;

    private String usersConfig;

    private String preferencesConfig;

    /**
     * Whether to run Chrome in headless mode (without GUI).
     * Default is true for better performance and CI/CD compatibility.
     */
    private boolean chromeHeadless = true;
}