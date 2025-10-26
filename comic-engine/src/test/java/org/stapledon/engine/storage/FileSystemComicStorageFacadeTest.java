package org.stapledon.engine.storage;

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
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

    private FileSystemComicStorageFacade storageFacade;
    private File cacheRoot;
    private static final int COMIC_ID = 42;
    private static final String COMIC_NAME = "TestComic";
    private static final String COMIC_NAME_PARSED = "TestComic";
    private static final String YEAR_2023 = "2023";
    private static final LocalDate TEST_DATE = LocalDate.of(2023, 1, 15);

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        // Setup temp cache directory
        cacheRoot = tempDir.toFile();
        when(cacheProperties.getLocation()).thenReturn(cacheRoot.getAbsolutePath());

        // Mock image validation to always pass
        when(imageValidationService.validate(any(byte[].class)))
                .thenReturn(ImageValidationResult.success(ImageFormat.PNG, 100, 100, 1000));
        when(imageValidationService.validateWithMinDimensions(any(byte[].class), any(int.class), any(int.class)))
                .thenReturn(ImageValidationResult.success(ImageFormat.PNG, 100, 100, 1000));

        // Mock image analysis to return a test metadata
        when(imageAnalysisService.analyzeImage(any(byte[].class), anyString(), any(), any()))
                .thenReturn(createTestMetadata());

        // Mock metadata repository to return true
        when(imageMetadataRepository.saveMetadata(any())).thenReturn(true);

        // Mock duplicate validation to always return unique
        when(duplicateValidationService.validateNoDuplicate(any(int.class), anyString(), any(LocalDate.class), any(byte[].class)))
                .thenReturn(DuplicateValidationResult.unique("test-hash"));

        storageFacade = new FileSystemComicStorageFacade(cacheProperties, imageValidationService,
                duplicateValidationService, duplicateHashCacheService, imageAnalysisService, imageMetadataRepository);

        // Create test directory structure
        createTestDirectoryStructure();

        // Mock ImageUtils.getImageDto
        mockImageUtils();
    }
    
    private void mockImageUtils() {
        // This approach allows us to test without real image parsing
        // In a real test, we would need to have a test utility that replaces ImageUtils
        // or refactor the code to avoid static dependencies
        
        // No need to mock static methods for this test since we're testing
        // the storage operations and not the image processing logic
        // We'll need to revisit this when adding more comprehensive tests
        
        // For now, we'll skip the static mocking and focus on testing
        // the storage and retrieval logic
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
    void saveComicStrip_shouldCreateFileInCorrectLocation() throws IOException {
        // Arrange
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
    void saveAvatar_shouldCreateFileInCorrectLocation() throws IOException {
        // Arrange
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
        
        // Act
        Optional<LocalDate> result = storageFacade.getNextDateWithComic(COMIC_ID, COMIC_NAME, from);
        
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(LocalDate.of(2023, 1, 15));
    }
    
    @Test
    void getPreviousDateWithComic_shouldReturnPreviousAvailableDate() {
        // Arrange
        LocalDate from = LocalDate.of(2023, 1, 20);
        
        // Act
        Optional<LocalDate> result = storageFacade.getPreviousDateWithComic(COMIC_ID, COMIC_NAME, from);
        
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(LocalDate.of(2023, 1, 15));
    }
    
    @Test
    void getNewestDateWithComic_shouldReturnNewestAvailableDate() {
        // Act
        Optional<LocalDate> result = storageFacade.getNewestDateWithComic(COMIC_ID, COMIC_NAME);
        
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(LocalDate.of(2023, 1, 20));
    }
    
    @Test
    void getOldestDateWithComic_shouldReturnOldestAvailableDate() {
        // Act
        Optional<LocalDate> result = storageFacade.getOldestDateWithComic(COMIC_ID, COMIC_NAME);
        
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(LocalDate.of(2023, 1, 10));
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
    void saveComicStrip_shouldAddImageToCache_afterSuccessfulSave() throws IOException {
        // Arrange
        LocalDate date = LocalDate.of(2023, 2, 1);
        byte[] imageData = new byte[]{0x01, 0x02, 0x03, 0x04};

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
                anyString()  // File path will be generated
        );
    }

    @Test
    void saveComicStrip_shouldNotAddToCache_whenImageIsDuplicate() throws IOException {
        // Arrange
        LocalDate date = LocalDate.of(2023, 2, 1);
        byte[] imageData = new byte[]{0x01, 0x02, 0x03, 0x04};

        // Mock duplicate validation to return duplicate
        when(duplicateValidationService.validateNoDuplicate(any(int.class), anyString(), any(LocalDate.class), any(byte[].class)))
                .thenReturn(DuplicateValidationResult.duplicate("hash", LocalDate.of(2023, 1, 1), "/path/to/duplicate.png"));

        // Act
        boolean result = storageFacade.saveComicStrip(COMIC_ID, COMIC_NAME, date, imageData);

        // Assert
        assertThat(result).isTrue();  // Returns true because download was successful

        // Verify that addImageToCache was NOT called since it's a duplicate
        verify(duplicateHashCacheService, never()).addImageToCache(
                anyInt(),
                anyString(),
                any(LocalDate.class),
                any(byte[].class),
                anyString()
        );
    }

    @Test
    void saveComicStrip_shouldNotAddToCache_whenSaveFails() throws IOException {
        // Arrange
        LocalDate date = LocalDate.of(2023, 2, 1);
        byte[] imageData = new byte[]{0x01, 0x02, 0x03, 0x04};

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
                anyString()
        );
    }
}