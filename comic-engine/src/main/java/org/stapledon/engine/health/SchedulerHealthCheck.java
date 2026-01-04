package org.stapledon.engine.health;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.stapledon.engine.batch.ComicBackfillJobScheduler;
import org.stapledon.engine.batch.ComicDownloadJobScheduler;
import org.stapledon.engine.batch.ImageMetadataJobScheduler;
import org.stapledon.engine.batch.MetricsArchiveJobScheduler;
import org.stapledon.engine.batch.MetricsUpdateJobScheduler;
import org.stapledon.engine.batch.RetrievalRecordPurgeJobScheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Health check component to verify all scheduler beans are loaded correctly.
 * Provides diagnostic information during application startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerHealthCheck {

    private final ApplicationContext context;

    /**
     * Verify all scheduler beans exist at startup
     */
    @PostConstruct
    public void verifySchedulers() {
        log.warn("======== VERIFYING SCHEDULER BEANS ========");

        checkBean("comicDownloadJobScheduler", ComicDownloadJobScheduler.class);
        checkBean("comicBackfillJobScheduler", ComicBackfillJobScheduler.class);
        checkBean("imageMetadataJobScheduler", ImageMetadataJobScheduler.class);
        checkBean("metricsArchiveJobScheduler", MetricsArchiveJobScheduler.class);
        checkBean("metricsUpdateJobScheduler", MetricsUpdateJobScheduler.class);
        checkBean("retrievalRecordPurgeJobScheduler", RetrievalRecordPurgeJobScheduler.class);

        log.warn("======== SCHEDULER VERIFICATION COMPLETE ========");
    }

    /**
     * Check if a specific bean exists and log the result
     */
    private void checkBean(String beanName, Class<?> clazz) {
        if (context.containsBean(beanName)) {
            Object bean = context.getBean(beanName);
            log.warn("✓ {} bean exists (class: {})", clazz.getSimpleName(), bean.getClass().getName());
        } else {
            log.error("✗ {} bean MISSING - Expected bean name: {}", clazz.getSimpleName(), beanName);
        }
    }
}
