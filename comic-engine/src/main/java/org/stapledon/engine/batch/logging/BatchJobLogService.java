package org.stapledon.engine.batch.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.stapledon.common.config.CacheProperties;

/**
 * Reads per-execution log files produced by the SiftingAppender.
 * Log files are stored at {@code ${comics.cache.location}/batch-logs/{jobName}/{executionId}.log}.
 */
@Slf4j
@Service
public class BatchJobLogService {

    private final CacheProperties cacheProperties;

    private static final String BATCH_LOGS_DIR = "batch-logs";

    /**
     * Constructs a BatchJobLogService.
     */
    public BatchJobLogService(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    /**
     * Reads the log file for a specific job execution.
     */
    public Optional<String> getExecutionLog(long executionId, String jobName) {
        Path logFile = getLogFilePath(jobName, executionId);
        if (!Files.exists(logFile)) {
            log.debug("No log file found for {} execution {}", jobName, executionId);
            return Optional.empty();
        }

        try {
            return Optional.of(Files.readString(logFile));
        } catch (IOException e) {
            log.error("Failed to read log file for {} execution {}", jobName, executionId, e);
            return Optional.empty();
        }
    }

    /**
     * Lists recent log files for a job, sorted by execution ID descending.
     */
    public List<Path> getRecentLogFiles(String jobName, int count) {
        Path jobDir = getJobLogDir(jobName);
        if (!Files.isDirectory(jobDir)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(jobDir)) {
            return files
                    .filter(p -> p.toString().endsWith(".log"))
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .limit(count)
                    .toList();
        } catch (IOException e) {
            log.error("Failed to list log files for {}", jobName, e);
            return List.of();
        }
    }

    private Path getLogFilePath(String jobName, long executionId) {
        return getJobLogDir(jobName).resolve(executionId + ".log");
    }

    private Path getJobLogDir(String jobName) {
        return Paths.get(cacheProperties.getLocation(), BATCH_LOGS_DIR, jobName);
    }
}
