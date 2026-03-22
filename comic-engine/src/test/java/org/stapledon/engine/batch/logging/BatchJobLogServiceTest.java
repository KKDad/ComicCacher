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
    @DisplayName("should read existing log file by name")
    void shouldReadExistingLogFile() throws Exception {
        Path logDir = tempDir.resolve("batch-logs").resolve("TestJob");
        Files.createDirectories(logDir);
        Files.writeString(logDir.resolve("TestJob-20260320-a3f7b2c1.log"), "INFO Starting job...\nINFO Job completed.");

        Optional<String> log = service.getExecutionLog("TestJob", "TestJob-20260320-a3f7b2c1.log");
        assertThat(log).isPresent();
        assertThat(log.get()).contains("Starting job");
        assertThat(log.get()).contains("Job completed");
    }

    @Test
    @DisplayName("should return empty for missing log file")
    void shouldReturnEmptyForMissingLog() {
        Optional<String> log = service.getExecutionLog("NonexistentJob", "NonexistentJob-20260320-abcd1234.log");
        assertThat(log).isEmpty();
    }

    @Test
    @DisplayName("should list recent log files sorted descending")
    void shouldListRecentLogFiles() throws Exception {
        Path logDir = tempDir.resolve("batch-logs").resolve("TestJob");
        Files.createDirectories(logDir);
        Files.writeString(logDir.resolve("TestJob-20260318-abc12345.log"), "log 1");
        Files.writeString(logDir.resolve("TestJob-20260319-def67890.log"), "log 2");
        Files.writeString(logDir.resolve("TestJob-20260320-fed09876.log"), "log 3");

        List<Path> recent = service.getRecentLogFiles("TestJob", 2);
        assertThat(recent).hasSize(2);
        assertThat(recent.get(0).getFileName().toString()).isEqualTo("TestJob-20260320-fed09876.log");
        assertThat(recent.get(1).getFileName().toString()).isEqualTo("TestJob-20260319-def67890.log");
    }

    @Test
    @DisplayName("should return empty list for missing job directory")
    void shouldReturnEmptyListForMissingDir() {
        List<Path> recent = service.getRecentLogFiles("NonexistentJob", 10);
        assertThat(recent).isEmpty();
    }

    @Test
    @DisplayName("should return empty string for empty log file")
    void shouldReturnEmptyStringForEmptyLogFile() throws Exception {
        Path logDir = tempDir.resolve("batch-logs").resolve("TestJob");
        Files.createDirectories(logDir);
        Files.writeString(logDir.resolve("TestJob-20260320-abcd1234.log"), "");

        Optional<String> log = service.getExecutionLog("TestJob", "TestJob-20260320-abcd1234.log");
        assertThat(log).isPresent();
        assertThat(log.get()).isEmpty();
    }

    @Test
    @DisplayName("should filter non-log files in getRecentLogFiles")
    void shouldFilterNonLogFiles() throws Exception {
        Path logDir = tempDir.resolve("batch-logs").resolve("TestJob");
        Files.createDirectories(logDir);
        Files.writeString(logDir.resolve("TestJob-20260320-abcd1234.log"), "log content");
        Files.writeString(logDir.resolve("TestJob-20260320-abcd1234.txt"), "not a log");
        Files.writeString(logDir.resolve("summary.json"), "{}");

        List<Path> recent = service.getRecentLogFiles("TestJob", 10);
        assertThat(recent).hasSize(1);
        assertThat(recent.getFirst().getFileName().toString()).endsWith(".log");
    }

    @Test
    @DisplayName("should return empty list when count is zero")
    void shouldReturnEmptyListWhenCountIsZero() throws Exception {
        Path logDir = tempDir.resolve("batch-logs").resolve("TestJob");
        Files.createDirectories(logDir);
        Files.writeString(logDir.resolve("TestJob-20260320-abcd1234.log"), "log content");

        List<Path> recent = service.getRecentLogFiles("TestJob", 0);
        assertThat(recent).isEmpty();
    }

    @Test
    @DisplayName("should sort log files in descending order by filename")
    void shouldSortDescendingByFilename() throws Exception {
        Path logDir = tempDir.resolve("batch-logs").resolve("TestJob");
        Files.createDirectories(logDir);
        Files.writeString(logDir.resolve("TestJob-20260315-aaa11111.log"), "oldest");
        Files.writeString(logDir.resolve("TestJob-20260320-bbb22222.log"), "newest");
        Files.writeString(logDir.resolve("TestJob-20260318-ccc33333.log"), "middle");

        List<Path> recent = service.getRecentLogFiles("TestJob", 10);
        assertThat(recent).hasSize(3);
        assertThat(recent.get(0).getFileName().toString()).contains("20260320");
        assertThat(recent.get(1).getFileName().toString()).contains("20260318");
        assertThat(recent.get(2).getFileName().toString()).contains("20260315");
    }
}
