package org.stapledon.metrics.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.stapledon.metrics.config.MetricsProperties;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.metrics.repository.MetricsArchiver;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for scheduled metrics archiving.
 * Creates daily snapshots of combined metrics for historical analysis.
 * Metrics are built on demand from storage and access data at archive time.
 */
@Slf4j
@ToString
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "comics.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MetricsArchiveService {

    private final MetricsUpdateService metricsUpdateService;
    private final MetricsArchiver metricsArchiver;
    private final MetricsProperties metricsProperties;

    /**
     * Archive current combined metrics under the given date, then prune archives
     * older than the configured retention. Called by MetricsArchiveJob.
     *
     * @param date Date to archive metrics for
     * @return true if successful, false otherwise
     */
    public boolean archiveMetricsForDate(LocalDate date) {
        try {
            CombinedMetricsData metrics = metricsUpdateService.buildCombinedMetrics();

            if (metrics != null && metrics.getPerComicMetrics() != null && !metrics.getPerComicMetrics().isEmpty()) {
                boolean archived = metricsArchiver.archiveMetrics(metrics, date);
                if (archived) {
                    log.info("Successfully archived metrics for {}", date);

                    int deleted = metricsArchiver.cleanupOldArchives(metricsProperties.getHistoryRetentionDays());
                    if (deleted > 0) {
                        log.info("Cleaned up {} old metric archives", deleted);
                    }
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
