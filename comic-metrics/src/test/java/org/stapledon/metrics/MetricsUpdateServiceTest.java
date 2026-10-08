package org.stapledon.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.stapledon.common.util.GsonUtils;
import org.stapledon.metrics.collector.AccessMetricsCollector;
import org.stapledon.metrics.collector.StorageMetricsCollector;
import org.stapledon.metrics.dto.AccessMetricsData;
import org.stapledon.metrics.dto.AccessMetricsData.ComicAccessMetrics;
import org.stapledon.metrics.dto.CombinedMetricsData;
import org.stapledon.metrics.dto.GlobalMetrics;
import org.stapledon.metrics.repository.AccessMetricsRepository;
import org.stapledon.metrics.repository.MetricsArchiver;
import org.stapledon.metrics.service.MetricsUpdateService;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MetricsUpdateServiceTest {

    @TempDir
    Path cacheRoot;

    private StorageMetricsCollector storage;
    private MetricsArchiver archiver;
    private AccessMetricsRepository accessRepository;
    private MetricsUpdateService service;

    @BeforeEach
    void setUp() throws IOException {
        Path year = Files.createDirectories(cacheRoot.resolve("Garfield/2026"));
        Files.write(year.resolve("2026-09-30.png"), new byte[100]);
        Files.write(year.resolve("2026-10-01.png"), new byte[300]);

        Gson gson = GsonUtils.createGson();
        storage = new StorageMetricsCollector(cacheRoot.toString());
        accessRepository = new AccessMetricsRepository(gson, cacheRoot.toString());
        archiver = new MetricsArchiver(gson, cacheRoot.toString(), Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneOffset.UTC));
        service = new MetricsUpdateService(new AccessMetricsCollector(accessRepository, 50), storage, accessRepository, archiver);
    }

    @Test
    void beforeTheFirstScanItServesTheLatestSnapshotWithoutScanning() {
        CombinedMetricsData snapshot = CombinedMetricsData.builder()
                .globalMetrics(GlobalMetrics.builder().totalStorageBytes(42).build())
                .lastUpdated(OffsetDateTime.parse("2026-10-02T12:00:00Z"))
                .build();
        archiver.archiveMetrics(snapshot, LocalDate.of(2026, 10, 2));

        CombinedMetricsData metrics = service.buildCombinedMetrics();

        assertThat(metrics.getGlobalMetrics().getTotalStorageBytes()).isEqualTo(42);
        assertThat(metrics.getLastUpdated()).isEqualTo(OffsetDateTime.parse("2026-10-02T12:00:00Z"));
        assertThat(storage.currentStats()).isEmpty();
    }

    @Test
    void withNoSnapshotItScans() {
        CombinedMetricsData metrics = service.buildCombinedMetrics();

        assertThat(metrics.getGlobalMetrics().getTotalStorageBytes()).isEqualTo(400);
        assertThat(storage.currentStats()).isPresent();
    }

    @Test
    void afterAScanItUsesTheScanAndCountsImagesPerYear() {
        archiver.archiveMetrics(CombinedMetricsData.builder().globalMetrics(GlobalMetrics.builder().totalStorageBytes(42).build()).build(),
                LocalDate.of(2026, 10, 1));
        storage.updateStats();

        CombinedMetricsData metrics = service.buildCombinedMetrics();

        assertThat(metrics.getGlobalMetrics().getTotalStorageBytes()).isEqualTo(400);
        assertThat(metrics.getPerComicMetrics().get("Garfield").getYearlyStorage().get("2026").getImageCount()).isEqualTo(2);
    }

    @Test
    void accessUnderTheDisplayNameJoinsStorageUnderTheDirectoryName() throws IOException {
        Path year = Files.createDirectories(cacheRoot.resolve("MotherGoose&Grimm/2026"));
        Files.write(year.resolve("2026-10-01.png"), new byte[50]);
        accessRepository.save(AccessMetricsData.builder().comicMetrics(Map.of(
                "Mother Goose & Grimm", ComicAccessMetrics.builder().comicName("Mother Goose & Grimm").accessCount(6).totalAccessTimeMs(60)
                        .lastAccess("2026-10-02T10:00:00Z").build(),
                "MotherGoose&Grimm", ComicAccessMetrics.builder().comicName("MotherGoose&Grimm").accessCount(2).totalAccessTimeMs(20)
                        .lastAccess("2026-09-01T10:00:00Z").build(),
                "Sherman's Lagoon", ComicAccessMetrics.builder().comicName("Sherman's Lagoon").accessCount(3).build())).build());
        storage.updateStats();

        Map<String, CombinedMetricsData.ComicCombinedMetrics> perComic = service.buildCombinedMetrics().getPerComicMetrics();

        assertThat(perComic).containsOnlyKeys("Garfield", "MotherGoose&Grimm", "Sherman'sLagoon");
        CombinedMetricsData.ComicCombinedMetrics grimm = perComic.get("MotherGoose&Grimm");
        assertThat(grimm.getComicName()).isEqualTo("Mother Goose & Grimm");
        assertThat(grimm.getImageCount()).isEqualTo(1);
        assertThat(grimm.getAccessCount()).isEqualTo(8);
        assertThat(grimm.getAverageAccessTime()).isEqualTo(10.0);
        assertThat(grimm.getLastAccess()).isEqualTo("2026-10-02T10:00:00Z");
        assertThat(perComic.get("Sherman'sLagoon").getComicName()).isEqualTo("Sherman's Lagoon");
        assertThat(perComic.get("Sherman'sLagoon").getAccessCount()).isEqualTo(3);
    }
}
