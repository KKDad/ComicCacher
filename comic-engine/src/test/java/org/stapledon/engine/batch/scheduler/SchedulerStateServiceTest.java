package org.stapledon.engine.batch.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.util.GsonUtils;

@DisplayName("SchedulerStateService")
class SchedulerStateServiceTest {

    @TempDir
    Path tempDir;

    private SchedulerStateService service;
    private CacheProperties cacheProperties;
    private Gson gson;

    @BeforeEach
    void setUp() {
        cacheProperties = new CacheProperties();
        cacheProperties.setLocation(tempDir.toString());
        gson = GsonUtils.createGsonBuilder().create();
        service = new SchedulerStateService(cacheProperties, gson);
    }

    @Test
    @DisplayName("should return false for unknown job")
    void shouldReturnFalseForUnknownJob() {
        assertThat(service.isPaused("UnknownJob")).isFalse();
    }

    @Test
    @DisplayName("should pause and resume a job")
    void shouldPauseAndResumeJob() {
        service.setPaused("TestJob", true, "admin");
        assertThat(service.isPaused("TestJob")).isTrue();

        service.setPaused("TestJob", false, "admin");
        assertThat(service.isPaused("TestJob")).isFalse();
    }

    @Test
    @DisplayName("should persist state to file and reload")
    void shouldPersistAndReload() {
        service.setPaused("TestJob", true, "admin");

        var reloaded = new SchedulerStateService(cacheProperties, gson);
        assertThat(reloaded.isPaused("TestJob")).isTrue();
    }

    @Test
    @DisplayName("should return state details")
    void shouldReturnStateDetails() {
        service.setPaused("TestJob", true, "testuser");

        var state = service.getState("TestJob");
        assertThat(state).isPresent();
        assertThat(state.get().paused()).isTrue();
        assertThat(state.get().toggledBy()).isEqualTo("testuser");
        assertThat(state.get().lastToggled()).isNotNull();
    }

    @Test
    @DisplayName("should return empty state for unknown job")
    void shouldReturnEmptyStateForUnknownJob() {
        assertThat(service.getState("UnknownJob")).isEmpty();
    }

    @Test
    @DisplayName("should return all states")
    void shouldReturnAllStates() {
        service.setPaused("Job1", true, "admin");
        service.setPaused("Job2", false, "admin");

        Map<String, SchedulerStateService.SchedulerState> states = service.getAllStates();
        assertThat(states).hasSize(2);
        assertThat(states).containsKey("Job1");
        assertThat(states).containsKey("Job2");
    }

    @Test
    @DisplayName("should handle missing state file gracefully")
    void shouldHandleMissingStateFile() {
        assertThat(service.getAllStates()).isEmpty();
    }

    @Test
    @DisplayName("should handle corrupt state file gracefully")
    void shouldHandleCorruptStateFile() throws Exception {
        Path stateFile = tempDir.resolve("scheduler-state.json");
        Files.writeString(stateFile, "not valid json{{{");

        var reloaded = new SchedulerStateService(cacheProperties, gson);
        assertThat(reloaded.getAllStates()).isEmpty();
    }
}
