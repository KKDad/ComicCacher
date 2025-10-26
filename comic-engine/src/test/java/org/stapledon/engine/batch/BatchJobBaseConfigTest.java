package org.stapledon.engine.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals("America/Toronto", BatchJobBaseConfig.BATCH_TIMEZONE);
    }

    @Test
    void shouldHaveValidCronSchedules() {
        // Assert - all cron schedules should include timezone
        assertTrue(BatchJobBaseConfig.CronSchedules.COMIC_DOWNLOAD.contains("America/Toronto"));
        assertTrue(BatchJobBaseConfig.CronSchedules.RECONCILIATION.contains("America/Toronto"));
        assertTrue(BatchJobBaseConfig.CronSchedules.METRICS_ARCHIVE.contains("America/Toronto"));
        assertTrue(BatchJobBaseConfig.CronSchedules.IMAGE_BACKFILL.contains("America/Toronto"));
        assertTrue(BatchJobBaseConfig.CronSchedules.RECORD_PURGE.contains("America/Toronto"));
    }

    @Test
    void shouldHaveValidPropertyKeys() {
        // Assert
        assertEquals("batch.comic-download.enabled", BatchJobBaseConfig.PropertyKeys.COMIC_DOWNLOAD_ENABLED);
        assertEquals("batch.comic-download.cron", BatchJobBaseConfig.PropertyKeys.COMIC_DOWNLOAD_CRON);
        assertEquals("batch.reconciliation.enabled", BatchJobBaseConfig.PropertyKeys.RECONCILIATION_ENABLED);
        assertEquals("batch.reconciliation.cron", BatchJobBaseConfig.PropertyKeys.RECONCILIATION_CRON);
        assertEquals("batch.metrics-archive.enabled", BatchJobBaseConfig.PropertyKeys.METRICS_ARCHIVE_ENABLED);
        assertEquals("batch.metrics-archive.cron", BatchJobBaseConfig.PropertyKeys.METRICS_ARCHIVE_CRON);
        assertEquals("batch.image-backfill.enabled", BatchJobBaseConfig.PropertyKeys.IMAGE_BACKFILL_ENABLED);
        assertEquals("batch.image-backfill.cron", BatchJobBaseConfig.PropertyKeys.IMAGE_BACKFILL_CRON);
        assertEquals("batch.metrics-update.enabled", BatchJobBaseConfig.PropertyKeys.METRICS_UPDATE_ENABLED);
        assertEquals("batch.metrics-update.fixed-delay", BatchJobBaseConfig.PropertyKeys.METRICS_UPDATE_DELAY);
        assertEquals("batch.record-purge.enabled", BatchJobBaseConfig.PropertyKeys.RECORD_PURGE_ENABLED);
        assertEquals("batch.record-purge.cron", BatchJobBaseConfig.PropertyKeys.RECORD_PURGE_CRON);
    }

    @Test
    void shouldHaveValidCronExpressionForComicDownload() {
        // Assert - should be daily at 6:00 AM
        String cron = BatchJobBaseConfig.CronSchedules.COMIC_DOWNLOAD;
        assertTrue(cron.startsWith("0 0 6"));
        assertTrue(cron.endsWith("America/Toronto"));
    }

    @Test
    void shouldHaveValidCronExpressionForReconciliation() {
        // Assert - should be daily at 6:15 AM
        String cron = BatchJobBaseConfig.CronSchedules.RECONCILIATION;
        assertTrue(cron.startsWith("0 15 6"));
        assertTrue(cron.endsWith("America/Toronto"));
    }

    @Test
    void shouldHaveValidCronExpressionForMetricsArchive() {
        // Assert - should be daily at 6:30 AM
        String cron = BatchJobBaseConfig.CronSchedules.METRICS_ARCHIVE;
        assertTrue(cron.startsWith("0 30 6"));
        assertTrue(cron.endsWith("America/Toronto"));
    }

    @Test
    void shouldHaveValidCronExpressionForImageBackfill() {
        // Assert - should be daily at 6:30 AM
        String cron = BatchJobBaseConfig.CronSchedules.IMAGE_BACKFILL;
        assertTrue(cron.startsWith("0 30 6"));
        assertTrue(cron.endsWith("America/Toronto"));
    }

    @Test
    void shouldHaveValidCronExpressionForRecordPurge() {
        // Assert - should be daily at 6:45 AM
        String cron = BatchJobBaseConfig.CronSchedules.RECORD_PURGE;
        assertTrue(cron.startsWith("0 45 6"));
        assertTrue(cron.endsWith("America/Toronto"));
    }

    @Test
    void shouldInitializeSuccessfully() {
        // Act
        BatchJobBaseConfig newConfig = new BatchJobBaseConfig();

        // Assert
        assertNotNull(newConfig);
    }

    @Test
    void shouldHaveValidToString() {
        // Act
        String toString = config.toString();

        // Assert
        assertNotNull(toString);
    }
}
