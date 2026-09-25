package org.stapledon.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.stapledon.metrics.config.MetricsProperties;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.metrics.repository.MetricsArchiver;
import org.stapledon.metrics.service.MetricsArchiveService;
import org.stapledon.metrics.service.MetricsUpdateService;

import java.time.LocalDate;
import java.util.Map;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for MetricsArchiveService.
 */
@ExtendWith(MockitoExtension.class)
class MetricsArchiveServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 24);

    @Mock
    private MetricsUpdateService metricsUpdateService;

    @Mock
    private MetricsArchiver metricsArchiver;

    private MetricsArchiveService service;

    @BeforeEach
    void setUp() {
        MetricsProperties properties = MetricsProperties.builder().historyRetentionDays(90).build();
        service = new MetricsArchiveService(metricsUpdateService, metricsArchiver, properties);
    }

    @Test
    void archivesFreshlyBuiltMetricsAndPrunesOldArchives() {
        CombinedMetricsData metrics = metricsFor("Garfield");
        when(metricsUpdateService.buildCombinedMetrics()).thenReturn(metrics);
        when(metricsArchiver.archiveMetrics(metrics, DATE)).thenReturn(true);

        assertThat(service.archiveMetricsForDate(DATE)).isTrue();

        verify(metricsArchiver).archiveMetrics(metrics, DATE);
        verify(metricsArchiver).cleanupOldArchives(90);
    }

    @Test
    void returnsFalseWithoutArchivingWhenNoComicMetrics() {
        when(metricsUpdateService.buildCombinedMetrics()).thenReturn(CombinedMetricsData.builder().build());

        assertThat(service.archiveMetricsForDate(DATE)).isFalse();

        verify(metricsArchiver, never()).archiveMetrics(any(), any());
        verify(metricsArchiver, never()).cleanupOldArchives(anyInt());
    }

    @Test
    void returnsFalseAndSkipsCleanupWhenArchiveFails() {
        CombinedMetricsData metrics = metricsFor("Garfield");
        when(metricsUpdateService.buildCombinedMetrics()).thenReturn(metrics);
        when(metricsArchiver.archiveMetrics(metrics, DATE)).thenReturn(false);

        assertThat(service.archiveMetricsForDate(DATE)).isFalse();

        verify(metricsArchiver, never()).cleanupOldArchives(anyInt());
    }

    private static CombinedMetricsData metricsFor(String comicName) {
        return CombinedMetricsData.builder()
                .perComicMetrics(Map.of(comicName,
                        CombinedMetricsData.ComicCombinedMetrics.builder().comicName(comicName).imageCount(3).build()))
                .build();
    }
}
