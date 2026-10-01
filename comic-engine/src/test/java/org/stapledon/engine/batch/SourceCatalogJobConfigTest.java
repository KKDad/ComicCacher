package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.stapledon.engine.batch.config.SourceCatalogJobConfig;
import org.stapledon.engine.source.CatalogThumbnailService;
import org.stapledon.engine.source.ComicSource;
import org.stapledon.engine.source.SourceCatalog;
import org.stapledon.engine.source.SourceCatalogService;
import org.stapledon.engine.source.SourceCatalogService.RefreshResult;
import org.stapledon.engine.source.SourceCatalogService.RefreshStatus;
import org.stapledon.engine.source.SourceRegistry;

class SourceCatalogJobConfigTest {

    private SourceRegistry registry;
    private SourceCatalogService catalogService;
    private CatalogThumbnailService thumbnails;
    private SourceCatalogJobConfig config;

    @BeforeEach
    void setUp() {
        registry = mock(SourceRegistry.class);
        catalogService = mock(SourceCatalogService.class);
        thumbnails = mock(CatalogThumbnailService.class);
        List<ComicSource> sources = List.of(source("alpha"), source("beta"));
        when(registry.all()).thenReturn(sources);
        config = new SourceCatalogJobConfig(registry, catalogService, thumbnails);
        setField(config, "maxAgeDays", 7);
        setField(config, "startDetectPerRun", 5);
        setField(config, "thumbnailMaxAgeDays", 30);
    }

    private static ComicSource source(String id) {
        ComicSource source = mock(ComicSource.class);
        when(source.id()).thenReturn(id);
        when(source.catalog()).thenReturn(Optional.of(mock(SourceCatalog.class)));
        return source;
    }

    private static RefreshResult refreshed() {
        return new RefreshResult(RefreshStatus.REFRESHED, null, null);
    }

    private RepeatStatus run(Tasklet tasklet) throws Exception {
        return tasklet.execute(null, null);
    }

    @Test
    void refreshesOnlyDueCatalogs() throws Exception {
        when(catalogService.isStale("alpha", Duration.ofDays(7))).thenReturn(true);
        when(catalogService.isStale("beta", Duration.ofDays(7))).thenReturn(false);
        when(catalogService.refresh("alpha")).thenReturn(refreshed());

        assertThat(run(config.sourceCatalogTasklet(null, null))).isEqualTo(RepeatStatus.FINISHED);

        verify(catalogService).refresh("alpha");
        verify(catalogService, never()).refresh("beta");
        verify(catalogService).detectMissingStarts(5);
        verify(thumbnails).purge(Duration.ofDays(30));
    }

    @Test
    void forceRefreshesTheNamedSourceOnly() throws Exception {
        when(catalogService.refresh("beta")).thenReturn(refreshed());

        run(config.sourceCatalogTasklet("beta", "true"));

        verify(catalogService).refresh("beta");
        verify(catalogService, never()).refresh("alpha");
        verify(catalogService, never()).isStale(anyString(), any());
    }

    @Test
    void aFailedRefreshFailsTheStepAfterTheRest() {
        when(catalogService.refresh(anyString())).thenReturn(new RefreshResult(RefreshStatus.FAILED, null, "HTTP 503"));

        assertThatThrownBy(() -> run(config.sourceCatalogTasklet("ALL", "true"))).isInstanceOf(IllegalStateException.class);
        verify(catalogService).refresh("alpha");
        verify(catalogService).refresh("beta");
        verify(catalogService).detectMissingStarts(anyInt());
    }
}
