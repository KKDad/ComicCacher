package org.stapledon.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "daily.comics.startup")
public class StartupConfig {
    boolean enabled;
    boolean downloadOnStartup;
}