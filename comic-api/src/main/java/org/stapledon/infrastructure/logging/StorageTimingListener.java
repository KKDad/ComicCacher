package org.stapledon.infrastructure.logging;

import org.springframework.stereotype.Component;
import org.stapledon.common.util.StorageTimings;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Receives the storage reads reported through {@link StorageTimings}: adds each to the current request's {@link RequestTimings} and logs a
 * WARN for a read at or above {@code comics.timing.slow-storage-ms}, wherever it happens (requests and batch jobs alike).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageTimingListener {

    private final TimingProperties properties;

    @PostConstruct
    void install() {
        StorageTimings.setListener(this::onRead);
    }

    @PreDestroy
    void uninstall() {
        StorageTimings.setListener(null);
    }

    void onRead(String what, long nanos) {
        long ms = RequestTimings.millis(nanos);
        if (ms >= properties.slowStorageMs()) {
            log.warn("Slow storage read {} took {}ms", what, ms);
        }
        RequestTimings timings = RequestTimings.current();
        if (timings != null) {
            timings.recordStorage(nanos);
        }
    }
}
