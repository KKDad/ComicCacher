package org.stapledon.core.comic.downloader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.stapledon.api.dto.comic.ComicConfig;
import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.core.comic.dto.ComicDownloadRequest;
import org.stapledon.core.comic.dto.ComicDownloadResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ComicDownloaderFacadeImplTest {

    private final LocalDate testDate = LocalDate.of(2023, 1, 1);
    private final byte[] testImageData = "test-image-data".getBytes();
    private ComicDownloaderFacadeImpl facade;
    @Mock
    private ComicDownloaderStrategy goComicsStrategy;
    @Mock
    private ComicDownloaderStrategy comicsKingdomStrategy;

    @BeforeEach
    void setUp() {
        facade = new ComicDownloaderFacadeImpl(Mockito.mock(org.stapledon.core.comic.service.RetrievalStatusService.class));
        
        // Remove unnecessary stubbing - the getSource() method isn't used in the implementation
        // when(goComicsStrategy.getSource()).thenReturn("gocomics");
        // when(comicsKingdomStrategy.getSource()).thenReturn("comicskingdom");

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
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals(testImageData, result.getImageData());
        assertEquals(request, result.getRequest());
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
        assertNotNull(result);
        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorMessage().contains("No downloader strategy registered"));
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
        assertNotNull(result);
        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorMessage().contains("Error downloading comic"));
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
        assertTrue(result.isPresent());
        assertEquals(testImageData, result.get());
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
        assertFalse(result.isPresent());
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
        assertEquals(2, results.size());
        assertTrue(results.get(0).isSuccessful());
        assertFalse(results.get(1).isSuccessful());

        verify(goComicsStrategy).downloadComic(any(ComicDownloadRequest.class));
        verify(comicsKingdomStrategy).downloadComic(any(ComicDownloadRequest.class));
    }

    @Test
    void shouldHandleNullConfigWhenDownloadingComics() {
        // Act
        List<ComicDownloadResult> results = facade.downloadComicsForDate(null, testDate);

        // Assert
        assertTrue(results.isEmpty());
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
        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccessful());

        verify(goComicsStrategy).downloadComic(any(ComicDownloadRequest.class));
    }
}