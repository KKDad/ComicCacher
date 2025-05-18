package org.stapledon.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "startup.reconcile")
public class StartupReconcilerProperties {
    boolean enabled;
    String scheduleTime = "06:00:00";
}