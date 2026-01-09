package org.stapledon.metrics.collector;

import org.springframework.beans.factory.annotation.Qualifier;
import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.repository.AccessMetricsRepository;

import com.google.common.base.Stopwatch;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Collector for access metrics and comic cache operations.
 * This class delegates storage operations to ComicStorageFacade
 * while tracking access patterns for metrics and performance monitoring.
 *
 * Note: This class is configured as a @Bean in CacheConfiguration, not
 * as @Component
 */
@Slf4j
@ToString
public class AccessMetricsCollector {
    private static final int WARNING_TIME_MS = 100;
    public static final String COMBINE_PATH = "%s/%s";
    private final String cacheHome;
    private final ComicStorageFacade storageFacade;
    private final AccessMetricsRepository accessMetricsRepository;

    // Access tracking metrics (in-memory for performance)
    private final ConcurrentHashMap<String, AtomicInteger> accessCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> lastAccessTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> totalAccessTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> cacheHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> cacheMisses = new ConcurrentHashMap<>();

    /**
     * Primary constructor with StorageFacade and AccessMetricsRepository
     * dependencies
     */
    public AccessMetricsCollector(@Qualifier("cacheLocation") String cacheHome,
            ComicStorageFacade storageFacade,
            AccessMetricsRepository accessMetricsRepository) {
        this.cacheHome = Objects.requireNonNull(cacheHome, "cacheHome must be specified");
        this.storageFacade = Objects.requireNonNull(storageFacade, "storageFacade must be specified");
        this.accessMetricsRepository = Objects.requireNonNull(accessMetricsRepository,
                "accessMetricsRepository must be specified");
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
        log.info("Shutting down CacheUtils, persisting access metrics");
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
            log.info("Loaded access metrics for {} comics from persistent storage",
                    data.getComicMetrics().size());
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
                    .comicName(comicName)
                    .accessCount(count.get())
                    .lastAccess(lastAccessTime.getOrDefault(comicName, ""))
                    .totalAccessTimeMs(totalAccessTime.getOrDefault(comicName, 0L))
                    .cacheHits(cacheHits.getOrDefault(comicName, new AtomicInteger(0)).get())
                    .cacheMisses(cacheMisses.getOrDefault(comicName, new AtomicInteger(0)).get())
                    .build();
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
        totalAccessTime.compute(comicName, (k, v) -> (v == null) ? accessTime : v + accessTime);
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

    public File findOldest(ComicItem comic) {
        var timer = Stopwatch.createStarted();
        File result = null;

        // Use the facade
        Optional<LocalDate> oldestDate = storageFacade.getOldestDateWithComic(ComicIdentifier.from(comic));
        if (oldestDate.isPresent()) {
            LocalDate date = oldestDate.get();
            String yearPath = date.format(DateTimeFormatter.ofPattern("yyyy"));
            String datePath = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            result = new File(String.format("%s/%s/%s/%s.png",
                    cacheHome, comic.getName().replace(" ", ""), yearPath, datePath));
        }

        timer.stop();
        trackAccess(comic.getName(), result != null, timer.elapsed(TimeUnit.MILLISECONDS));
        return result;
    }

    public File findNewest(ComicItem comic) {
        var timer = Stopwatch.createStarted();
        File result = null;

        // Use the facade
        Optional<LocalDate> newestDate = storageFacade.getNewestDateWithComic(ComicIdentifier.from(comic));
        if (newestDate.isPresent()) {
            LocalDate date = newestDate.get();
            String yearPath = date.format(DateTimeFormatter.ofPattern("yyyy"));
            String datePath = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            result = new File(String.format("%s/%s/%s/%s.png",
                    cacheHome, comic.getName().replace(" ", ""), yearPath, datePath));
        }

        timer.stop();
        trackAccess(comic.getName(), result != null, timer.elapsed(TimeUnit.MILLISECONDS));
        return result;
    }

    public File findNext(ComicItem comic, LocalDate from) {
        var timer = Stopwatch.createStarted();
        File resultFile = null;
        boolean found = false;

        // Use the facade
        Optional<LocalDate> nextDate = storageFacade.getNextDateWithComic(ComicIdentifier.from(comic), from);
        if (nextDate.isPresent()) {
            LocalDate date = nextDate.get();
            String yearPath = date.format(DateTimeFormatter.ofPattern("yyyy"));
            String datePath = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            resultFile = new File(String.format("%s/%s/%s/%s.png",
                    cacheHome, comic.getName().replace(" ", ""), yearPath, datePath));
            found = true;
        }

        timer.stop();
        if (timer.elapsed(TimeUnit.MILLISECONDS) > WARNING_TIME_MS && log.isInfoEnabled())
            log.info(String.format("findNext took: %s for %s", timer, comic.getName()));

        trackAccess(comic.getName(), found, timer.elapsed(TimeUnit.MILLISECONDS));
        return resultFile;
    }

    public File findPrevious(ComicItem comic, LocalDate from) {
        var timer = Stopwatch.createStarted();
        File resultFile = null;
        boolean found = false;

        // Use the facade
        Optional<LocalDate> prevDate = storageFacade.getPreviousDateWithComic(ComicIdentifier.from(comic), from);
        if (prevDate.isPresent()) {
            LocalDate date = prevDate.get();
            String yearPath = date.format(DateTimeFormatter.ofPattern("yyyy"));
            String datePath = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            resultFile = new File(String.format("%s/%s/%s/%s.png",
                    cacheHome, comic.getName().replace(" ", ""), yearPath, datePath));
            found = true;
        }

        timer.stop();
        if (timer.elapsed(TimeUnit.MILLISECONDS) > WARNING_TIME_MS && log.isInfoEnabled())
            log.info(String.format("findPrevious took: %s for %s", timer, comic.getName()));

        trackAccess(comic.getName(), found, timer.elapsed(TimeUnit.MILLISECONDS));
        return resultFile;
    }
}
