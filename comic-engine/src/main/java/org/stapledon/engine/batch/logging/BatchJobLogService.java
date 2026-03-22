package org.stapledon.engine.batch.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final Pattern LOG_DATE_PATTERN = Pattern.compile("^.+-(\\d{8})-[0-9a-f]{8}\\.log$");

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

    /**
     * Deletes batch log files older than the specified retention period across all job directories.
     */
    public int purgeOldLogFiles(int daysToKeep) {
        Path batchLogsRoot = Paths.get(cacheProperties.getLocation(), BATCH_LOGS_DIR);
        if (!Files.isDirectory(batchLogsRoot)) {
            log.debug("Batch logs directory does not exist: {}", batchLogsRoot);
            return 0;
        }

        LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);
        int deletedCount = 0;

        try (Stream<Path> jobDirs = Files.list(batchLogsRoot)) {
            List<Path> directories = jobDirs.filter(Files::isDirectory).toList();
            for (Path jobDir : directories) {
                deletedCount += purgeLogFilesInDirectory(jobDir, cutoffDate);
            }
        } catch (IOException e) {
            log.error("Failed to list batch log directories under {}", batchLogsRoot, e);
        }

        return deletedCount;
    }

    private int purgeLogFilesInDirectory(Path jobDir, LocalDate cutoffDate) {
        int deleted = 0;
        try (Stream<Path> files = Files.list(jobDir)) {
            List<Path> logFiles = files.filter(p -> p.toString().endsWith(".log")).toList();
            for (Path logFile : logFiles) {
                Optional<LocalDate> fileDate = extractDateFromFilename(logFile.getFileName().toString());
                if (fileDate.isPresent() && fileDate.get().isBefore(cutoffDate)) {
                    Files.delete(logFile);
                    deleted++;
                }
            }
        } catch (IOException e) {
            log.error("Failed to purge log files in {}", jobDir, e);
        }
        return deleted;
    }

    private Optional<LocalDate> extractDateFromFilename(String filename) {
        Matcher matcher = LOG_DATE_PATTERN.matcher(filename);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(matcher.group(1), DateTimeFormatter.BASIC_ISO_DATE));
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date from log filename: {}", filename);
            return Optional.empty();
        }
    }

    private Path getJobLogDir(String jobName) {
        return Paths.get(cacheProperties.getLocation(), BATCH_LOGS_DIR, jobName);
    }
}
