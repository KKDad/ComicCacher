package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BatchJobBaseConfigTest {

    private BatchJobBaseConfig config;

    @BeforeEach
    void setUp() {
        config = new BatchJobBaseConfig();
    }

    @Test
    void shouldHaveCorrectTimezone() {
        // Assert
        assertThat(BatchJobBaseConfig.BATCH_TIMEZONE).isEqualTo("America/Toronto");
    }

    @Test
    void shouldHaveValidCronSchedules() {
        // Assert - all cron schedules should include timezone
        assertThat(BatchJobBaseConfig.CronSchedules.COMIC_DOWNLOAD.contains("America/Toronto")).isTrue();
        assertThat(BatchJobBaseConfig.CronSchedules.RECONCILIATION.contains("America/Toronto")).isTrue();
        assertThat(BatchJobBaseConfig.CronSchedules.METRICS_ARCHIVE.contains("America/Toronto")).isTrue();
        assertThat(BatchJobBaseConfig.CronSchedules.IMAGE_BACKFILL.contains("America/Toronto")).isTrue();
        assertThat(BatchJobBaseConfig.CronSchedules.RECORD_PURGE.contains("America/Toronto")).isTrue();
    }

    @Test
    void shouldHaveValidPropertyKeys() {
        // Assert
        assertThat(BatchJobBaseConfig.PropertyKeys.COMIC_DOWNLOAD_ENABLED).isEqualTo("batch.comic-download.enabled");
        assertThat(BatchJobBaseConfig.PropertyKeys.COMIC_DOWNLOAD_CRON).isEqualTo("batch.comic-download.cron");
        assertThat(BatchJobBaseConfig.PropertyKeys.RECONCILIATION_ENABLED).isEqualTo("batch.reconciliation.enabled");
        assertThat(BatchJobBaseConfig.PropertyKeys.RECONCILIATION_CRON).isEqualTo("batch.reconciliation.cron");
        assertThat(BatchJobBaseConfig.PropertyKeys.METRICS_ARCHIVE_ENABLED).isEqualTo("batch.metrics-archive.enabled");
        assertThat(BatchJobBaseConfig.PropertyKeys.METRICS_ARCHIVE_CRON).isEqualTo("batch.metrics-archive.cron");
        assertThat(BatchJobBaseConfig.PropertyKeys.IMAGE_BACKFILL_ENABLED).isEqualTo("batch.image-backfill.enabled");
        assertThat(BatchJobBaseConfig.PropertyKeys.IMAGE_BACKFILL_CRON).isEqualTo("batch.image-backfill.cron");
        assertThat(BatchJobBaseConfig.PropertyKeys.RECORD_PURGE_ENABLED).isEqualTo("batch.record-purge.enabled");
        assertThat(BatchJobBaseConfig.PropertyKeys.RECORD_PURGE_CRON).isEqualTo("batch.record-purge.cron");
    }

    @Test
    void shouldHaveValidCronExpressionForComicDownload() {
        // Assert - should be daily at 6:00 AM
        String cron = BatchJobBaseConfig.CronSchedules.COMIC_DOWNLOAD;
        assertThat(cron.startsWith("0 0 6")).isTrue();
        assertThat(cron.endsWith("America/Toronto")).isTrue();
    }

    @Test
    void shouldHaveValidCronExpressionForReconciliation() {
        // Assert - should be daily at 6:15 AM
        String cron = BatchJobBaseConfig.CronSchedules.RECONCILIATION;
        assertThat(cron.startsWith("0 15 6")).isTrue();
        assertThat(cron.endsWith("America/Toronto")).isTrue();
    }

    @Test
    void shouldHaveValidCronExpressionForMetricsArchive() {
        // Assert - should be daily at 6:30 AM
        String cron = BatchJobBaseConfig.CronSchedules.METRICS_ARCHIVE;
        assertThat(cron.startsWith("0 30 6")).isTrue();
        assertThat(cron.endsWith("America/Toronto")).isTrue();
    }

    @Test
    void shouldHaveValidCronExpressionForImageBackfill() {
        // Assert - should be daily at 6:30 AM
        String cron = BatchJobBaseConfig.CronSchedules.IMAGE_BACKFILL;
        assertThat(cron.startsWith("0 30 6")).isTrue();
        assertThat(cron.endsWith("America/Toronto")).isTrue();
    }

    @Test
    void shouldHaveValidCronExpressionForRecordPurge() {
        // Assert - should be daily at 6:45 AM
        String cron = BatchJobBaseConfig.CronSchedules.RECORD_PURGE;
        assertThat(cron.startsWith("0 45 6")).isTrue();
        assertThat(cron.endsWith("America/Toronto")).isTrue();
    }

    @Test
    void shouldInitializeSuccessfully() {
        // Act
        BatchJobBaseConfig newConfig = new BatchJobBaseConfig();

        // Assert
        assertThat(newConfig).isNotNull();
    }

    @Test
    void shouldHaveValidToString() {
        // Act
        String toString = config.toString();

        // Assert
        assertThat(toString).isNotNull();
    }
}
