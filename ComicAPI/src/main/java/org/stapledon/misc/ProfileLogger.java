package org.stapledon.misc;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(value = "logging.profile", havingValue = "true", matchIfMissing = true)
public class ProfileLogger {

    @PostConstruct
    public void setup() {
        // Log the active application.properties file
        String activeProfile = System.getProperty("spring.profiles.active", "default");
        log.info("Using application-{}.properties", activeProfile);
    }
}
