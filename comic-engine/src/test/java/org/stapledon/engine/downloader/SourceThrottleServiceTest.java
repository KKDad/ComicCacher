package org.stapledon.engine.downloader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.stapledon.common.config.properties.DownloaderProperties;


class SourceThrottleServiceTest {

    @Test
    void await_whenSourceHasNoConfig_returnsImmediately() throws Exception {
        SourceThrottleService service = new SourceThrottleService(new DownloaderProperties());

        long elapsed = timeMs(() -> service.await("unknown"));

        assertThat(elapsed).isLessThan(50);
    }

    @Test
    void await_whenMaxIsZero_returnsImmediately() throws Exception {
        SourceThrottleService service = new SourceThrottleService(propertiesFor("test", 0, 0));

        long elapsed = timeMs(() -> service.await("test"));

        assertThat(elapsed).isLessThan(50);
    }

    @Test
    void await_secondCallSameSource_blocksUntilDelayElapses() throws Exception {
        long min = 100;
        long max = 100;
        SourceThrottleService service = new SourceThrottleService(propertiesFor("test", min, max));

        service.await("test");
        long elapsed = timeMs(() -> service.await("test"));

        assertThat(elapsed).isGreaterThanOrEqualTo(min - 20);
        assertThat(elapsed).isLessThan(min + 200);
    }

    @Test
    void await_concurrentCallsForDifferentSources_doNotBlockEachOther() throws Exception {
        DownloaderProperties props = new DownloaderProperties();
        DownloaderProperties.Source slow = new DownloaderProperties.Source();
        slow.getThrottle().setMinDelayMs(500);
        slow.getThrottle().setMaxDelayMs(500);
        DownloaderProperties.Source fast = new DownloaderProperties.Source();
        fast.getThrottle().setMinDelayMs(0);
        fast.getThrottle().setMaxDelayMs(0);
        props.setSources(Map.of("slow", slow, "fast", fast));

        SourceThrottleService service = new SourceThrottleService(props);

        // Prime the slow source so the next slow call has to wait 500ms.
        service.await("slow");

        ExecutorService exec = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            long t0 = System.currentTimeMillis();
            var fastTask = exec.submit(() -> {
                start.await();
                service.await("fast");
                return System.currentTimeMillis() - t0;
            });
            var slowTask = exec.submit(() -> {
                start.await();
                service.await("slow");
                return System.currentTimeMillis() - t0;
            });

            start.countDown();

            long fastDuration = fastTask.get(2, TimeUnit.SECONDS);
            long slowDuration = slowTask.get(2, TimeUnit.SECONDS);

            // Fast source thread is not blocked by the slow source's throttle.
            assertThat(fastDuration).isLessThan(200);
            // Slow source thread waits roughly the configured 500ms.
            assertThat(slowDuration).isGreaterThanOrEqualTo(400);
        } finally {
            exec.shutdownNow();
        }
    }

    @Test
    void await_jitterStaysWithinBounds() throws Exception {
        long min = 50;
        long max = 80;
        SourceThrottleService service = new SourceThrottleService(propertiesFor("jitter", min, max));

        // First call sets the baseline; subsequent calls measure jitter.
        service.await("jitter");

        for (int i = 0; i < 5; i++) {
            long elapsed = timeMs(() -> service.await("jitter"));
            assertThat(elapsed)
                    .as("attempt %d", i)
                    .isGreaterThanOrEqualTo(min - 20)
                    .isLessThan(max + 200);
        }
    }

    private static DownloaderProperties propertiesFor(String source, long minMs, long maxMs) {
        DownloaderProperties props = new DownloaderProperties();
        DownloaderProperties.Source cfg = new DownloaderProperties.Source();
        cfg.getThrottle().setMinDelayMs(minMs);
        cfg.getThrottle().setMaxDelayMs(maxMs);
        props.setSources(Map.of(source, cfg));
        return props;
    }

    private static long timeMs(ThrowingRunnable r) throws Exception {
        long t0 = System.currentTimeMillis();
        r.run();
        return System.currentTimeMillis() - t0;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
