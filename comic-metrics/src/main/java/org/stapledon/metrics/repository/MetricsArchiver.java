package org.stapledon.metrics.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.stapledon.common.config.CacheLayout;
import org.stapledon.common.util.NfsFileOperations;
import org.stapledon.metrics.dto.CombinedMetricsData;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for archiving daily metrics snapshots.
 * Saves historical snapshots to metrics-history/ directory and cleans up old
 * archives.
 * Configured as a bean in MetricsConfiguration when metrics are enabled.
 */
@Slf4j
@ToString
public class MetricsArchiver {

    private final Gson gson;
    private final String cacheLocation;
    private final Clock clock;

    public MetricsArchiver(
            @Qualifier("gsonWithLocalDate") Gson gson,
            @Qualifier("cacheLocation") String cacheLocation,
            Clock clock) {
        this.gson = gson;
        this.cacheLocation = cacheLocation;
        this.clock = clock;
    }

    /** Archive directory under the cache root; not a comic, so storage scans skip it. */
    public static final String HISTORY_DIRECTORY = CacheLayout.METRICS_HISTORY_DIRECTORY;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int DEFAULT_RETENTION_DAYS = 90;

    /**
     * Archive combined metrics for a specific date.
     *
     * @param metrics Combined metrics to archive
     * @param date    Date of the snapshot
     * @return true if successful, false otherwise
     */
    public boolean archiveMetrics(CombinedMetricsData metrics, LocalDate date) {
        try {
            Path historyDir = Paths.get(cacheLocation, HISTORY_DIRECTORY);
            if (!Files.exists(historyDir)) {
                Files.createDirectories(historyDir);
            }

            String filename = date.format(DATE_FORMATTER) + ".json";
            Path filePath = historyDir.resolve(filename);

            NfsFileOperations.atomicWrite(filePath, gson.toJson(metrics));
            log.info("Archived metrics snapshot for {}", date);
            return true;
        } catch (IOException e) {
            log.error("Failed to archive metrics for date {}", date, e);
            return false;
        }
    }

    /**
     * The newest archived snapshot, or empty when there is none or it can't be read.
     */
    public Optional<CombinedMetricsData> latestArchive() {
        Path historyDir = Paths.get(cacheLocation, HISTORY_DIRECTORY);
        if (!Files.isDirectory(historyDir)) {
            return Optional.empty();
        }

        Path latest = null;
        LocalDate latestDate = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(historyDir, "*.json")) {
            for (Path file : stream) {
                try {
                    LocalDate fileDate = LocalDate.parse(file.getFileName().toString().replace(".json", ""), DATE_FORMATTER);
                    if (latestDate == null || fileDate.isAfter(latestDate)) {
                        latestDate = fileDate;
                        latest = file;
                    }
                } catch (DateTimeParseException _) {
                    // Not a snapshot
                }
            }
        } catch (IOException e) {
            log.warn("Couldn't list metrics archives in {}: {}", historyDir, e.toString());
            return Optional.empty();
        }
        if (latest == null) {
            return Optional.empty();
        }

        try (Reader reader = Files.newBufferedReader(latest)) {
            return Optional.ofNullable(gson.fromJson(reader, CombinedMetricsData.class));
        } catch (IOException | RuntimeException e) {
            log.warn("Couldn't read metrics archive {}: {}", latest, e.toString());
            return Optional.empty();
        }
    }

    /**
     * Clean up old archived metrics beyond the retention period.
     *
     * @param retentionDays Number of days to retain
     * @return Number of files deleted
     */
    public int cleanupOldArchives(int retentionDays) {
        try {
            Path historyDir = Paths.get(cacheLocation, HISTORY_DIRECTORY);
            if (!Files.exists(historyDir)) {
                log.debug("History directory does not exist, nothing to clean up");
                return 0;
            }

            LocalDate cutoffDate = LocalDate.now(clock).minusDays(retentionDays);
            int deletedCount = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(historyDir, "*.json")) {
                for (Path file : stream) {
                    String filename = file.getFileName().toString();
                    String dateStr = filename.replace(".json", "");

                    try {
                        LocalDate fileDate = LocalDate.parse(dateStr, DATE_FORMATTER);
                        if (fileDate.isBefore(cutoffDate)) {
                            Files.delete(file);
                            deletedCount++;
                            log.debug("Deleted old metrics archive: {}", filename);
                        }
                    } catch (Exception e) {
                        log.warn("Skipping metrics archive {}: {}", file, e.toString());
                    }
                }
            }

            if (deletedCount > 0) {
                log.info("Cleaned up {} old metrics archives (retention: {} days)", deletedCount, retentionDays);
            }

            return deletedCount;
        } catch (IOException e) {
            log.error("Failed to cleanup old metrics archives", e);
            return 0;
        }
    }

    /**
     * Clean up old archived metrics using default retention period.
     *
     * @return Number of files deleted
     */
    public int cleanupOldArchives() {
        return cleanupOldArchives(DEFAULT_RETENTION_DAYS);
    }
}
