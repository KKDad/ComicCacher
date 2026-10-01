package org.stapledon.engine.downloader;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
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
 *
 * <p>When a source answers HTTP 429, {@link #backOff(String, int, Optional)} pushes that source's next allowed time further out, so the whole source slows down.
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
     * Returns immediately if no throttle is configured for the source and no 429 backoff is pending.
     */
    public void await(String source) {
        DownloaderProperties.Throttle throttle = properties.throttleFor(source);
        long min = Math.max(0, throttle.getMinDelayMs());
        long max = Math.max(min, throttle.getMaxDelayMs());

        // Unthrottled sources skip the bookkeeping unless a 429 backoff is pending for them
        if (max == 0 && !nextAllowedAt.containsKey(source)) {
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

    /**
     * Total attempts allowed per download when {@code source} answers HTTP 429. Always at least 1.
     */
    public int maxAttempts(String source) {
        return Math.max(1, properties.retryFor(source).getMaxAttempts());
    }

    /**
     * Records an HTTP 429 from {@code source} and pushes its next allowed request time back, so every caller for that source waits, not just the one that was rejected.
     * The backoff honours {@code retryAfter} when present, otherwise doubles from {@code initial-backoff-ms} per attempt with up to 20% jitter; either way it is capped at
     * {@code max-backoff-ms} (when configured).
     *
     * @param attempt the 1-based attempt that was rate limited
     * @return the backoff applied
     */
    public Duration backOff(String source, int attempt, Optional<Duration> retryAfter) {
        DownloaderProperties.Retry retry = properties.retryFor(source);
        long backoffMs = retryAfter.map(Duration::toMillis).orElseGet(() -> exponentialBackoffMs(retry, attempt));
        if (retry.getMaxBackoffMs() > 0) {
            backoffMs = Math.min(backoffMs, retry.getMaxBackoffMs());
        }

        long resumeAt = System.currentTimeMillis() + backoffMs;
        synchronized (nextAllowedAt) {
            nextAllowedAt.merge(source, resumeAt, Math::max);
        }
        return Duration.ofMillis(backoffMs);
    }

    /**
     * Runs one request against {@code source}, paced by {@link #await(String)}. An HTTP 429 backs the source off and retries, up to
     * {@link #maxAttempts(String)} attempts; the last 429 is rethrown.
     */
    public <T> T withRetries(String source, IoCall<T> call) throws IOException {
        int maxAttempts = maxAttempts(source);
        for (int attempt = 1; ; attempt++) {
            await(source);
            try {
                return call.call();
            } catch (RateLimitedException e) {
                Duration backoff = backOff(source, attempt, e.getRetryAfter());
                if (attempt >= maxAttempts) {
                    throw e;
                }
                log.warn("Rate limited (HTTP 429) on {}, attempt {}/{}; Retry-After={}; backing off {}s", e.getUrl(), attempt, maxAttempts,
                        e.getRetryAfter().map(d -> d.toSeconds() + "s").orElse("none"), backoff.toSeconds());
            }
        }
    }

    /**
     * A request that can fail with an {@link IOException}.
     */
    @FunctionalInterface
    public interface IoCall<T> {
        T call() throws IOException;
    }

    private static long exponentialBackoffMs(DownloaderProperties.Retry retry, int attempt) {
        long initial = Math.max(0, retry.getInitialBackoffMs());
        long base = initial << Math.min(Math.max(0, attempt - 1), 20);
        long jitter = base == 0 ? 0 : ThreadLocalRandom.current().nextLong(base / 5 + 1);
        return base + jitter;
    }
}
