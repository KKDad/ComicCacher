package org.stapledon.engine.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.DuplicateValidationResult;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.dto.ImageMetadata;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.service.DuplicateValidationService;
import org.stapledon.common.service.ValidationService;
import org.stapledon.engine.validation.DuplicateHashCacheService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Unit tests for FileSystemComicStorageFacade
 */
@ExtendWith(MockitoExtension.class)
class FileSystemComicStorageFacadeTest {

    @TempDir
    Path tempDir;

    @Mock
    private CacheProperties cacheProperties;

    @Mock
    private ValidationService imageValidationService;

    @Mock
    private DuplicateValidationService duplicateValidationService;

    @Mock
    private DuplicateHashCacheService duplicateHashCacheService;

    @Mock
    private org.stapledon.common.service.AnalysisService imageAnalysisService;

    @Mock
    private ImageMetadataRepository imageMetadataRepository;

    @Mock
    private ComicIndexService comicIndexService;

    private FileSystemComicStorageFacade storageFacade;
    private File cacheRoot;
    private static final int COMIC_ID = 42;
    private static final String COMIC_NAME = "TestComic";
    private static final String COMIC_NAME_PARSED = "TestComic";
    private static final String YEAR_2023 = "2023";
    private static final LocalDate TEST_DATE = LocalDate.of(2023, 1, 15);

    @BeforeEach
    void setUp() throws IOException {
        // Setup temp cache directory
        cacheRoot = tempDir.toFile();
        lenient().when(cacheProperties.getLocation()).thenReturn(cacheRoot.getAbsolutePath());

        storageFacade = new FileSystemComicStorageFacade(cacheProperties, imageValidationService,
                duplicateValidationService, duplicateHashCacheService, imageAnalysisService, imageMetadataRepository,
                comicIndexService);

        // Create test directory structure
        createTestDirectoryStructure();
    }

    private void configureMocksForSave() {
        // Mock image validation to always pass
        lenient().when(imageValidationService.validate(any(byte[].class)))
                .thenReturn(ImageValidationResult.success(ImageFormat.PNG, 100, 100, 1000));
        lenient()
                .when(imageValidationService.validateWithMinDimensions(any(byte[].class), any(int.class),
                        any(int.class)))
                .thenReturn(ImageValidationResult.success(ImageFormat.PNG, 100, 100, 1000));

        // Mock image analysis to return a test metadata
        lenient()
                .when(imageAnalysisService.analyzeImage(anyInt(), anyString(), any(byte[].class), anyString(), any(),
                        any()))
                .thenReturn(createTestMetadata());

        // Mock metadata repository to return true
        lenient().when(imageMetadataRepository.saveMetadata(any())).thenReturn(true);

        // Mock duplicate validation to always return unique
        lenient().when(duplicateValidationService.validateNoDuplicate(any(int.class), anyString(), any(LocalDate.class),
                any(byte[].class)))
                .thenReturn(DuplicateValidationResult.unique("test-hash"));
    }

    /**
     * Creates a test comic directory with daily comic files
     */
    private void createTestDirectoryStructure() {
        try {
            // Create comic directory
            File comicDir = new File(cacheRoot, COMIC_NAME_PARSED);
            comicDir.mkdir();

            // Create year directory
            File yearDir = new File(comicDir, YEAR_2023);
            yearDir.mkdir();

            // Create test comic files
            createTestComicFile(yearDir, LocalDate.of(2023, 1, 10));
            createTestComicFile(yearDir, LocalDate.of(2023, 1, 15));
            createTestComicFile(yearDir, LocalDate.of(2023, 1, 20));

            // Create avatar file
            File avatarFile = new File(comicDir, "avatar.png");
            Files.writeString(avatarFile.toPath(), "Test avatar content");
        } catch (IOException e) {
            throw new RuntimeException("Failed to set up test directory structure", e);
        }
    }

    private void createTestComicFile(File yearDir, LocalDate date) throws IOException {
        String fileName = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".png";
        File comicFile = new File(yearDir, fileName);
        Files.writeString(comicFile.toPath(), "Test comic content for " + fileName);
    }

    @Test
    void saveComicStrip_shouldCreateFileInCorrectLocation() throws Exception {
        // Arrange
        configureMocksForSave();
        LocalDate date = LocalDate.of(2023, 2, 1);
        byte[] imageData = "Test image data".getBytes();

        // Act
        boolean result = storageFacade.saveComicStrip(COMIC_ID, COMIC_NAME, date, imageData);

        // Assert
        assertThat(result).isTrue();

        File expectedFile = new File(cacheRoot, COMIC_NAME_PARSED + "/2023/2023-02-01.png");
        assertThat(expectedFile).exists();
        assertThat(Files.readAllBytes(expectedFile.toPath())).isEqualTo(imageData);
    }

    @Test
    void saveAvatar_shouldCreateFileInCorrectLocation() throws Exception {
        // Arrange
        configureMocksForSave();
        byte[] avatarData = "New avatar data".getBytes();

        // Act
        boolean result = storageFacade.saveAvatar(COMIC_ID, COMIC_NAME, avatarData);

        // Assert
        assertThat(result).isTrue();

        File expectedFile = new File(cacheRoot, COMIC_NAME_PARSED + "/avatar.png");
        assertThat(expectedFile).exists();
        assertThat(Files.readAllBytes(expectedFile.toPath())).isEqualTo(avatarData);
    }

    // Test removed - on-demand downloads via CacheMissEvent no longer supported

    @Test
    void getNextDateWithComic_shouldReturnNextAvailableDate() {
        // Arrange
        LocalDate from = LocalDate.of(2023, 1, 10);
        LocalDate next = LocalDate.of(2023, 1, 15);
        when(comicIndexService.getNextDate(COMIC_ID, COMIC_NAME, from)).thenReturn(Optional.of(next));

        // Act
        Optional<LocalDate> result = storageFacade.getNextDateWithComic(COMIC_ID, COMIC_NAME, from);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(next);
        verify(comicIndexService).getNextDate(COMIC_ID, COMIC_NAME, from);
    }

    @Test
    void getPreviousDateWithComic_shouldReturnPreviousAvailableDate() {
        // Arrange
        LocalDate from = LocalDate.of(2023, 1, 20);
        LocalDate prev = LocalDate.of(2023, 1, 15);
        when(comicIndexService.getPreviousDate(COMIC_ID, COMIC_NAME, from)).thenReturn(Optional.of(prev));

        // Act
        Optional<LocalDate> result = storageFacade.getPreviousDateWithComic(COMIC_ID, COMIC_NAME, from);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(prev);
        verify(comicIndexService).getPreviousDate(COMIC_ID, COMIC_NAME, from);
    }

    @Test
    void getNewestDateWithComic_shouldReturnNewestAvailableDate() {
        // Arrange
        LocalDate newest = LocalDate.of(2023, 1, 20);
        when(comicIndexService.getNewestDate(COMIC_ID, COMIC_NAME)).thenReturn(Optional.of(newest));

        // Act
        Optional<LocalDate> result = storageFacade.getNewestDateWithComic(COMIC_ID, COMIC_NAME);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(newest);
        verify(comicIndexService).getNewestDate(COMIC_ID, COMIC_NAME);
    }

    @Test
    void getOldestDateWithComic_shouldReturnOldestAvailableDate() {
        // Arrange
        LocalDate oldest = LocalDate.of(2023, 1, 10);
        when(comicIndexService.getOldestDate(COMIC_ID, COMIC_NAME)).thenReturn(Optional.of(oldest));

        // Act
        Optional<LocalDate> result = storageFacade.getOldestDateWithComic(COMIC_ID, COMIC_NAME);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(oldest);
        verify(comicIndexService).getOldestDate(COMIC_ID, COMIC_NAME);
    }

    @Test
    void getOldestDateWithComic_shouldSkipEmptyYearDirectories() throws Exception {
        // Note: This logic now lives in ComicIndexService.
        // We verify that the facade correctly delegates to the service.
        LocalDate oldest = LocalDate.of(2023, 1, 10);
        when(comicIndexService.getOldestDate(COMIC_ID, COMIC_NAME)).thenReturn(Optional.of(oldest));

        // Act
        Optional<LocalDate> result = storageFacade.getOldestDateWithComic(COMIC_ID, COMIC_NAME);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(oldest);
    }

    @Test
    void navigation_regressionTest_shouldHandleYearBoundaries() throws Exception {
        // Arrange: mock consecutive navigation back calls
        LocalDate jan2 = LocalDate.of(2026, 1, 2);
        LocalDate jan1 = LocalDate.of(2026, 1, 1);
        LocalDate dec31 = LocalDate.of(2025, 12, 31);
        LocalDate dec30 = LocalDate.of(2025, 12, 30);

        when(comicIndexService.getPreviousDate(COMIC_ID, COMIC_NAME, jan2)).thenReturn(Optional.of(jan1));
        when(comicIndexService.getPreviousDate(COMIC_ID, COMIC_NAME, jan1)).thenReturn(Optional.of(dec31));
        when(comicIndexService.getPreviousDate(COMIC_ID, COMIC_NAME, dec31)).thenReturn(Optional.of(dec30));

        // Act & Assert
        assertThat(storageFacade.getPreviousDateWithComic(COMIC_ID, COMIC_NAME, jan2)).hasValue(jan1);
        assertThat(storageFacade.getPreviousDateWithComic(COMIC_ID, COMIC_NAME, jan1)).hasValue(dec31);
        assertThat(storageFacade.getPreviousDateWithComic(COMIC_ID, COMIC_NAME, dec31)).hasValue(dec30);
    }

    @Test
    void navigation_shouldIgnoreInvalidFilenames() throws Exception {
        // Note: This logic now lives in ComicIndexService.
        LocalDate oldest = LocalDate.of(2023, 1, 10);
        when(comicIndexService.getOldestDate(COMIC_ID, COMIC_NAME)).thenReturn(Optional.of(oldest));

        // Act
        Optional<LocalDate> result = storageFacade.getOldestDateWithComic(COMIC_ID, COMIC_NAME);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(oldest);
    }

    @Test
    void comicStripExists_shouldReturnTrueWhenExists() {
        // Act
        boolean result = storageFacade.comicStripExists(COMIC_ID, COMIC_NAME, TEST_DATE);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void comicStripExists_shouldReturnFalseWhenNotExists() {
        // Arrange
        LocalDate missingDate = LocalDate.of(2023, 3, 1);

        // Act
        boolean result = storageFacade.comicStripExists(COMIC_ID, COMIC_NAME, missingDate);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void getYearsWithContent_shouldReturnCorrectYears() {
        // Act
        List<String> years = storageFacade.getYearsWithContent(COMIC_ID, COMIC_NAME);

        // Assert
        assertThat(years).containsExactly(YEAR_2023);
    }

    @Test
    void getStorageSize_shouldReturnCorrectSize() {
        // Act
        long size = storageFacade.getStorageSize(COMIC_ID, COMIC_NAME);

        // Assert
        assertThat(size).isGreaterThan(0);
    }

    @Test
    void deleteComic_shouldRemoveAllComicFiles() {
        // Act
        boolean result = storageFacade.deleteComic(COMIC_ID, COMIC_NAME);

        // Assert
        assertThat(result).isTrue();

        File comicDir = new File(cacheRoot, COMIC_NAME_PARSED);
        assertThat(comicDir).doesNotExist();
    }

    private ImageMetadata createTestMetadata() {
        return ImageMetadata.builder()
                .filePath("/test/path.png")
                .format(ImageFormat.PNG)
                .width(100)
                .height(100)
                .sizeInBytes(1000)
                .colorMode(ImageMetadata.ColorMode.COLOR)
                .samplePercentage(5.0)
                .captureTimestamp(LocalDateTime.now())
                .sourceUrl(null)
                .build();
    }

    @Test
    void saveComicStrip_shouldAddImageToCache_afterSuccessfulSave() throws Exception {
        // Arrange
        configureMocksForSave();
        LocalDate date = LocalDate.of(2023, 2, 1);
        byte[] imageData = new byte[] { 0x01, 0x02, 0x03, 0x04 };

        // Act
        boolean result = storageFacade.saveComicStrip(COMIC_ID, COMIC_NAME, date, imageData);

        // Assert
        assertThat(result).isTrue();

        // Verify that addImageToCache was called with correct parameters
        verify(duplicateHashCacheService).addImageToCache(
                eq(COMIC_ID),
                eq(COMIC_NAME),
                eq(date),
                eq(imageData),
                anyString() // File path will be generated
        );

        // Verify that the date was added to the index
        verify(comicIndexService).addDateToIndex(COMIC_ID, COMIC_NAME, date);
    }

    @Test
    void saveComicStrip_shouldNotAddToCache_whenImageIsDuplicate() throws Exception {
        // Arrange
        configureMocksForSave();
        LocalDate date = LocalDate.of(2023, 2, 1);
        byte[] imageData = new byte[] { 0x01, 0x02, 0x03, 0x04 };

        // Mock duplicate validation to return duplicate
        when(duplicateValidationService.validateNoDuplicate(any(int.class), anyString(), any(LocalDate.class),
                any(byte[].class)))
                .thenReturn(DuplicateValidationResult.duplicate("hash", LocalDate.of(2023, 1, 1),
                        "/path/to/duplicate.png"));

        // Act
        boolean result = storageFacade.saveComicStrip(COMIC_ID, COMIC_NAME, date, imageData);

        // Assert
        assertThat(result).isTrue(); // Returns true because download was successful

        // Verify that addImageToCache was NOT called since it's a duplicate
        verify(duplicateHashCacheService, never()).addImageToCache(
                anyInt(),
                anyString(),
                any(LocalDate.class),
                any(byte[].class),
                anyString());
    }

    @Test
    void saveComicStrip_shouldNotAddToCache_whenSaveFails() throws Exception {
        // Arrange
        configureMocksForSave();
        LocalDate date = LocalDate.of(2023, 2, 1);
        byte[] imageData = new byte[] { 0x01, 0x02, 0x03, 0x04 };

        // Mock validation to fail
        when(imageValidationService.validateWithMinDimensions(any(byte[].class), any(int.class), any(int.class)))
                .thenReturn(ImageValidationResult.failure("Validation failed"));

        // Act
        boolean result = storageFacade.saveComicStrip(COMIC_ID, COMIC_NAME, date, imageData);

        // Assert
        assertThat(result).isFalse();

        // Verify that addImageToCache was NOT called since validation failed
        verify(duplicateHashCacheService, never()).addImageToCache(
                anyInt(),
                anyString(),
                any(LocalDate.class),
                any(byte[].class),
                anyString());
    }
}
