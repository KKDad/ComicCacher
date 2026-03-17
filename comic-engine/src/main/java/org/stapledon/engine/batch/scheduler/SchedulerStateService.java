package org.stapledon.engine.batch.scheduler;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.stapledon.common.config.CacheProperties;

/**
 * Manages runtime pause/resume state for batch job schedulers.
 * State is persisted to a JSON file so it survives application restarts.
 */
@Slf4j
@Service
public class SchedulerStateService {

    private final CacheProperties cacheProperties;
    private final Gson gson;
    private final ConcurrentHashMap<String, SchedulerState> states = new ConcurrentHashMap<>();

    private static final String STATE_FILENAME = "scheduler-state.json";

    /**
     * Constructs a SchedulerStateService and loads persisted state.
     */
    public SchedulerStateService(CacheProperties cacheProperties, @Qualifier("gsonWithLocalDate") Gson gson) {
        this.cacheProperties = cacheProperties;
        this.gson = gson;
        loadStates();
    }

    /**
     * Checks whether the given job is currently paused.
     */
    public boolean isPaused(String jobName) {
        return Optional.ofNullable(states.get(jobName))
                .map(SchedulerState::paused)
                .orElse(false);
    }

    /**
     * Sets the paused state for a job and persists the change.
     */
    public void setPaused(String jobName, boolean paused, String username) {
        var state = new SchedulerState(paused, LocalDateTime.now(), username);
        states.put(jobName, state);
        persistStates();
        log.info("Job {} {} by {}", jobName, paused ? "paused" : "resumed", username);
    }

    /**
     * Returns the current state for a specific job, if any.
     */
    public Optional<SchedulerState> getState(String jobName) {
        return Optional.ofNullable(states.get(jobName));
    }

    /**
     * Returns a snapshot of all scheduler states.
     */
    public Map<String, SchedulerState> getAllStates() {
        return Map.copyOf(states);
    }

    private void loadStates() {
        Path filePath = getStateFilePath();
        if (!Files.exists(filePath)) {
            log.debug("No scheduler state file found at {}, starting with empty state", filePath);
            return;
        }

        try {
            String json = Files.readString(filePath);
            Type type = new TypeToken<Map<String, SchedulerState>>() {
            }.getType();
            Map<String, SchedulerState> loaded = gson.fromJson(json, type);
            if (loaded != null) {
                states.putAll(loaded);
                log.info("Loaded scheduler states for {} jobs", loaded.size());
            }
        } catch (IOException e) {
            log.error("Failed to read scheduler state file", e);
        } catch (JsonSyntaxException e) {
            log.error("Corrupt scheduler state file at {}, starting with empty state", filePath, e);
        }
    }

    private void persistStates() {
        Path filePath = getStateFilePath();
        try {
            Files.createDirectories(filePath.getParent());
            String json = gson.toJson(new HashMap<>(states));
            Files.writeString(filePath, json);
            log.debug("Scheduler states persisted to {}", filePath);
        } catch (IOException e) {
            log.error("Failed to persist scheduler states", e);
        }
    }

    private Path getStateFilePath() {
        return Paths.get(cacheProperties.getLocation(), STATE_FILENAME);
    }

    /**
     * Represents the pause/resume state for a single job scheduler.
     */
    public record SchedulerState(boolean paused, LocalDateTime lastToggled, String toggledBy) {
    }
}
