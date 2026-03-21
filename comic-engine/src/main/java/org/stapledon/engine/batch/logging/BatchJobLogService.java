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
 * Log files are stored at {@code ${comics.cache.location}/batch-logs/{jobName}/{jobName}-{date}-{hash}.log}.
 * The exact filename is recorded in {@code BatchExecutionSummary.logFileName}.
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
     * Reads the log file for a specific job execution by its stored filename.
     */
    public Optional<String> getExecutionLog(String jobName, String logFileName) {
        Path logFile = getJobLogDir(jobName).resolve(logFileName);
        if (!Files.exists(logFile)) {
            log.debug("No log file found at {}", logFile);
            return Optional.empty();
        }

        try {
            return Optional.of(Files.readString(logFile));
        } catch (IOException e) {
            log.error("Failed to read log file {}", logFile, e);
            return Optional.empty();
        }
    }

    /**
     * Lists recent log files for a job, sorted by filename descending.
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

    private Path getJobLogDir(String jobName) {
        return Paths.get(cacheProperties.getLocation(), BATCH_LOGS_DIR, jobName);
    }
}
