package org.stapledon.common.infrastructure.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.stapledon.common.config.CacheProperties;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of ExecutionTracker that persists execution data to a JSON file.
 * Ensures tasks only run once per day by tracking their last execution date.
 */
@Slf4j
@ToString
@Component
@RequiredArgsConstructor
public class JsonExecutionTracker implements ExecutionTracker {

    @Qualifier("gsonWithLocalDate")
    private final Gson gson;
    private final CacheProperties cacheProperties;

    private final Map<String, LocalDate> taskExecutions = new HashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static final String EXECUTION_TRACKER_FILE = "task-executions.json";

    /**
     * Initialize by loading existing execution data.
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        loadTaskExecutions();
    }

    @Override
    public boolean canRunToday(String taskName) {
        lock.readLock().lock();
        try {
            LocalDate lastRun = taskExecutions.get(taskName);
            LocalDate today = LocalDate.now();

            // Task has never run or hasn't run today
            return lastRun == null || !lastRun.equals(today);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean markTaskExecuted(String taskName) {
        lock.writeLock().lock();
        try {
            taskExecutions.put(taskName, LocalDate.now());
            return saveTaskExecutions();
        } catch (Exception e) {
            log.error("Failed to mark task {} as executed", taskName, e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public LocalDate getLastExecutionDate(String taskName) {
        lock.readLock().lock();
        try {
            return taskExecutions.get(taskName);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Load task execution data from the JSON file.
     */
    private void loadTaskExecutions() {
        lock.writeLock().lock();
        try {
            Path filePath = Paths.get(cacheProperties.getLocation(), EXECUTION_TRACKER_FILE);

            if (Files.exists(filePath)) {
                try (Reader reader = new FileReader(filePath.toFile())) {
                    Type type = new TypeToken<Map<String, LocalDate>>() {}.getType();
                    Map<String, LocalDate> loadedData = gson.fromJson(reader, type);

                    if (loadedData != null) {
                        taskExecutions.clear();
                        taskExecutions.putAll(loadedData);
                        log.info("Loaded task execution data for {} tasks", taskExecutions.size());
                    }
                } catch (Exception e) {
                    log.error("Failed to load task execution data", e);
                }
            } else {
                log.info("Task execution tracker file does not exist yet, will be created when tasks run");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Save task execution data to the JSON file
     *
     * @return true if successful, false otherwise
     */
    private boolean saveTaskExecutions() {
        try {
            Path directory = Paths.get(cacheProperties.getLocation());
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            Path filePath = directory.resolve(EXECUTION_TRACKER_FILE);

            try (Writer writer = new FileWriter(filePath.toFile())) {
                gson.toJson(taskExecutions, writer);
                writer.flush();
                return true;
            }
        } catch (IOException e) {
            log.error("Failed to save task execution data", e);
            return false;
        }
    }
}