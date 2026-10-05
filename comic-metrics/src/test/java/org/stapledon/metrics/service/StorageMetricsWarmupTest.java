package org.stapledon.metrics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.stapledon.metrics.collector.StorageMetricsCollector;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StorageMetricsWarmupTest {

    @Test
    void springCanCreateTheBean() {
        new ApplicationContextRunner()
                .withBean(StorageMetricsCollector.class, () -> mock(StorageMetricsCollector.class))
                .withBean(StorageMetricsWarmup.class)
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(StorageMetricsWarmup.class));
    }

    @Test
    void scansInTheBackgroundWhenTheApplicationIsReady() {
        StorageMetricsCollector collector = mock(StorageMetricsCollector.class);
        List<Runnable> queued = new ArrayList<>();
        StorageMetricsWarmup warmup = new StorageMetricsWarmup(collector, queued::add);

        warmup.onApplicationReady();

        verify(collector, never()).updateStats();
        assertThat(queued).hasSize(1);
        queued.getFirst().run();
        verify(collector).updateStats();
    }

    @Test
    void aFailedScanDoesNotEscapeTheBackgroundThread() {
        StorageMetricsCollector collector = mock(StorageMetricsCollector.class);
        when(collector.updateStats()).thenThrow(new IllegalStateException("NFS gone"));
        StorageMetricsWarmup warmup = new StorageMetricsWarmup(collector, Runnable::run);

        warmup.onApplicationReady();

        verify(collector).updateStats();
    }
}
