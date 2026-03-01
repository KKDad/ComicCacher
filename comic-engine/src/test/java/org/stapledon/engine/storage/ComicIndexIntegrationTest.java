package org.stapledon.engine.storage;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.DuplicateValidationResult;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.service.AnalysisService;
import org.stapledon.common.service.DuplicateValidationService;
import org.stapledon.common.service.ValidationService;
import org.stapledon.common.util.GsonUtils;
import org.stapledon.engine.validation.DuplicateHashCacheService;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for comic index update functionality.
 * Tests the complete flow from saveComicStrip() to index persistence.
 *
 * This test reproduces the bug where files exist on disk but available-dates.json
 * isn't updated after a backend redeployment.
 */
class ComicIndexIntegrationTest {

    @TempDir
    Path tempDir;

    private FileSystemComicStorageFacade storageFacade;
    private ComicIndexService indexService;
    private ImageMetadataRepository metadataRepository;

    private static final int COMIC_ID = 1;
    private static final String COMIC_NAME = "TestComic";
    private static final ComicIdentifier COMIC = new ComicIdentifier(COMIC_ID, COMIC_NAME);

    @BeforeEach
    void setUp() {
        // Real Gson instance with LocalDate adapter
        Gson gson = GsonUtils.createGson();

        // Real cache properties pointing to temp directory
        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.setLocation(tempDir.toAbsolutePath().toString());

        // Mock metadata repository (not critical for this test)
        metadataRepository = mock(ImageMetadataRepository.class);
        lenient().when(metadataRepository.saveMetadata(any())).thenReturn(true);

        // Real ComicIndexService - this is what we're testing
        indexService = new ComicIndexService(gson, cacheProperties, metadataRepository);

        // Mock validation services (not critical for this test)
        ValidationService validationService = mock(ValidationService.class);
        lenient().when(validationService.validateWithMinDimensions(any(byte[].class), anyInt(), anyInt()))
                .thenReturn(ImageValidationResult.success(ImageFormat.PNG, 200, 100, 1000));

        DuplicateValidationService duplicateValidationService = mock(DuplicateValidationService.class);
        lenient().when(duplicateValidationService.validateNoDuplicate(anyInt(), anyString(), any(LocalDate.class), any(byte[].class)))
                .thenReturn(DuplicateValidationResult.unique("test-hash"));

        DuplicateHashCacheService hashCacheService = mock(DuplicateHashCacheService.class);

        AnalysisService analysisService = mock(AnalysisService.class);

        // Real FileSystemComicStorageFacade
        storageFacade = new FileSystemComicStorageFacade(
                cacheProperties,
                validationService,
                duplicateValidationService,
                hashCacheService,
                analysisService,
                metadataRepository,
                indexService
        );
    }

    @Test
    void saveComicStrip_shouldUpdateIndex() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 2, 10);
        byte[] imageData = new byte[] { 0x01, 0x02, 0x03, 0x04 };

        // Act - Save a comic strip
        boolean saved = storageFacade.saveComicStrip(COMIC, date, imageData);

        // Assert - File should exist
        assertThat(saved).isTrue();
        assertThat(storageFacade.comicStripExists(COMIC, date)).isTrue();

        // Assert - Index should be updated
        Optional<LocalDate> newest = storageFacade.getNewestDateWithComic(COMIC);
        assertThat(newest).isPresent();
        assertThat(newest.get()).isEqualTo(date);

        // Assert - Index file should exist on disk
        File indexFile = new File(tempDir.toFile(), COMIC_NAME + "/available-dates.json");
        assertThat(indexFile).exists();
    }

    @Test
    void saveMultipleComicStrips_shouldUpdateIndexForEach() {
        // Arrange
        LocalDate date1 = LocalDate.of(2026, 2, 8);
        LocalDate date2 = LocalDate.of(2026, 2, 9);
        LocalDate date3 = LocalDate.of(2026, 2, 10);
        byte[] imageData = new byte[] { 0x01, 0x02, 0x03, 0x04 };

        // Act - Save multiple strips
        assertThat(storageFacade.saveComicStrip(COMIC, date1, imageData)).isTrue();
        assertThat(storageFacade.saveComicStrip(COMIC, date2, imageData)).isTrue();
        assertThat(storageFacade.saveComicStrip(COMIC, date3, imageData)).isTrue();

        // Assert - All files should exist
        assertThat(storageFacade.comicStripExists(COMIC, date1)).isTrue();
        assertThat(storageFacade.comicStripExists(COMIC, date2)).isTrue();
        assertThat(storageFacade.comicStripExists(COMIC, date3)).isTrue();

        // Assert - Index should contain all dates
        assertThat(storageFacade.getOldestDateWithComic(COMIC)).hasValue(date1);
        assertThat(storageFacade.getNewestDateWithComic(COMIC)).hasValue(date3);

        // Assert - Navigation should work
        assertThat(storageFacade.getNextDateWithComic(COMIC, date1)).hasValue(date2);
        assertThat(storageFacade.getNextDateWithComic(COMIC, date2)).hasValue(date3);
        assertThat(storageFacade.getPreviousDateWithComic(COMIC, date3)).hasValue(date2);
        assertThat(storageFacade.getPreviousDateWithComic(COMIC, date2)).hasValue(date1);
    }

    @Test
    void saveComicStrip_shouldPersistIndexAcrossServiceRestart() {
        // Arrange
        LocalDate date1 = LocalDate.of(2026, 2, 9);
        LocalDate date2 = LocalDate.of(2026, 2, 10);
        byte[] imageData = new byte[] { 0x01, 0x02, 0x03, 0x04 };

        // Act - Save first strip
        assertThat(storageFacade.saveComicStrip(COMIC, date1, imageData)).isTrue();

        // Simulate service restart by creating new instances
        // (this mimics what happens when backend is redeployed)
        Gson gson = GsonUtils.createGson();
        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.setLocation(tempDir.toAbsolutePath().toString());
        ComicIndexService newIndexService = new ComicIndexService(gson, cacheProperties, metadataRepository);

        ValidationService validationService = mock(ValidationService.class);
        when(validationService.validateWithMinDimensions(any(byte[].class), anyInt(), anyInt()))
                .thenReturn(ImageValidationResult.success(ImageFormat.PNG, 200, 100, 1000));
        DuplicateValidationService duplicateValidationService = mock(DuplicateValidationService.class);
        when(duplicateValidationService.validateNoDuplicate(anyInt(), anyString(), any(LocalDate.class), any(byte[].class)))
                .thenReturn(DuplicateValidationResult.unique("test-hash"));
        DuplicateHashCacheService hashCacheService = mock(DuplicateHashCacheService.class);
        AnalysisService analysisService = mock(AnalysisService.class);

        FileSystemComicStorageFacade newStorageFacade = new FileSystemComicStorageFacade(
                cacheProperties,
                validationService,
                duplicateValidationService,
                hashCacheService,
                analysisService,
                metadataRepository,
                newIndexService
        );

        // Act - Save second strip after "restart"
        assertThat(newStorageFacade.saveComicStrip(COMIC, date2, imageData)).isTrue();

        // Assert - Both files should exist
        assertThat(newStorageFacade.comicStripExists(COMIC, date1)).isTrue();
        assertThat(newStorageFacade.comicStripExists(COMIC, date2)).isTrue();

        // Assert - Index should contain both dates
        assertThat(newStorageFacade.getOldestDateWithComic(COMIC)).hasValue(date1);
        assertThat(newStorageFacade.getNewestDateWithComic(COMIC)).hasValue(date2);

        // This is the CRITICAL assertion that reproduces the bug:
        // After restart, when we save a new file, the index should be updated
        // with BOTH the old date (from disk scan/rebuild) and the new date.
        assertThat(newStorageFacade.getNextDateWithComic(COMIC, date1)).hasValue(date2);
    }
}
