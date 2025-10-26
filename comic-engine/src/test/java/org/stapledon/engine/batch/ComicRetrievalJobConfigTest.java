package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ItemReader;
import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.service.ComicConfigurationService;
import org.stapledon.engine.downloader.DownloaderFacade;

import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for ComicRetrievalJobConfig download skip logic
 * Tests that inactive and non-publishing comics are properly skipped
 */
@ExtendWith(MockitoExtension.class)
class ComicRetrievalJobConfigTest {

    @Mock
    private ComicConfigurationService configurationService;

    @Mock
    private DownloaderFacade downloaderFacade;

    private ComicRetrievalJobConfig jobConfig;

    @BeforeEach
    void setUp() {
        jobConfig = new ComicRetrievalJobConfig(configurationService, downloaderFacade);
    }

    @Test
    void testCreateComicRequestsIncludesActiveComics() throws Exception {
        // Given: A configuration with active comics
        ComicConfig config = new ComicConfig();
        List<ComicItem> comics = new ArrayList<>();

        ComicItem activeComic = ComicItem.builder()
                .id(1)
                .name("Active Comic")
                .source("gocomics")
                .sourceIdentifier("active-comic")
                .active(true)
                .build();

        comics.add(activeComic);
        config.setComics(comics);

        when(configurationService.loadComicConfig()).thenReturn(config);

        // When: Creating comic requests
        List<ComicDownloadRequest> requests = invokeCreateComicRequests();

        // Then: Active comic should be included
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getComicName()).isEqualTo("Active Comic");
    }

    @Test
    void testCreateComicRequestsSkipsInactiveComics() throws Exception {
        // Given: A configuration with inactive comics
        ComicConfig config = new ComicConfig();
        List<ComicItem> comics = new ArrayList<>();

        ComicItem inactiveComic = ComicItem.builder()
                .id(1)
                .name("Committed")
                .source("gocomics")
                .sourceIdentifier("committed")
                .active(false)
                .build();

        comics.add(inactiveComic);
        config.setComics(comics);

        when(configurationService.loadComicConfig()).thenReturn(config);

        // When: Creating comic requests
        List<ComicDownloadRequest> requests = invokeCreateComicRequests();

        // Then: Inactive comic should be skipped
        assertThat(requests).isEmpty();
    }

    @Test
    void testCreateComicRequestsIncludesComicsPublishingToday() throws Exception {
        // Given: A Sunday-only comic and today is Sunday
        LocalDate today = LocalDate.now();
        DayOfWeek todayDay = today.getDayOfWeek();

        ComicConfig config = new ComicConfig();
        List<ComicItem> comics = new ArrayList<>();

        ComicItem sundayComic = ComicItem.builder()
                .id(1)
                .name("FoxTrot")
                .source("gocomics")
                .sourceIdentifier("foxtrot")
                .publicationDays(List.of(todayDay)) // Publishes today
                .active(true)
                .build();

        comics.add(sundayComic);
        config.setComics(comics);

        when(configurationService.loadComicConfig()).thenReturn(config);

        // When: Creating comic requests
        List<ComicDownloadRequest> requests = invokeCreateComicRequests();

        // Then: Comic should be included (it publishes today)
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getComicName()).isEqualTo("FoxTrot");
    }

    @Test
    void testCreateComicRequestsSkipsComicsNotPublishingToday() throws Exception {
        // Given: A Sunday-only comic and today is NOT Sunday
        LocalDate today = LocalDate.now();
        DayOfWeek todayDay = today.getDayOfWeek();

        // Find a day that is NOT today
        DayOfWeek differentDay = (todayDay == DayOfWeek.SUNDAY) ? DayOfWeek.MONDAY : DayOfWeek.SUNDAY;

        ComicConfig config = new ComicConfig();
        List<ComicItem> comics = new ArrayList<>();

        ComicItem sundayComic = ComicItem.builder()
                .id(1)
                .name("FoxTrot")
                .source("gocomics")
                .sourceIdentifier("foxtrot")
                .publicationDays(List.of(differentDay)) // Does NOT publish today
                .active(true)
                .build();

        comics.add(sundayComic);
        config.setComics(comics);

        when(configurationService.loadComicConfig()).thenReturn(config);

        // When: Creating comic requests
        List<ComicDownloadRequest> requests = invokeCreateComicRequests();

        // Then: Comic should be skipped (doesn't publish today)
        assertThat(requests).isEmpty();
    }

    @Test
    void testCreateComicRequestsNullPublicationDaysMeansDaily() throws Exception {
        // Given: A comic with null publicationDays (meaning daily)
        ComicConfig config = new ComicConfig();
        List<ComicItem> comics = new ArrayList<>();

        ComicItem dailyComic = ComicItem.builder()
                .id(1)
                .name("Garfield")
                .source("gocomics")
                .sourceIdentifier("garfield")
                .publicationDays(null) // Daily publication
                .active(true)
                .build();

        comics.add(dailyComic);
        config.setComics(comics);

        when(configurationService.loadComicConfig()).thenReturn(config);

        // When: Creating comic requests
        List<ComicDownloadRequest> requests = invokeCreateComicRequests();

        // Then: Comic should be included (publishes daily)
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getComicName()).isEqualTo("Garfield");
    }

    @Test
    void testCreateComicRequestsEmptyPublicationDaysMeansDaily() throws Exception {
        // Given: A comic with empty publicationDays list (meaning daily)
        ComicConfig config = new ComicConfig();
        List<ComicItem> comics = new ArrayList<>();

        ComicItem dailyComic = ComicItem.builder()
                .id(1)
                .name("Garfield")
                .source("gocomics")
                .sourceIdentifier("garfield")
                .publicationDays(List.of()) // Empty = daily
                .active(true)
                .build();

        comics.add(dailyComic);
        config.setComics(comics);

        when(configurationService.loadComicConfig()).thenReturn(config);

        // When: Creating comic requests
        List<ComicDownloadRequest> requests = invokeCreateComicRequests();

        // Then: Comic should be included (publishes daily)
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getComicName()).isEqualTo("Garfield");
    }

    @Test
    void testCreateComicRequestsMixedComics() throws Exception {
        // Given: A mix of active, inactive, and scheduled comics
        LocalDate today = LocalDate.now();
        DayOfWeek todayDay = today.getDayOfWeek();
        DayOfWeek differentDay = (todayDay == DayOfWeek.SUNDAY) ? DayOfWeek.MONDAY : DayOfWeek.SUNDAY;

        ComicConfig config = new ComicConfig();
        List<ComicItem> comics = new ArrayList<>();

        // Active daily comic - SHOULD be included
        comics.add(ComicItem.builder()
                .id(1)
                .name("Active Daily")
                .source("gocomics")
                .sourceIdentifier("active-daily")
                .active(true)
                .build());

        // Inactive comic - SHOULD be skipped
        comics.add(ComicItem.builder()
                .id(2)
                .name("Inactive Comic")
                .source("gocomics")
                .sourceIdentifier("inactive")
                .active(false)
                .build());

        // Publishes today - SHOULD be included
        comics.add(ComicItem.builder()
                .id(3)
                .name("Publishes Today")
                .source("gocomics")
                .sourceIdentifier("publishes-today")
                .publicationDays(List.of(todayDay))
                .active(true)
                .build());

        // Does NOT publish today - SHOULD be skipped
        comics.add(ComicItem.builder()
                .id(4)
                .name("Not Today")
                .source("gocomics")
                .sourceIdentifier("not-today")
                .publicationDays(List.of(differentDay))
                .active(true)
                .build());

        config.setComics(comics);
        when(configurationService.loadComicConfig()).thenReturn(config);

        // When: Creating comic requests
        List<ComicDownloadRequest> requests = invokeCreateComicRequests();

        // Then: Only active comics publishing today should be included
        assertThat(requests).hasSize(2);
        assertThat(requests).extracting(ComicDownloadRequest::getComicName)
                .containsExactlyInAnyOrder("Active Daily", "Publishes Today");
    }

    @Test
    void testCreateComicRequestsSkipsComicsWithNoSource() throws Exception {
        // Given: A comic with null source
        ComicConfig config = new ComicConfig();
        List<ComicItem> comics = new ArrayList<>();

        ComicItem noSourceComic = ComicItem.builder()
                .id(1)
                .name("No Source")
                .source(null)
                .active(true)
                .build();

        comics.add(noSourceComic);
        config.setComics(comics);

        when(configurationService.loadComicConfig()).thenReturn(config);

        // When: Creating comic requests
        List<ComicDownloadRequest> requests = invokeCreateComicRequests();

        // Then: Comic with no source should be skipped
        assertThat(requests).isEmpty();
    }

    /**
     * Helper method to invoke the private createComicRequests() method via reflection
     */
    private List<ComicDownloadRequest> invokeCreateComicRequests() throws Exception {
        Method method = ComicRetrievalJobConfig.class.getDeclaredMethod("createComicRequests");
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ComicDownloadRequest> result = (List<ComicDownloadRequest>) method.invoke(jobConfig);
        return result;
    }
}
