package org.stapledon.engine.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import java.util.List;

import org.stapledon.engine.batch.scheduler.JobParameterDefinition.Option;
import org.stapledon.engine.downloader.DownloaderFacade;

class SourceRegistryTest {

    @Test
    void registersEverySourcesDownloaderAndSortsByName() {
        DownloaderFacade downloaders = mock(DownloaderFacade.class);
        StubSource zebra = new StubSource("zebra");
        StubSource apple = new StubSource("apple");

        SourceRegistry registry = new SourceRegistry(List.of(zebra, apple), downloaders);

        verify(downloaders).registerDownloaderStrategy("zebra", zebra.downloader());
        verify(downloaders).registerDownloaderStrategy("apple", apple.downloader());
        assertThat(registry.all()).extracting(ComicSource::id).containsExactly("apple", "zebra");
        assertThat(registry.isKnown("apple")).isTrue();
        assertThat(registry.isKnown(null)).isFalse();
        assertThat(registry.find("pear")).isEmpty();
    }

    @Test
    void jobOptionsStartWithAll() {
        SourceRegistry registry = new SourceRegistry(List.of(new StubSource("b"), new StubSource("a")), mock(DownloaderFacade.class));

        assertThat(registry.jobSourceOptions()).containsExactly(new Option("ALL", "All Sources"), new Option("a", "A"), new Option("b", "B"));
    }

    @Test
    void duplicateIdsAreAMistake() {
        List<ComicSource> sources = List.of(new StubSource("same"), new StubSource("same"));

        assertThatThrownBy(() -> new SourceRegistry(sources, mock(DownloaderFacade.class))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void identifiersMustLookLikeSlugs() {
        assertThat(ComicSource.isValidIdentifier("calvinandhobbes")).isTrue();
        assertThat(ComicSource.isValidIdentifier("stonesoup_espanol")).isTrue();
        assertThat(ComicSource.isValidIdentifier("beetle-bailey-1")).isTrue();
        assertThat(ComicSource.isValidIdentifier("Peanuts")).isFalse();
        assertThat(ComicSource.isValidIdentifier("-leading")).isFalse();
        assertThat(ComicSource.isValidIdentifier("a/b")).isFalse();
        assertThat(ComicSource.isValidIdentifier("")).isFalse();
        assertThat(ComicSource.isValidIdentifier(null)).isFalse();
    }
}
