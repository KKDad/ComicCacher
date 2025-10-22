package org.stapledon.infrastructure.metrics;

import com.google.gson.Gson;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.infrastructure.config.properties.CacheProperties;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.extern.slf4j.Slf4j;

/**
 * Repository for access metrics persistence.
 * Loads and saves access metrics to access-metrics.json in the cache directory.
 */
@Slf4j
@Component
public class AccessMetricsRepository {

    private final Gson gson;
    private final CacheProperties cacheProperties;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private AccessMetricsData cachedMetrics;

    private static final String ACCESS_METRICS_FILE = "access-metrics.json";

    public AccessMetricsRepository(@Qualifier("gsonWithLocalDate") Gson gson, CacheProperties cacheProperties) {
        this.gson = gson;
        this.cacheProperties = cacheProperties;
    }

    /**
     * Initialize by loading existing access metrics.
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        load();
    }

    /**
     * Get the current access metrics.
     * Returns cached metrics if available, otherwise loads from disk.
     *
     * @return Access metrics data
     */
    public AccessMetricsData get() {
        lock.readLock().lock();
        try {
            if (cachedMetrics == null) {
                lock.readLock().unlock();
                load();
                lock.readLock().lock();
            }
            return cachedMetrics;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Load access metrics from the JSON file.
     */
    public void load() {
        lock.writeLock().lock();
        try {
            Path filePath = Paths.get(cacheProperties.getLocation(), ACCESS_METRICS_FILE);

            if (Files.exists(filePath)) {
                try (Reader reader = new FileReader(filePath.toFile())) {
                    AccessMetricsData loadedData = gson.fromJson(reader, AccessMetricsData.class);

                    if (loadedData != null) {
                        cachedMetrics = loadedData;
                        log.info("Loaded access metrics for {} comics from {}",
                            cachedMetrics.getComicMetrics().size(), ACCESS_METRICS_FILE);
                    } else {
                        cachedMetrics = createEmpty();
                        log.info("Access metrics file was empty, initialized new metrics");
                    }
                } catch (Exception e) {
                    log.error("Failed to load access metrics, initializing empty", e);
                    cachedMetrics = createEmpty();
                }
            } else {
                log.info("Access metrics file does not exist yet, will be created on first save");
                cachedMetrics = createEmpty();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Save access metrics to the JSON file.
     *
     * @param metrics Access metrics to save
     * @return true if successful, false otherwise
     */
    public boolean save(AccessMetricsData metrics) {
        lock.writeLock().lock();
        try {
            // Update timestamp
            metrics.setLastUpdated(LocalDateTime.now());

            Path directory = Paths.get(cacheProperties.getLocation());
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            Path filePath = directory.resolve(ACCESS_METRICS_FILE);

            try (Writer writer = new FileWriter(filePath.toFile())) {
                gson.toJson(metrics, writer);
                writer.flush();
                cachedMetrics = metrics;
                log.debug("Saved access metrics for {} comics", metrics.getComicMetrics().size());
                return true;
            }
        } catch (IOException e) {
            log.error("Failed to save access metrics", e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Create an empty AccessMetricsData object
     *
     * @return Empty access metrics
     */
    private AccessMetricsData createEmpty() {
        return AccessMetricsData.builder()
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}
