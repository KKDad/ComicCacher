package org.stapledon.metrics.repository;

import com.google.gson.Gson;

import org.springframework.beans.factory.annotation.Qualifier;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.metrics.dto.CombinedMetricsData;

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
 * Repository for combined metrics persistence.
 * Loads and saves combined metrics to combined-metrics.json in the cache directory.
 * Configured as a bean in MetricsConfiguration when metrics are enabled.
 */
@Slf4j
public class CombinedMetricsRepository {

    private final Gson gson;
    private final CacheProperties cacheProperties;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private CombinedMetricsData cachedMetrics;

    private static final String COMBINED_METRICS_FILE = "combined-metrics.json";

    public CombinedMetricsRepository(@Qualifier("gsonWithLocalDate") Gson gson, CacheProperties cacheProperties) {
        this.gson = gson;
        this.cacheProperties = cacheProperties;
    }

    /**
     * Initialize by loading existing combined metrics.
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        load();
    }

    /**
     * Get the current combined metrics.
     * Returns cached metrics if available, otherwise loads from disk.
     *
     * @return Combined metrics data
     */
    public CombinedMetricsData get() {
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
     * Load combined metrics from the JSON file.
     */
    public void load() {
        lock.writeLock().lock();
        try {
            Path filePath = Paths.get(cacheProperties.getLocation(), COMBINED_METRICS_FILE);

            if (Files.exists(filePath)) {
                try (Reader reader = new FileReader(filePath.toFile())) {
                    CombinedMetricsData loadedData = gson.fromJson(reader, CombinedMetricsData.class);

                    if (loadedData != null) {
                        cachedMetrics = loadedData;
                        log.info("Loaded combined metrics for {} comics from {}",
                            cachedMetrics.getComics().size(), COMBINED_METRICS_FILE);
                    } else {
                        cachedMetrics = createEmpty();
                        log.info("Combined metrics file was empty, initialized new metrics");
                    }
                } catch (Exception e) {
                    log.error("Failed to load combined metrics, initializing empty", e);
                    cachedMetrics = createEmpty();
                }
            } else {
                log.info("Combined metrics file does not exist yet, will be created on first save");
                cachedMetrics = createEmpty();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Save combined metrics to the JSON file.
     *
     * @param metrics Combined metrics to save
     * @return true if successful, false otherwise
     */
    public boolean save(CombinedMetricsData metrics) {
        lock.writeLock().lock();
        try {
            // Update timestamp
            metrics.setLastUpdated(LocalDateTime.now());

            Path directory = Paths.get(cacheProperties.getLocation());
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            Path filePath = directory.resolve(COMBINED_METRICS_FILE);

            try (Writer writer = new FileWriter(filePath.toFile())) {
                gson.toJson(metrics, writer);
                writer.flush();
                cachedMetrics = metrics;
                log.debug("Saved combined metrics for {} comics", metrics.getComics().size());
                return true;
            }
        } catch (IOException e) {
            log.error("Failed to save combined metrics", e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Create an empty CombinedMetricsData object
     *
     * @return Empty combined metrics
     */
    private CombinedMetricsData createEmpty() {
        return CombinedMetricsData.builder()
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}
