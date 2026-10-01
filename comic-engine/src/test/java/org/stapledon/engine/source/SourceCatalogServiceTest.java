package org.stapledon.engine.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.StartSource;
import org.stapledon.common.util.GsonUtils;
import org.stapledon.engine.downloader.DownloaderFacade;
import org.stapledon.engine.downloader.IndexedComicDownloaderStrategy;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.engine.source.SourceCatalogService.AddResult;
import org.stapledon.engine.source.SourceCatalogService.CatalogRow;
import org.stapledon.engine.source.SourceCatalogService.DetectionOutcome;
import org.stapledon.engine.source.SourceCatalogService.RefreshResult;
import org.stapledon.engine.source.SourceCatalogService.RefreshStatus;

class SourceCatalogServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-28T11:00:00Z");

    @TempDir
    Path cacheRoot;

    private final StubSource daily = new StubSource("daily");
    private final StubSource numbered = new StubSource("numbered", mock(IndexedComicDownloaderStrategy.class));
    private final Map<Integer, ComicItem> stored = new ConcurrentHashMap<>();
    private ManagementFacade comics;
    private CatalogThumbnailService thumbnails;
    private SourceCatalogRepository repository;
    private SourceCatalogService service;

    @BeforeEach
    void setUp() {
        comics = mock(ManagementFacade.class);
        when(comics.getAllComics()).thenAnswer(_ -> new ArrayList<>(stored.values()));
        when(comics.getComic(anyInt())).thenAnswer(inv -> Optional.ofNullable(stored.get(inv.<Integer>getArgument(0))));
        when(comics.createComic(any())).thenAnswer(inv -> {
            ComicItem comic = inv.<ComicItem>getArgument(0).toBuilder().id(stored.size() + 1).build();
            stored.put(comic.getId(), comic);
            return Optional.of(comic);
        });
        when(comics.updateComic(anyInt(), any())).thenAnswer(inv -> {
            stored.put(inv.getArgument(0), inv.getArgument(1));
            return Optional.of(inv.getArgument(1));
        });

        SourceRegistry registry = new SourceRegistry(List.of(daily, numbered), mock(DownloaderFacade.class));
        repository = new SourceCatalogRepository(CacheProperties.builder().location(cacheRoot.toString()).build(), GsonUtils.createGson(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        thumbnails = mock(CatalogThumbnailService.class);
        ComicValidator validator = new ComicValidator(registry, comics, Clock.fixed(NOW, ZoneOffset.UTC));
        service = new SourceCatalogService(registry, repository, comics, validator, thumbnails, Runnable::run, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void listInCatalog(String identifier, String name, LocalDate start) {
        daily.catalogEntries.add(new SourceCatalogEntry(identifier, name, "Someone", null, start));
    }

    @Test
    void refreshStoresTheCatalog() {
        listInCatalog("one", "One", null);
        listInCatalog("two", "Two", null);

        RefreshResult result = service.refresh("daily");

        assertThat(result.status()).isEqualTo(RefreshStatus.REFRESHED);
        assertThat(result.changes().added()).isEqualTo(2);
        assertThat(service.summary("daily").orElseThrow().catalogCount()).isEqualTo(2);
        assertThat(service.isStale("daily", Duration.ofDays(7))).isFalse();
    }

    @Test
    void failedRefreshKeepsTheCatalogAndRecordsTheError() {
        listInCatalog("one", "One", null);
        service.refresh("daily");
        daily.catalogFailure = new IOException("HTTP 503");

        RefreshResult result = service.refresh("daily");

        assertThat(result.status()).isEqualTo(RefreshStatus.FAILED);
        assertThat(service.summary("daily").orElseThrow().lastError()).isEqualTo("HTTP 503");
        assertThat(service.summary("daily").orElseThrow().catalogCount()).isEqualTo(1);
    }

    @Test
    void unknownSourceAndNeverReadCatalogs() {
        assertThat(service.refresh("nowhere").status()).isEqualTo(RefreshStatus.UNKNOWN_SOURCE);
        assertThat(service.isStale("daily", Duration.ofDays(7))).isTrue();
    }

    @Test
    void catalogJoinsConfiguredComicsAndListsOrphans() {
        listInCatalog("one", "One", null);
        listInCatalog("two", "Two", null);
        service.refresh("daily");
        stored.put(1, ComicItem.builder().id(1).name("One").source("daily").build());
        stored.put(2, ComicItem.builder().id(2).name("Gone").source("daily").sourceIdentifier("gone").build());

        List<CatalogRow> rows = service.catalog("daily");

        assertThat(rows).extracting(CatalogRow::identifier).containsExactly("one", "two");
        assertThat(rows.getFirst().comic().getId()).isEqualTo(1);
        assertThat(rows.getLast().comic()).isNull();
        assertThat(service.orphans("daily")).extracting(ComicItem::getName).containsExactly("Gone");
        assertThat(service.summary("daily").orElseThrow().configuredCount()).isEqualTo(2);
    }

    @Test
    void addFromCatalogUsesTheCatalogsDetailsAndStart() {
        listInCatalog("one", "One", LocalDate.of(2001, 2, 3));
        service.refresh("daily");

        AddResult result = service.addFromCatalog("daily", "one", true, false);

        assertThat(result.problems()).isEmpty();
        ComicItem comic = result.comic();
        assertThat(comic.getName()).isEqualTo("One");
        assertThat(comic.getAuthor()).isEqualTo("Someone");
        assertThat(comic.getSource()).isEqualTo("daily");
        assertThat(comic.getSourceIdentifier()).isEqualTo("one");
        assertThat(comic.isActive()).isTrue();
        assertThat(comic.isEnabled()).isFalse();
        assertThat(comic.getSourceStartDate()).isEqualTo(LocalDate.of(2001, 2, 3));
        assertThat(comic.getStartSource()).isEqualTo(StartSource.DETECTED);
        verify(comics).fetchAvatar(comic.getId());
    }

    @Test
    void addFromCatalogDetectsTheStartWhenTheCatalogHasNone() {
        listInCatalog("one", "One", null);
        daily.starts.put("one", StartInfo.ofDate(LocalDate.of(1999, 1, 1)));
        service.refresh("daily");

        ComicItem comic = service.addFromCatalog("daily", "one", true, true).comic();

        assertThat(stored.get(comic.getId()).getSourceStartDate()).isEqualTo(LocalDate.of(1999, 1, 1));
        assertThat(stored.get(comic.getId()).getStartSource()).isEqualTo(StartSource.DETECTED);
    }

    @Test
    void addFromCatalogStartsNumberedComicsAtOne() {
        numbered.catalogEntries.add(new SourceCatalogEntry("freefall", "Freefall", null, null, null));
        service.refresh("numbered");

        ComicItem comic = service.addFromCatalog("numbered", "freefall", true, true).comic();

        assertThat(comic.getFirstStripNumber()).isEqualTo(1);
    }

    @Test
    void addFromCatalogRefusesUnknownAndDuplicateComics() {
        listInCatalog("one", "One", null);
        service.refresh("daily");

        assertThat(service.addFromCatalog("daily", "missing", true, true).problems()).extracting(p -> p.field()).containsExactly("identifier");
        service.addFromCatalog("daily", "one", true, true);
        assertThat(service.addFromCatalog("daily", "one", true, true).problems()).isNotEmpty();
        assertThat(stored).hasSize(1);
    }

    @Test
    void addFromCatalogCopiesACachedThumbnailAsTheAvatar() {
        listInCatalog("one", "One", null);
        service.refresh("daily");
        byte[] image = {1, 2, 3};
        when(thumbnails.load("daily", "one")).thenReturn(Optional.of(new CatalogThumbnailService.Thumbnail(image, "image/png")));
        when(comics.saveAvatar(anyInt(), eq(image))).thenReturn(true);

        ComicItem comic = service.addFromCatalog("daily", "one", true, true).comic();

        verify(comics).saveAvatar(comic.getId(), image);
        verify(comics, never()).fetchAvatar(anyInt());
    }

    @Test
    void detectionNeverOverwritesAnAdminsStart() {
        stored.put(1, ComicItem.builder().id(1).name("One").source("daily").sourceIdentifier("one")
                .sourceStartDate(LocalDate.of(2010, 1, 1)).startSource(StartSource.MANUAL).build());
        listInCatalog("one", "One", null);
        service.refresh("daily");
        daily.starts.put("one", StartInfo.ofDate(LocalDate.of(2005, 5, 5)));

        DetectionOutcome outcome = service.detectStart(stored.get(1));

        assertThat(outcome).isEqualTo(DetectionOutcome.RECORDED);
        assertThat(stored.get(1).getSourceStartDate()).isEqualTo(LocalDate.of(2010, 1, 1));
        assertThat(service.reportedStart(stored.get(1))).contains(StartInfo.ofDate(LocalDate.of(2005, 5, 5)));
    }

    @Test
    void refreshGivesConfiguredComicsTheCatalogStart() {
        stored.put(1, ComicItem.builder().id(1).name("One").source("daily").sourceIdentifier("one").build());
        listInCatalog("one", "One", LocalDate.of(2001, 2, 3));

        service.refresh("daily");

        assertThat(stored.get(1).getSourceStartDate()).isEqualTo(LocalDate.of(2001, 2, 3));
        assertThat(stored.get(1).getStartSource()).isEqualTo(StartSource.DETECTED);
    }

    @Test
    void detectMissingStartsIsLimitedPerSource() {
        for (int i = 1; i <= 3; i++) {
            stored.put(i, ComicItem.builder().id(i).name("C" + i).source("daily").sourceIdentifier("c" + i).build());
            daily.starts.put("c" + i, StartInfo.ofDate(LocalDate.of(2000, 1, i)));
        }

        assertThat(service.detectMissingStarts(2)).isEqualTo(2);
        assertThat(stored.values().stream().filter(c -> c.getSourceStartDate() != null)).hasSize(2);
    }
}
