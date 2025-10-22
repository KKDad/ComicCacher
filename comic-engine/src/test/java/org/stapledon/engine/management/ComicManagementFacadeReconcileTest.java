package org.stapledon.engine.management;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.util.Bootstrap;
import org.stapledon.engine.downloader.ComicDownloaderFacade;
import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.service.ComicConfigurationService;
import org.stapledon.common.config.IComicsBootstrap;

import org.stapledon.common.infrastructure.config.TaskExecutionTracker;
import org.stapledon.common.config.properties.StartupReconcilerProperties;
import org.stapledon.common.service.ComicStorageFacade;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Separate test class for reconciling with bootstrap to avoid UnnecessaryStubbingException
 */
@ExtendWith(MockitoExtension.class)
public class ComicManagementFacadeReconcileTest {

    @Mock
    private ComicStorageFacade storageFacade;
    
    @Mock
    private ComicConfigurationService configFacade;
    
    @Mock
    private ComicDownloaderFacade downloaderFacade;
    
    @Mock
    private StartupReconcilerProperties reconcilerProperties;
    
    @Mock
    private TaskExecutionTracker taskExecutionTracker;
    
    @Mock
    private IComicsBootstrap goComicsBootstrap;
    
    @Mock
    private IComicsBootstrap kingComicsBootstrap;
    
    private ComicManagementFacadeImpl facade;
    private ComicConfig comicConfig;
    private Bootstrap bootstrap;
    private final byte[] testImageData = "test image data".getBytes();
    
    @BeforeEach
    void setUp() {
        // Create test comic item
        ComicItem testComic = ComicItem.builder()
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
        comicConfig = new ComicConfig();
        Map<Integer, ComicItem> items = new ConcurrentHashMap<>();
        items.put(testComic.getId(), testComic);
        comicConfig.setItems(items);
        
        // Create test bootstrap with configurable mocks
        bootstrap = new Bootstrap();
        bootstrap.setDailyComics(new ArrayList<>());
        bootstrap.setKingComics(new ArrayList<>());
        bootstrap.getDailyComics().add(goComicsBootstrap);
        bootstrap.getKingComics().add(kingComicsBootstrap);
        
        // Initialize facade
        facade = new ComicManagementFacadeImpl(
                storageFacade,
                configFacade,
                downloaderFacade,
                reconcilerProperties,
                taskExecutionTracker,
                Mockito.mock(org.stapledon.common.service.RetrievalStatusService.class)
        );
    }
    
    @Test
    void shouldReconcileWithBootstrap() {
        // Arrange
        when(taskExecutionTracker.canRunToday(anyString())).thenReturn(true);
        when(configFacade.loadBootstrapConfig()).thenReturn(bootstrap);
        when(configFacade.loadComicConfig()).thenReturn(comicConfig);
        
        // Configure bootstraps for this specific test - using lenient mode for potentially unused stubs
        lenient().when(goComicsBootstrap.stripName()).thenReturn("Test Comic");
        lenient().when(goComicsBootstrap.getSource()).thenReturn("gocomics");
        lenient().when(goComicsBootstrap.getSourceIdentifier()).thenReturn("testcomic");
        lenient().when(goComicsBootstrap.startDate()).thenReturn(LocalDate.now().minusDays(30));
        
        // Add a new comic in the bootstrap that's not in the comic config
        when(kingComicsBootstrap.stripName()).thenReturn("New Comic");
        when(kingComicsBootstrap.getSource()).thenReturn("comicskingdom");
        when(kingComicsBootstrap.getSourceIdentifier()).thenReturn("kingcomic");
        when(kingComicsBootstrap.startDate()).thenReturn(LocalDate.now().minusDays(30));
        
        ComicDownloadResult result = ComicDownloadResult.builder()
                .request(ComicDownloadRequest.builder().build())
                .successful(true)
                .imageData(testImageData)
                .build();
        
        when(downloaderFacade.downloadComic(any())).thenReturn(result);
        
        // Act
        boolean reconciled = facade.reconcileWithBootstrap();
        
        // Assert
        assertTrue(reconciled);
        verify(configFacade).loadBootstrapConfig();
        // The implementation calls loadComicConfig multiple times during the reconciliation process
        verify(configFacade, atLeastOnce()).loadComicConfig();
        verify(configFacade).saveComicConfig(any());
        verify(taskExecutionTracker).markTaskExecuted(anyString());
    }
    
}