package org.stapledon.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "batch.comic-download")
public class DailyRunnerProperties {
    boolean enabled;
}
