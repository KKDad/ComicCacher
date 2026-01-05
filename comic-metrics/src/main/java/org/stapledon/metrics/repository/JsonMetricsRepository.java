package org.stapledon.metrics.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.stapledon.metrics.dto.CombinedMetricsData;

import com.google.gson.Gson;
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
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON file-based implementation of MetricsRepository.
 * Loads and saves combined metrics to combined-metrics.json in the cache
 * directory.
 */
@Slf4j
@ToString
public class JsonMetricsRepository implements MetricsRepository {

    private final Gson gson;
    private final String cacheLocation;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private CombinedMetricsData cachedMetrics;

    private static final String COMBINED_METRICS_FILE = "combined-metrics.json";

    public JsonMetricsRepository(
            @Qualifier("gsonWithLocalDate") Gson gson,
            @Qualifier("cacheLocation") String cacheLocation) {
        this.gson = gson;
        this.cacheLocation = cacheLocation;
    }

    /**
     * Initialize by loading existing combined metrics.
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        load();
    }

    @Override
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

    @Override
    public CombinedMetricsData load() {
        lock.writeLock().lock();
        try {
            Path filePath = Paths.get(cacheLocation, COMBINED_METRICS_FILE);

            if (Files.exists(filePath)) {
                try (Reader reader = new FileReader(filePath.toFile())) {
                    CombinedMetricsData loadedData = gson.fromJson(reader, CombinedMetricsData.class);

                    if (loadedData != null) {
                        cachedMetrics = loadedData;
                        int comicCount = cachedMetrics.getPerComicMetrics() != null
                                ? cachedMetrics.getPerComicMetrics().size()
                                : 0;
                        log.info("Loaded combined metrics for {} comics from {}", comicCount, COMBINED_METRICS_FILE);
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
            return cachedMetrics;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean save(CombinedMetricsData metrics) {
        lock.writeLock().lock();
        try {
            // Update timestamp
            metrics.setLastUpdated(LocalDateTime.now());

            Path directory = Paths.get(cacheLocation);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            Path filePath = directory.resolve(COMBINED_METRICS_FILE);

            try (Writer writer = new FileWriter(filePath.toFile())) {
                gson.toJson(metrics, writer);
                writer.flush();
                cachedMetrics = metrics;
                int comicCount = metrics.getPerComicMetrics() != null
                        ? metrics.getPerComicMetrics().size()
                        : 0;
                log.debug("Saved combined metrics for {} comics", comicCount);
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
