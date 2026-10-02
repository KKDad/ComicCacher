package org.stapledon;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.stapledon.metrics.service.StorageMetricsWarmup;

/**
 * Starts the application with the startup work the other integration tests switch off, so a bean that only exists in production
 * still has to be created here. 2.6.2-rc1 failed to start on dev because {@link StorageMetricsWarmup} couldn't be instantiated.
 */
@SpringBootTest(properties = "comics.metrics.warm-on-startup=true")
@ActiveProfiles("integration")
class StartupBeansIT {

    @Autowired
    private ApplicationContext context;

    @Test
    void storageMetricsWarmupIsCreated() {
        assertThat(context.getBean(StorageMetricsWarmup.class)).isNotNull();
    }
}
