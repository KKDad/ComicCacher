package org.stapledon.metrics.collector;

import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.repository.AccessMetricsRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Collector for access metrics. Tracks comic access patterns for metrics and
 * performance monitoring. Access is recorded via {@link #trackAccess} and
 * persisted to disk after a configurable number of accesses.
 *
 * Note: This class is configured as a @Bean in MetricsConfiguration, not
 * as @Component
 */
@Slf4j
@ToString
public class AccessMetricsCollector {
    private final AccessMetricsRepository accessMetricsRepository;

    // Access tracking metrics (in-memory for performance)
    private final ConcurrentHashMap<String, AtomicInteger> accessCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> lastAccessTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> totalAccessTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> cacheHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> cacheMisses = new ConcurrentHashMap<>();

    // Event-driven persistence: persist after N accesses
    private final AtomicInteger accessSinceLastPersist = new AtomicInteger(0);
    private final int persistThreshold;

    /**
     * Primary constructor with AccessMetricsRepository dependency.
     *
     * @param accessMetricsRepository Repository for persisting access metrics
     * @param persistThreshold        Number of accesses before persisting (default 50)
     */
    public AccessMetricsCollector(AccessMetricsRepository accessMetricsRepository,
            @org.springframework.beans.factory.annotation.Value("${comics.metrics.persist-threshold:50}") int persistThreshold) {
        this.accessMetricsRepository = Objects.requireNonNull(accessMetricsRepository,
                "accessMetricsRepository must be specified");
        this.persistThreshold = persistThreshold;
        log.info("AccessMetricsCollector initialized with persist threshold: {}", persistThreshold);
    }

    /**
     * Initialize by loading persisted access metrics from disk
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        loadAccessMetrics();
    }

    /**
     * Persist access metrics to disk on shutdown
     */
    @jakarta.annotation.PreDestroy
    public void shutdown() {
        log.info("Shutting down AccessMetricsCollector, persisting access metrics");
        persistAccessMetrics();
    }

    /**
     * Load access metrics from persistent storage into in-memory maps
     */
    private void loadAccessMetrics() {
        AccessMetricsData data = accessMetricsRepository.get();
        if (data != null && data.getComicMetrics() != null) {
            data.getComicMetrics().forEach((comicName, metrics) -> {
                accessCounters.put(comicName, new AtomicInteger(metrics.getAccessCount()));
                lastAccessTime.put(comicName, metrics.getLastAccess());
                totalAccessTime.put(comicName, metrics.getTotalAccessTimeMs());
                cacheHits.put(comicName, new AtomicInteger(metrics.getCacheHits()));
                cacheMisses.put(comicName, new AtomicInteger(metrics.getCacheMisses()));
            });
            log.info("Loaded access metrics for {} comics from persistent storage", data.getComicMetrics().size());
        }
    }

    /**
     * Persist current in-memory access metrics to disk
     */
    public void persistAccessMetrics() {
        AccessMetricsData data = AccessMetricsData.builder().build();

        Map<String, AccessMetricsData.ComicAccessMetrics> comicMetrics = new HashMap<>();
        accessCounters.forEach((comicName, count) -> {
            AccessMetricsData.ComicAccessMetrics metrics = AccessMetricsData.ComicAccessMetrics.builder()
                    .comicName(comicName).accessCount(count.get())
                    .lastAccess(lastAccessTime.getOrDefault(comicName, ""))
                    .totalAccessTimeMs(totalAccessTime.getOrDefault(comicName, 0L))
                    .cacheHits(cacheHits.getOrDefault(comicName, new AtomicInteger(0)).get())
                    .cacheMisses(cacheMisses.getOrDefault(comicName, new AtomicInteger(0)).get()).build();
            comicMetrics.put(comicName, metrics);
        });

        data.setComicMetrics(comicMetrics);
        accessMetricsRepository.save(data);
    }

    /**
     * Track access to a comic.
     *
     * @param comicName  the name of the comic being accessed
     * @param isHit      whether this was a cache hit (true) or miss (false)
     * @param accessTime time taken for the access in milliseconds
     */
    public void trackAccess(String comicName, boolean isHit, long accessTime) {
        accessCounters.computeIfAbsent(comicName, k -> new AtomicInteger(0)).incrementAndGet();
        lastAccessTime.put(comicName, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Track hit/miss statistics
        if (isHit) {
            cacheHits.computeIfAbsent(comicName, k -> new AtomicInteger(0)).incrementAndGet();
        } else {
            cacheMisses.computeIfAbsent(comicName, k -> new AtomicInteger(0)).incrementAndGet();
        }

        // Track timing statistics
        totalAccessTime.compute(comicName, (k, v) -> v == null ? accessTime : v + accessTime);

        // Event-driven persistence: persist after threshold accesses
        if (accessSinceLastPersist.incrementAndGet() >= persistThreshold) {
            log.debug("Persist threshold ({}) reached, persisting access metrics", persistThreshold);
            persistAccessMetrics();
            accessSinceLastPersist.set(0);
        }
    }

    /**
     * Get access count metrics for all comics
     *
     * @return Map of comic name to access count
     */
    public Map<String, Integer> getAccessCounts() {
        Map<String, Integer> result = new HashMap<>();
        accessCounters.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }

    /**
     * Get last access time for all comics
     *
     * @return Map of comic name to last access time
     */
    public Map<String, String> getLastAccessTimes() {
        return new HashMap<>(lastAccessTime);
    }

    /**
     * Get average access time for all comics
     *
     * @return Map of comic name to average access time in milliseconds
     */
    public Map<String, Double> getAverageAccessTimes() {
        Map<String, Double> result = new HashMap<>();
        accessCounters.forEach((comic, count) -> {
            Long total = totalAccessTime.getOrDefault(comic, 0L);
            result.put(comic, count.get() > 0 ? (double) total / count.get() : 0.0);
        });
        return result;
    }

    /**
     * Get hit ratio for all comics
     *
     * @return Map of comic name to hit ratio (0.0-1.0)
     */
    public Map<String, Double> getHitRatios() {
        Map<String, Double> result = new HashMap<>();
        accessCounters.forEach((comic, totalCount) -> {
            int hits = cacheHits.getOrDefault(comic, new AtomicInteger(0)).get();
            result.put(comic, totalCount.get() > 0 ? (double) hits / totalCount.get() : 0.0);
        });
        return result;
    }
}
