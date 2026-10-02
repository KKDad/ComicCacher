package org.stapledon.metrics.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.stapledon.metrics.collector.StorageMetricsCollector;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs the first storage metrics scan in the background once the application is ready, so the first metrics request doesn't wait
 * for it. On the NFS cache the scan takes the better part of a minute; until it finishes, {@link MetricsUpdateService} serves the
 * latest daily snapshot.
 *
 * <p>
 * Set {@code comics.metrics.warm-on-startup=false} to skip it (the integration tests do, so a background scan can't race them).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "comics.metrics", name = {"enabled", "warm-on-startup"}, havingValue = "true", matchIfMissing = true)
public class StorageMetricsWarmup {

    private final StorageMetricsCollector storageMetricsCollector;
    private final Executor executor;

    @Autowired
    public StorageMetricsWarmup(StorageMetricsCollector storageMetricsCollector) {
        this(storageMetricsCollector, task -> Thread.ofVirtual().name("metrics-warmup").start(task));
    }

    StorageMetricsWarmup(StorageMetricsCollector storageMetricsCollector, Executor executor) {
        this.storageMetricsCollector = storageMetricsCollector;
        this.executor = executor;
    }

    /**
     * Starts the scan in the background and returns straight away.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        executor.execute(() -> {
            try {
                storageMetricsCollector.updateStats();
            } catch (RuntimeException e) {
                log.error("Startup storage metrics scan failed", e);
            }
        });
    }
}
