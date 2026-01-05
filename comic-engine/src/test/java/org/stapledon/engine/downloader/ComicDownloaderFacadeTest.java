package org.stapledon.engine.downloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ComicItem;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ComicDownloaderFacadeTest {

    private final LocalDate testDate = LocalDate.of(2023, 1, 1);
    private final byte[] testImageData = "test-image-data".getBytes();
    private DownloaderFacade facade;
    @Mock
    private ComicDownloaderStrategy goComicsStrategy;
    @Mock
    private ComicDownloaderStrategy comicsKingdomStrategy;

    @BeforeEach
    void setUp() {
        facade = new ComicDownloaderFacade(
                Mockito.mock(org.stapledon.common.service.RetrievalStatusService.class),
                Mockito.mock(org.stapledon.common.service.ErrorTrackingService.class));
        facade.registerDownloaderStrategy("gocomics", goComicsStrategy);
        facade.registerDownloaderStrategy("comicskingdom", comicsKingdomStrategy);
    }

    @Test
    void shouldDownloadComicSuccessfully() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("calvin")
                .source("gocomics")
                .sourceIdentifier("calvinandhobbes")
                .date(testDate)
                .build();

        ComicDownloadResult expectedResult = ComicDownloadResult.success(request, testImageData);
        when(goComicsStrategy.downloadComic(request)).thenReturn(expectedResult);

        // Act
        ComicDownloadResult result = facade.downloadComic(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getImageData()).isEqualTo(testImageData);
        assertThat(result.getRequest()).isEqualTo(request);
        verify(goComicsStrategy).downloadComic(request);
        verify(comicsKingdomStrategy, never()).downloadComic(any());
    }

    @Test
    void shouldReturnFailureWhenStrategyNotFound() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("calvin")
                .source("unknown")
                .sourceIdentifier("calvinandhobbes")
                .date(testDate)
                .build();

        // Act
        ComicDownloadResult result = facade.downloadComic(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrorMessage().contains("No downloader strategy registered")).isTrue();
        verify(goComicsStrategy, never()).downloadComic(any());
        verify(comicsKingdomStrategy, never()).downloadComic(any());
    }

    @Test
    void shouldReturnFailureWhenStrategyThrowsException() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("calvin")
                .source("gocomics")
                .sourceIdentifier("calvinandhobbes")
                .date(testDate)
                .build();

        when(goComicsStrategy.downloadComic(request)).thenThrow(new RuntimeException("Test exception"));

        // Act
        ComicDownloadResult result = facade.downloadComic(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrorMessage().contains("Error downloading comic")).isTrue();
        verify(goComicsStrategy).downloadComic(request);
    }

    @Test
    void shouldDownloadAvatarSuccessfully() {
        // Arrange
        int comicId = 1;
        String comicName = "calvin";
        String source = "gocomics";
        String sourceIdentifier = "calvinandhobbes";

        when(goComicsStrategy.downloadAvatar(comicId, comicName, sourceIdentifier))
                .thenReturn(Optional.of(testImageData));

        // Act
        Optional<byte[]> result = facade.downloadAvatar(comicId, comicName, source, sourceIdentifier);

        // Assert
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(testImageData);
        verify(goComicsStrategy).downloadAvatar(comicId, comicName, sourceIdentifier);
        verify(comicsKingdomStrategy, never()).downloadAvatar(anyInt(), anyString(), anyString());
    }

    @Test
    void shouldReturnEmptyWhenAvatarStrategyNotFound() {
        // Arrange
        int comicId = 1;
        String comicName = "calvin";
        String source = "unknown";
        String sourceIdentifier = "calvinandhobbes";

        // Act
        Optional<byte[]> result = facade.downloadAvatar(comicId, comicName, source, sourceIdentifier);

        // Assert
        assertThat(result.isPresent()).isFalse();
        verify(goComicsStrategy, never()).downloadAvatar(anyInt(), anyString(), anyString());
        verify(comicsKingdomStrategy, never()).downloadAvatar(anyInt(), anyString(), anyString());
    }

    @Test
    void shouldDownloadComicsForDate() {
        // Arrange
        List<ComicItem> comics = new ArrayList<>();
        comics.add(ComicItem.builder()
                .id(1)
                .name("calvin")
                .source("gocomics")
                .sourceIdentifier("calvinandhobbes")
                .build());
        comics.add(
                ComicItem.builder()
                        .id(2)
                        .name("dilbert")
                        .source("comicskingdom")
                        .sourceIdentifier("dilbert")
                        .build());
        ComicConfig config = new ComicConfig();
        config.setComics(comics);

        ComicDownloadRequest request1 = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("calvin")
                .source("gocomics")
                .sourceIdentifier("calvinandhobbes")
                .date(testDate)
                .build();

        ComicDownloadRequest request2 = ComicDownloadRequest.builder()
                .comicId(2)
                .comicName("dilbert")
                .source("comicskingdom")
                .sourceIdentifier("dilbert")
                .date(testDate)
                .build();

        ComicDownloadResult result1 = ComicDownloadResult.success(request1, testImageData);
        ComicDownloadResult result2 = ComicDownloadResult.failure(request2, "Test error");

        when(goComicsStrategy.downloadComic(any(ComicDownloadRequest.class))).thenReturn(result1);
        when(comicsKingdomStrategy.downloadComic(any(ComicDownloadRequest.class))).thenReturn(result2);

        // Act
        List<ComicDownloadResult> results = facade.downloadComicsForDate(config, testDate);

        // Assert
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0).isSuccessful()).isTrue();
        assertThat(results.get(1).isSuccessful()).isFalse();

        verify(goComicsStrategy).downloadComic(any(ComicDownloadRequest.class));
        verify(comicsKingdomStrategy).downloadComic(any(ComicDownloadRequest.class));
    }

    @Test
    void shouldHandleNullConfigWhenDownloadingComics() {
        // Act
        List<ComicDownloadResult> results = facade.downloadComicsForDate(null, testDate);

        // Assert
        assertThat(results.isEmpty()).isTrue();
        verify(goComicsStrategy, never()).downloadComic(any());
        verify(comicsKingdomStrategy, never()).downloadComic(any());
    }

    @Test
    void shouldUseCurrentDateWhenDownloadingLatestComics() {
        // Arrange
        List<ComicItem> comics = new ArrayList<>();
        comics.add(
                ComicItem.builder()
                        .id(1)
                        .name("calvin")
                        .source("gocomics")
                        .sourceIdentifier("calvinandhobbes")
                        .build());

        ComicConfig config = new ComicConfig();
        config.setComics(comics);

        ComicDownloadResult mockResult = ComicDownloadResult.success(
                ComicDownloadRequest.builder()
                        .comicId(1)
                        .comicName("calvin")
                        .source("gocomics")
                        .sourceIdentifier("calvinandhobbes")
                        .date(LocalDate.now())
                        .build(),
                testImageData);

        when(goComicsStrategy.downloadComic(any(ComicDownloadRequest.class))).thenReturn(mockResult);

        // Act
        List<ComicDownloadResult> results = facade.downloadLatestComics(config);

        // Assert
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).isSuccessful()).isTrue();

        verify(goComicsStrategy).downloadComic(any(ComicDownloadRequest.class));
    }

    @Test
    void shouldSkipInactiveComics() {
        // Arrange
        List<ComicItem> comics = new ArrayList<>();
        comics.add(ComicItem.builder()
                .id(1)
                .name("Active Comic")
                .source("gocomics")
                .sourceIdentifier("active")
                .active(true)
                .build());
        comics.add(ComicItem.builder()
                .id(2)
                .name("Inactive Comic")
                .source("gocomics")
                .sourceIdentifier("inactive")
                .active(false)
                .build());

        ComicConfig config = new ComicConfig();
        config.setComics(comics);

        ComicDownloadResult mockResult = ComicDownloadResult.success(
                ComicDownloadRequest.builder()
                        .comicId(1)
                        .comicName("Active Comic")
                        .source("gocomics")
                        .sourceIdentifier("active")
                        .date(testDate)
                        .build(),
                testImageData);

        when(goComicsStrategy.downloadComic(any(ComicDownloadRequest.class))).thenReturn(mockResult);

        // Act
        List<ComicDownloadResult> results = facade.downloadComicsForDate(config, testDate);

        // Assert
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).getRequest().getComicName()).isEqualTo("Active Comic");
    }

    @Test
    void shouldSkipComicsNotPublishingOnTargetDay() {
        // Arrange
        LocalDate monday = LocalDate.of(2025, 10, 27); // A Monday
        DayOfWeek mondayDay = monday.getDayOfWeek();

        List<ComicItem> comics = new ArrayList<>();
        comics.add(ComicItem.builder()
                .id(1)
                .name("Daily Comic")
                .source("gocomics")
                .sourceIdentifier("daily")
                .active(true)
                .build()); // No publication days = publishes daily
        comics.add(ComicItem.builder()
                .id(2)
                .name("Sunday Only")
                .source("gocomics")
                .sourceIdentifier("sunday-only")
                .active(true)
                .publicationDays(List.of(DayOfWeek.SUNDAY))
                .build());

        ComicConfig config = new ComicConfig();
        config.setComics(comics);

        ComicDownloadResult mockResult = ComicDownloadResult.success(
                ComicDownloadRequest.builder()
                        .comicId(1)
                        .comicName("Daily Comic")
                        .source("gocomics")
                        .sourceIdentifier("daily")
                        .date(monday)
                        .build(),
                testImageData);

        when(goComicsStrategy.downloadComic(any(ComicDownloadRequest.class))).thenReturn(mockResult);

        // Act
        List<ComicDownloadResult> results = facade.downloadComicsForDate(config, monday);

        // Assert - only the daily comic should be downloaded
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).getRequest().getComicName()).isEqualTo("Daily Comic");
    }

    @Test
    void shouldIncludeComicWhenPublicationDaysMatchesTargetDay() {
        // Arrange
        LocalDate monday = LocalDate.of(2025, 10, 27); // A Monday

        List<ComicItem> comics = new ArrayList<>();
        comics.add(ComicItem.builder()
                .id(1)
                .name("Monday Comic")
                .source("gocomics")
                .sourceIdentifier("monday")
                .active(true)
                .publicationDays(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
                .build());

        ComicConfig config = new ComicConfig();
        config.setComics(comics);

        ComicDownloadResult mockResult = ComicDownloadResult.success(
                ComicDownloadRequest.builder()
                        .comicId(1)
                        .comicName("Monday Comic")
                        .source("gocomics")
                        .sourceIdentifier("monday")
                        .date(monday)
                        .build(),
                testImageData);

        when(goComicsStrategy.downloadComic(any(ComicDownloadRequest.class))).thenReturn(mockResult);

        // Act
        List<ComicDownloadResult> results = facade.downloadComicsForDate(config, monday);

        // Assert
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).getRequest().getComicName()).isEqualTo("Monday Comic");
    }
}
