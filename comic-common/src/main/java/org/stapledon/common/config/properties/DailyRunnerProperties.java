package org.stapledon.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
@AllArgsConstructor
@ConfigurationProperties(prefix = "batch.comic-download")
public class DailyRunnerProperties {
    private final boolean enabled;
}
