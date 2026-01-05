package org.stapledon.engine.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.stapledon.common.config.IComicsBootstrap;
import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.infrastructure.config.ExecutionTracker;
import org.stapledon.common.service.ComicConfigurationService;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.common.util.Bootstrap;
import org.stapledon.common.util.Direction;
import org.stapledon.engine.downloader.DownloaderFacade;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class ComicManagementFacadeTest {

    @Mock
    private ComicStorageFacade storageFacade;

    @Mock
    private ComicConfigurationService configFacade;

    @Mock
    private DownloaderFacade downloaderFacade;

    @Mock
    private ExecutionTracker taskExecutionTracker;

    @Mock
    private IComicsBootstrap goComicsBootstrap;

    @Mock
    private IComicsBootstrap kingComicsBootstrap;

    private ComicManagementFacade facade;
    private ComicItem testComic;
    private final byte[] testImageData = "test image data".getBytes();

    @BeforeEach
    void setUp() {
        // Create test comic item
        testComic = ComicItem.builder()
                .id(1)
                .name("Test Comic")
                .author("Test Author")
                .description("Test Description")
                .newest(LocalDate.now())
                .oldest(LocalDate.now().minusDays(30))
                .enabled(true)
                .avatarAvailable(true)
                .source("gocomics")
                .sourceIdentifier("testcomic")
                .build();

        // Create test comic config
        ComicConfig comicConfig = new ComicConfig();
        Map<Integer, ComicItem> items = new ConcurrentHashMap<>();
        items.put(testComic.getId(), testComic);
        comicConfig.setItems(items);

        // Create test bootstrap
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.setDailyComics(new ArrayList<>());
        bootstrap.setKingComics(new ArrayList<>());
        bootstrap.getDailyComics().add(goComicsBootstrap);
        bootstrap.getKingComics().add(kingComicsBootstrap);

        // Configure the minimal mocks needed for basic setup
        when(configFacade.loadComicConfig()).thenReturn(comicConfig);

        // Initialize facade
        facade = new ComicManagementFacade(
                storageFacade,
                configFacade,
                downloaderFacade,
                Mockito.mock(org.stapledon.common.service.RetrievalStatusService.class));
        ReflectionTestUtils.setField(facade, "self", facade);
    }

    // Test removed - on-demand downloads via CacheMissEvent no longer supported

    @Test
    void shouldGetAllComics() {
        // Act
        List<ComicItem> comics = facade.getAllComics();

        // Assert
        assertThat(comics.size()).isEqualTo(1);
        assertThat(comics.get(0)).isEqualTo(testComic);
    }

    @Test
    void shouldGetAllComicsWithNullNames() {
        // Arrange - Create a comic config with null name comic
        ComicConfig comicConfig = new ComicConfig();
        Map<Integer, ComicItem> items = new ConcurrentHashMap<>();

        // Normal comic
        ComicItem normalComic = ComicItem.builder()
                .id(1)
                .name("B Comic") // B will sort after A but before C
                .build();

        // Comic with null name
        ComicItem nullNameComic = ComicItem.builder()
                .id(2)
                .name(null)
                .build();

        // Another normal comic
        ComicItem anotherComic = ComicItem.builder()
                .id(3)
                .name("C Comic")
                .build();

        // Yet another normal comic
        ComicItem yetAnotherComic = ComicItem.builder()
                .id(4)
                .name("A Comic")
                .build();

        // Add comics to the map
        items.put(normalComic.getId(), normalComic);
        items.put(nullNameComic.getId(), nullNameComic);
        items.put(anotherComic.getId(), anotherComic);
        items.put(yetAnotherComic.getId(), yetAnotherComic);
        comicConfig.setItems(items);

        when(configFacade.loadComicConfig()).thenReturn(comicConfig);

        // Create new facade instance with our test data
        ComicManagementFacade testFacade = new ComicManagementFacade(
                storageFacade,
                configFacade,
                downloaderFacade,
                Mockito.mock(org.stapledon.common.service.RetrievalStatusService.class));

        // Act
        List<ComicItem> comics = testFacade.getAllComics();

        // Assert
        assertThat(comics.size()).isEqualTo(4);
        // Null name should be sorted first
        assertThat(comics.get(0).getName()).isEqualTo(null);
        assertThat(comics.get(1).getName()).isEqualTo("A Comic");
        assertThat(comics.get(2).getName()).isEqualTo("B Comic");
        assertThat(comics.get(3).getName()).isEqualTo("C Comic");
    }

    @Test
    void shouldGetComicById() {
        // Act
        Optional<ComicItem> comic = facade.getComic(1);

        // Assert
        assertThat(comic.isPresent()).isTrue();
        assertThat(comic.get()).isEqualTo(testComic);
    }

    @Test
    void shouldReturnEmptyWhenComicNotFound() {
        // Act
        Optional<ComicItem> comic = facade.getComic(999);

        // Assert
        assertThat(comic.isPresent()).isFalse();
    }

    @Test
    void shouldGetComicByName() {
        // Act
        Optional<ComicItem> comic = facade.getComicByName("Test Comic");

        // Assert
        assertThat(comic.isPresent()).isTrue();
        assertThat(comic.get()).isEqualTo(testComic);
    }

    @Test
    void shouldReturnEmptyWhenComicNameIsNull() {
        // Act
        Optional<ComicItem> comic = facade.getComicByName(null);

        // Assert
        assertThat(comic.isPresent()).isFalse();
    }

    @Test
    void shouldHandleComicItemWithNullName() {
        // Arrange - create comic with null name
        ComicItem nullNameComic = ComicItem.builder()
                .id(123)
                .name(null)
                .build();

        // Add to comic config
        ComicConfig comicConfig = new ComicConfig();
        Map<Integer, ComicItem> items = new ConcurrentHashMap<>();
        items.put(nullNameComic.getId(), nullNameComic);
        comicConfig.setItems(items);

        when(configFacade.loadComicConfig()).thenReturn(comicConfig);

        // Create new facade with our null-name comic
        ComicManagementFacade nullNameFacade = new ComicManagementFacade(
                storageFacade,
                configFacade,
                downloaderFacade,
                Mockito.mock(org.stapledon.common.service.RetrievalStatusService.class));

        // Act and Assert - this shouldn't throw an NPE
        assertThat(nullNameFacade.getAllComics().size()).isEqualTo(1);
        assertThat(nullNameFacade.getComic(123).get()).isEqualTo(nullNameComic);
        assertThat(nullNameFacade.getComicByName("AnyName").stream().count()).isEqualTo(0);
    }

    @Test
    void shouldCreateComic() {
        // Arrange
        ComicItem newComic = ComicItem.builder()
                .id(2)
                .name("New Comic")
                .build();

        // Act
        Optional<ComicItem> created = facade.createComic(newComic);

        // Assert
        assertThat(created.isPresent()).isTrue();
        assertThat(created.get()).isEqualTo(newComic);
        verify(configFacade).saveComicConfig(any());
    }

    @Test
    void shouldNotCreateComicIfAlreadyExists() {
        // Act
        Optional<ComicItem> created = facade.createComic(testComic);

        // Assert
        assertThat(created.isPresent()).isFalse();
        verify(configFacade, never()).saveComicConfig(any());
    }

    @Test
    void shouldUpdateComic() {
        // Arrange
        ComicItem updatedComic = ComicItem.builder()
                .id(1)
                .name("Updated Comic")
                .build();

        // Act
        Optional<ComicItem> updated = facade.updateComic(1, updatedComic);

        // Assert
        assertThat(updated.isPresent()).isTrue();
        assertThat(updated.get().getName()).isEqualTo("Updated Comic");
        verify(configFacade).saveComicConfig(any());
    }

    @Test
    void shouldDeleteComic() {
        // Act
        boolean deleted = facade.deleteComic(1);

        // Assert
        assertThat(deleted).isTrue();
        verify(storageFacade).deleteComic(1, "Test Comic");
        verify(configFacade).saveComicConfig(any());
    }

    @Test
    void shouldNotDeleteComicIfNotFound() {
        // Act
        boolean deleted = facade.deleteComic(999);

        // Assert
        assertThat(deleted).isFalse();
        verify(storageFacade, never()).deleteComic(anyInt(), anyString());
        verify(configFacade, never()).saveComicConfig(any());
    }

    @Test
    void shouldGetComicStripInForwardDirection() {
        // Arrange
        LocalDate oldestDate = LocalDate.now().minusDays(30);
        ImageDto imageDto = new ImageDto();

        when(storageFacade.getOldestDateWithComic(1, "Test Comic")).thenReturn(Optional.of(oldestDate));
        when(storageFacade.getComicStrip(1, "Test Comic", oldestDate)).thenReturn(Optional.of(imageDto));

        // Act
        ComicNavigationResult result = facade.getComicStrip(1, Direction.FORWARD);

        // Assert
        assertThat(result.isFound()).isTrue();
        assertThat(result.getImage()).isNotNull();
        assertThat(result.getImage()).isEqualTo(imageDto);
    }

    @Test
    void shouldGetComicStripInBackwardDirection() {
        // Arrange
        LocalDate newestDate = LocalDate.now();
        ImageDto imageDto = new ImageDto();

        when(storageFacade.getNewestDateWithComic(1, "Test Comic")).thenReturn(Optional.of(newestDate));
        when(storageFacade.getComicStrip(1, "Test Comic", newestDate)).thenReturn(Optional.of(imageDto));

        // Act
        ComicNavigationResult result = facade.getComicStrip(1, Direction.BACKWARD);

        // Assert
        assertThat(result.isFound()).isTrue();
        assertThat(result.getImage()).isNotNull();
        assertThat(result.getImage()).isEqualTo(imageDto);
    }

    @Test
    void shouldGetComicStripFromDate() {
        // Arrange
        LocalDate from = LocalDate.now().minusDays(15);
        LocalDate next = LocalDate.now().minusDays(14);
        ImageDto imageDto = new ImageDto();

        when(storageFacade.getNextDateWithComic(1, "Test Comic", from)).thenReturn(Optional.of(next));
        when(storageFacade.getComicStrip(1, "Test Comic", next)).thenReturn(Optional.of(imageDto));

        // Act
        ComicNavigationResult result = facade.getComicStrip(1, Direction.FORWARD, from);

        // Assert
        assertThat(result.isFound()).isTrue();
        assertThat(result.getImage()).isNotNull();
        assertThat(result.getImage()).isEqualTo(imageDto);
    }

    @Test
    void shouldGetComicStripOnDate() {
        // Arrange
        LocalDate date = LocalDate.now();
        ImageDto imageDto = new ImageDto();

        when(storageFacade.getComicStrip(1, "Test Comic", date)).thenReturn(Optional.of(imageDto));

        // Act
        Optional<ImageDto> result = facade.getComicStripOnDate(1, date);

        // Assert
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(imageDto);
    }

    @Test
    void shouldGetAvatar() {
        // Arrange
        ImageDto imageDto = new ImageDto();

        when(storageFacade.getAvatar(1, "Test Comic")).thenReturn(Optional.of(imageDto));

        // Act
        Optional<ImageDto> result = facade.getAvatar(1);

        // Assert
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(imageDto);
    }

    @Test
    void shouldUpdateAllComics() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Test Comic")
                .source("gocomics")
                .sourceIdentifier("testcomic")
                .date(LocalDate.now())
                .build();

        ComicDownloadResult result = ComicDownloadResult.builder()
                .request(request)
                .successful(true)
                .imageData(testImageData)
                .build();

        // Mock: comic doesn't exist on disk yet
        when(storageFacade.comicStripExists(anyInt(), anyString(), any())).thenReturn(false);

        // Mock: download succeeds
        when(downloaderFacade.downloadComic(any())).thenReturn(result);

        // Mock: save succeeds
        when(storageFacade.saveComicStrip(anyInt(), anyString(), any(), any())).thenReturn(true);

        // Act
        boolean updated = facade.updateAllComics();

        // Assert
        assertThat(updated).isTrue();
        verify(storageFacade).comicStripExists(eq(1), eq("Test Comic"), any());
        verify(downloaderFacade).downloadComic(any());
        verify(storageFacade).saveComicStrip(eq(1), eq("Test Comic"), any(), eq(testImageData));
        verify(configFacade).saveComicConfig(any());
    }

    @Test
    void shouldUpdateSingleComic() {
        // Arrange
        ComicDownloadResult result = ComicDownloadResult.builder()
                .request(ComicDownloadRequest.builder().build())
                .successful(true)
                .imageData(testImageData)
                .build();

        when(downloaderFacade.downloadComic(any())).thenReturn(result);
        when(storageFacade.saveComicStrip(anyInt(), anyString(), any(), any())).thenReturn(true);

        // Act
        boolean updated = facade.updateComic(1);

        // Assert
        assertThat(updated).isTrue();
        verify(downloaderFacade).downloadComic(any());
        verify(storageFacade).saveComicStrip(eq(1), eq("Test Comic"), any(), eq(testImageData));
        verify(configFacade).saveComicConfig(any());
    }

    // Moved the reconcile test to a separate test class to avoid
    // UnnecessaryStubbingException
    // See ComicManagementFacadeReconcileTest.java

    @Test
    void shouldPurgeOldImages() {
        // Arrange
        when(storageFacade.purgeOldImages(anyInt(), anyString(), anyInt())).thenReturn(true);

        // Act
        boolean purged = facade.purgeOldImages(7);

        // Assert
        assertThat(purged).isTrue();
        verify(storageFacade).purgeOldImages(eq(1), eq("Test Comic"), eq(7));
    }

    @Test
    void shouldGetNewestDateWithComic() {
        // Arrange
        LocalDate newest = LocalDate.now();
        when(storageFacade.getNewestDateWithComic(1, "Test Comic")).thenReturn(Optional.of(newest));

        // Act
        Optional<LocalDate> result = facade.getNewestDateWithComic(1);

        // Assert
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(newest);
    }

    @Test
    void shouldGetOldestDateWithComic() {
        // Arrange
        LocalDate oldest = LocalDate.now().minusDays(30);
        when(storageFacade.getOldestDateWithComic(1, "Test Comic")).thenReturn(Optional.of(oldest));

        // Act
        Optional<LocalDate> result = facade.getOldestDateWithComic(1);

        // Assert
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(oldest);
    }
}
