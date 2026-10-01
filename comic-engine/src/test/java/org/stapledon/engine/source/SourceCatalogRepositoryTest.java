package org.stapledon.engine.source;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.util.GsonUtils;
import org.stapledon.engine.source.SourceCatalogRepository.MergeResult;
import org.stapledon.engine.source.SourceCatalogState.Entry;
import org.stapledon.engine.source.SourceCatalogState.SourceEntries;

class SourceCatalogRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-09-28T11:00:00Z");

    @TempDir
    Path cacheRoot;

    private CacheProperties cacheProperties;

    @BeforeEach
    void setUp() {
        cacheProperties = CacheProperties.builder().location(cacheRoot.toString()).build();
    }

    private SourceCatalogRepository repository(Instant now) {
        return new SourceCatalogRepository(cacheProperties, GsonUtils.createGson(), Clock.fixed(now, ZoneOffset.UTC));
    }

    private static SourceCatalogEntry entry(String identifier, String name) {
        return new SourceCatalogEntry(identifier, name, "Author", "https://img.example/" + identifier + ".png", null);
    }

    @Test
    void mergeAddsEntriesAndRecordsTheRefresh() {
        SourceCatalogRepository repository = repository(NOW);

        MergeResult result = repository.merge("gocomics", List.of(entry("a", "A"), entry("b", "B")));

        assertThat(result).isEqualTo(new MergeResult(2, 2, 0, 0));
        SourceEntries stored = repository.find("gocomics").orElseThrow();
        assertThat(stored.getLastRefreshed()).isEqualTo(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(stored.getLastError()).isNull();
        assertThat(stored.getEntries()).containsOnlyKeys("a", "b");
        assertThat(stored.getEntries().get("a").getFirstSeen()).isEqualTo(stored.getLastRefreshed());
    }

    @Test
    void mergeMarksMissingEntriesRemovedAndBringsThemBack() {
        repository(NOW).merge("gocomics", List.of(entry("a", "A"), entry("b", "B")));

        SourceCatalogRepository later = repository(NOW.plusSeconds(3600));
        MergeResult dropped = later.merge("gocomics", List.of(entry("a", "A renamed")));
        assertThat(dropped).isEqualTo(new MergeResult(1, 0, 1, 0));
        Entry b = later.find("gocomics").orElseThrow().getEntries().get("b");
        assertThat(b.getRemovedAt()).isNotNull();
        assertThat(later.find("gocomics").orElseThrow().getEntries().get("a").getName()).isEqualTo("A renamed");

        MergeResult back = later.merge("gocomics", List.of(entry("a", "A"), entry("b", "B")));
        assertThat(back).isEqualTo(new MergeResult(2, 0, 0, 1));
        assertThat(later.find("gocomics").orElseThrow().getEntries().get("b").getRemovedAt()).isNull();
    }

    @Test
    void mergeKeepsTheCatalogStartDate() {
        SourceCatalogRepository repository = repository(NOW);

        repository.merge("comicskingdom", List.of(new SourceCatalogEntry("wannabe", "Wannabe", null, null, LocalDate.of(2024, 1, 14))));

        Entry wannabe = repository.find("comicskingdom").orElseThrow().getEntries().get("wannabe");
        assertThat(wannabe.getStartDate()).isEqualTo(LocalDate.of(2024, 1, 14));
        assertThat(wannabe.getStartCheckedAt()).isNotNull();
    }

    @Test
    void failureKeepsTheCatalog() {
        SourceCatalogRepository repository = repository(NOW);
        repository.merge("gocomics", List.of(entry("a", "A")));

        repository.recordFailure("gocomics", "HTTP 503");

        SourceEntries stored = repository.find("gocomics").orElseThrow();
        assertThat(stored.getLastError()).isEqualTo("HTTP 503");
        assertThat(stored.getEntries()).containsOnlyKeys("a");
    }

    @Test
    void recordStartUpdatesAListedEntryOnly() {
        SourceCatalogRepository repository = repository(NOW);
        repository.merge("freefall", List.of(entry("freefall", "Freefall")));

        repository.recordStart("freefall", "freefall", StartInfo.ofStripNumber(1));
        repository.recordStart("freefall", "unknown", StartInfo.ofStripNumber(5));

        assertThat(repository.find("freefall").orElseThrow().getEntries()).containsOnlyKeys("freefall");
        assertThat(repository.find("freefall").orElseThrow().getEntries().get("freefall").getStartStripNumber()).isEqualTo(1);
    }

    @Test
    void stateSurvivesARestart() throws IOException {
        repository(NOW).merge("gocomics", List.of(entry("a", "A")));
        assertThat(Files.exists(cacheRoot.resolve(SourceCatalogRepository.FILENAME))).isTrue();

        SourceCatalogRepository reloaded = repository(NOW);

        assertThat(reloaded.find("gocomics").orElseThrow().getEntries().get("a").getName()).isEqualTo("A");
    }

    @Test
    void corruptFileStartsEmpty() throws IOException {
        Files.writeString(cacheRoot.resolve(SourceCatalogRepository.FILENAME), "{not json");

        assertThat(repository(NOW).find("gocomics")).isEmpty();
    }

    @Test
    void findReturnsACopy() {
        SourceCatalogRepository repository = repository(NOW);
        repository.merge("gocomics", List.of(entry("a", "A")));

        repository.find("gocomics").orElseThrow().getEntries().get("a").setName("changed");

        assertThat(repository.find("gocomics").orElseThrow().getEntries().get("a").getName()).isEqualTo("A");
    }
}
