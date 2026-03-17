package org.stapledon.engine.batch.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.stapledon.common.config.CacheProperties;

@DisplayName("BatchJobLogService")
class BatchJobLogServiceTest {

    @TempDir
    Path tempDir;

    private BatchJobLogService service;

    @BeforeEach
    void setUp() {
        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.setLocation(tempDir.toString());
        service = new BatchJobLogService(cacheProperties);
    }

    @Test
    @DisplayName("should read existing log file")
    void shouldReadExistingLogFile() throws Exception {
        Path logDir = tempDir.resolve("batch-logs").resolve("TestJob");
        Files.createDirectories(logDir);
        Files.writeString(logDir.resolve("42.log"), "INFO Starting job...\nINFO Job completed.");

        Optional<String> log = service.getExecutionLog(42, "TestJob");
        assertThat(log).isPresent();
        assertThat(log.get()).contains("Starting job");
        assertThat(log.get()).contains("Job completed");
    }

    @Test
    @DisplayName("should return empty for missing log file")
    void shouldReturnEmptyForMissingLog() {
        Optional<String> log = service.getExecutionLog(999, "NonexistentJob");
        assertThat(log).isEmpty();
    }

    @Test
    @DisplayName("should list recent log files sorted descending")
    void shouldListRecentLogFiles() throws Exception {
        Path logDir = tempDir.resolve("batch-logs").resolve("TestJob");
        Files.createDirectories(logDir);
        Files.writeString(logDir.resolve("10.log"), "log 10");
        Files.writeString(logDir.resolve("20.log"), "log 20");
        Files.writeString(logDir.resolve("30.log"), "log 30");

        List<Path> recent = service.getRecentLogFiles("TestJob", 2);
        assertThat(recent).hasSize(2);
        assertThat(recent.get(0).getFileName().toString()).isEqualTo("30.log");
        assertThat(recent.get(1).getFileName().toString()).isEqualTo("20.log");
    }

    @Test
    @DisplayName("should return empty list for missing job directory")
    void shouldReturnEmptyListForMissingDir() {
        List<Path> recent = service.getRecentLogFiles("NonexistentJob", 10);
        assertThat(recent).isEmpty();
    }
}
