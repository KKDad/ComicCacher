package org.stapledon.engine.downloader;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

import org.stapledon.common.config.properties.DownloaderProperties;


/**
 * Per-source request pacing for outbound HTTP downloaders. Each call to {@link #await(String)} blocks the caller until enough time has elapsed since the previous call for the same source,
 * with randomized jitter between {@code min-delay-ms} and {@code max-delay-ms}.
 *
 * <p>Throttle state is per-source and concurrent: a slow source (e.g. GoComics) does not block requests against other sources running on different threads.
 *
 * <p>Sources with no throttle configuration (or zero delay) return immediately.
 */
@Slf4j
@ToString
@Service
public class SourceThrottleService {

    private final DownloaderProperties properties;
    private final ConcurrentMap<String, Long> nextAllowedAt = new ConcurrentHashMap<>();

    public SourceThrottleService(DownloaderProperties properties) {
        this.properties = properties;
    }

    /**
     * Blocks the caller until the next allowed request time for {@code source}. Updates the source's next-allowed time before returning so concurrent callers for the same source serialize.
     * Returns immediately if no throttle is configured for the source.
     */
    public void await(String source) {
        DownloaderProperties.Throttle throttle = properties.throttleFor(source);
        long min = Math.max(0, throttle.getMinDelayMs());
        long max = Math.max(min, throttle.getMaxDelayMs());

        if (max == 0) {
            return;
        }

        long jitter = min == max ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
        long now = System.currentTimeMillis();

        long sleepFor;
        synchronized (nextAllowedAt) {
            long allowedAt = nextAllowedAt.getOrDefault(source, 0L);
            long startAt = Math.max(now, allowedAt);
            sleepFor = startAt - now;
            nextAllowedAt.put(source, startAt + jitter);
        }

        if (sleepFor > 0) {
            log.debug("Throttling {}: sleeping {}ms before next request", source, sleepFor);
            try {
                Thread.sleep(sleepFor);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Throttle wait interrupted for source " + source, e);
            }
        }
    }
}
