package org.stapledon.metrics.repository;

import com.google.gson.Gson;

import org.springframework.beans.factory.annotation.Qualifier;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.common.config.CacheProperties;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for archiving daily metrics snapshots.
 * Saves historical snapshots to metrics-history/ directory and cleans up old archives.
 * Configured as a bean in MetricsConfiguration when metrics are enabled.
 */
@Slf4j
@RequiredArgsConstructor
public class MetricsArchiver {

    @Qualifier("gsonWithLocalDate")
    private final Gson gson;
    private final CacheProperties cacheProperties;

    private static final String HISTORY_DIRECTORY = "metrics-history";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int DEFAULT_RETENTION_DAYS = 90;

    /**
     * Archive combined metrics for a specific date.
     *
     * @param metrics Combined metrics to archive
     * @param date Date of the snapshot
     * @return true if successful, false otherwise
     */
    public boolean archiveMetrics(CombinedMetricsData metrics, LocalDate date) {
        try {
            Path historyDir = Paths.get(cacheProperties.getLocation(), HISTORY_DIRECTORY);
            if (!Files.exists(historyDir)) {
                Files.createDirectories(historyDir);
            }

            String filename = date.format(DATE_FORMATTER) + ".json";
            Path filePath = historyDir.resolve(filename);

            try (Writer writer = new FileWriter(filePath.toFile())) {
                gson.toJson(metrics, writer);
                writer.flush();
                log.info("Archived metrics snapshot for {}", date);
                return true;
            }
        } catch (IOException e) {
            log.error("Failed to archive metrics for date {}", date, e);
            return false;
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
            Path historyDir = Paths.get(cacheProperties.getLocation(), HISTORY_DIRECTORY);
            if (!Files.exists(historyDir)) {
                log.debug("History directory does not exist, nothing to clean up");
                return 0;
            }

            LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
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
                        log.warn("Could not parse date from filename: {}", filename);
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

    /**
     * Get list of available archived dates.
     *
     * @return List of dates for which archives exist
     */
    public List<LocalDate> getAvailableArchives() {
        try {
            Path historyDir = Paths.get(cacheProperties.getLocation(), HISTORY_DIRECTORY);
            if (!Files.exists(historyDir)) {
                return Collections.emptyList();
            }

            List<LocalDate> dates = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(historyDir, "*.json")) {
                for (Path file : stream) {
                    String filename = file.getFileName().toString();
                    String dateStr = filename.replace(".json", "");

                    try {
                        LocalDate fileDate = LocalDate.parse(dateStr, DATE_FORMATTER);
                        dates.add(fileDate);
                    } catch (Exception e) {
                        log.warn("Could not parse date from filename: {}", filename);
                    }
                }
            }

            Collections.sort(dates);
            return dates;
        } catch (IOException e) {
            log.error("Failed to list archived metrics", e);
            return Collections.emptyList();
        }
    }
}
