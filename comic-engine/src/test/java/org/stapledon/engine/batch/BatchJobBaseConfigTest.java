package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BatchJobBaseConfigTest {

    @Test
    void shouldHaveValidPropertyKeys() {
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
    void shouldHaveKnownJobs() {
        assertThat(BatchJobBaseConfig.KNOWN_JOBS).isNotEmpty();
        assertThat(BatchJobBaseConfig.KNOWN_JOBS).contains("ComicDownloadJob", "MetricsArchiveJob");
    }
}
