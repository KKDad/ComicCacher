package org.stapledon.metrics.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.stapledon.metrics.config.MetricsProperties;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.metrics.repository.MetricsArchiver;
import org.stapledon.metrics.repository.MetricsRepository;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for scheduled metrics archiving.
 * Creates daily snapshots of combined metrics for historical analysis.
 */
@Slf4j
@ToString
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "comics.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MetricsArchiveService {

    private final MetricsRepository metricsRepository;
    private final MetricsArchiver metricsArchiver;
    private final MetricsProperties metricsProperties;

    /**
     * Archive previous day's metrics.
     * Called by MetricsArchiveJob batch job (scheduled at 6:30 AM EST).
     */
    public void archiveDailyMetrics() {
        try {
            log.info("Starting daily metrics archiving");

            // Archive yesterday's metrics
            LocalDate yesterday = LocalDate.now().minusDays(1);
            CombinedMetricsData metrics = metricsRepository.get();

            if (metrics != null && metrics.getPerComicMetrics() != null && !metrics.getPerComicMetrics().isEmpty()) {
                boolean archived = metricsArchiver.archiveMetrics(metrics, yesterday);

                if (archived) {
                    log.info("Successfully archived metrics for {}", yesterday);

                    // Cleanup old archives
                    int deleted = metricsArchiver.cleanupOldArchives(metricsProperties.getHistoryRetentionDays());
                    if (deleted > 0) {
                        log.info("Cleaned up {} old metric archives", deleted);
                    }
                } else {
                    log.error("Failed to archive metrics for {}", yesterday);
                }
            } else {
                log.warn("No metrics available to archive for {}", yesterday);
            }
        } catch (Exception e) {
            log.error("Failed to archive daily metrics", e);
        }
    }

    /**
     * Manually trigger archiving for a specific date.
     * Useful for backfilling or re-archiving specific dates.
     *
     * @param date Date to archive metrics for
     * @return true if successful, false otherwise
     */
    public boolean archiveMetricsForDate(LocalDate date) {
        try {
            CombinedMetricsData metrics = metricsRepository.get();

            if (metrics != null && metrics.getPerComicMetrics() != null && !metrics.getPerComicMetrics().isEmpty()) {
                boolean archived = metricsArchiver.archiveMetrics(metrics, date);
                if (archived) {
                    log.info("Successfully archived metrics for {}", date);
                    return true;
                } else {
                    log.error("Failed to archive metrics for {}", date);
                    return false;
                }
            } else {
                log.warn("No metrics available to archive for {}", date);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to archive metrics for date {}", date, e);
            return false;
        }
    }
}
