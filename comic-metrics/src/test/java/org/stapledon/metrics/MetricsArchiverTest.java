package org.stapledon.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stapledon.common.util.GsonUtils;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.metrics.dto.GlobalMetrics;
import org.stapledon.metrics.repository.MetricsArchiver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

class MetricsArchiverTest {

    @TempDir
    Path cacheRoot;

    private MetricsArchiver archiver;

    @BeforeEach
    void setUp() {
        archiver = new MetricsArchiver(GsonUtils.createGson(), cacheRoot.toString(), Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void latestArchiveIsEmptyWithoutSnapshots() {
        assertThat(archiver.latestArchive()).isEmpty();
    }

    @Test
    void latestArchiveReadsTheNewestSnapshot() throws IOException {
        archiver.archiveMetrics(snapshot(1), LocalDate.of(2026, 9, 30));
        archiver.archiveMetrics(snapshot(3), LocalDate.of(2026, 10, 2));
        archiver.archiveMetrics(snapshot(2), LocalDate.of(2026, 10, 1));
        Files.writeString(cacheRoot.resolve("metrics-history/notes.json"), "{}");

        assertThat(archiver.latestArchive()).hasValueSatisfying(m -> assertThat(m.getGlobalMetrics().getTotalStorageBytes()).isEqualTo(3));
    }

    @Test
    void latestArchiveIsEmptyWhenTheNewestSnapshotIsUnreadable() throws IOException {
        archiver.archiveMetrics(snapshot(1), LocalDate.of(2026, 9, 30));
        Files.writeString(cacheRoot.resolve("metrics-history/2026-10-01.json"), "{not json");

        assertThat(archiver.latestArchive()).isEmpty();
    }

    private static CombinedMetricsData snapshot(long bytes) {
        return CombinedMetricsData.builder().globalMetrics(GlobalMetrics.builder().totalStorageBytes(bytes).build()).build();
    }
}
