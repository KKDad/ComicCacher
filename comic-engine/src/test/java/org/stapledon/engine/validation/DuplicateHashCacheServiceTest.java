package org.stapledon.engine.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.HashAlgorithm;
import org.stapledon.common.dto.ImageHashRecord;
import org.stapledon.common.service.ImageHasher;
import org.stapledon.engine.storage.DuplicateImageHashRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DuplicateHashCacheService.
 * Tests cache management, backfill, and algorithm change detection.
 */
@ExtendWith(MockitoExtension.class)
class DuplicateHashCacheServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private DuplicateImageHashRepository hashRepository;

    @Mock
    private ImageHasherFactory imageHasherFactory;

    @Mock
    private ImageHasher imageHasher;

    @Mock
    private CacheProperties cacheProperties;

    private DuplicateHashCacheService service;

    private static final int COMIC_ID = 42;
    private static final String COMIC_NAME = "TestComic";
    private static final int YEAR = 2023;
    private static final String TEST_HASH = "abc123def456";
    private static final HashAlgorithm CURRENT_ALGORITHM = HashAlgorithm.DIFFERENCE_HASH;

    @BeforeEach
    void setUp() {
        service = new DuplicateHashCacheService(hashRepository, imageHasherFactory, cacheProperties);
    }

    @Test
    void testLoadHashesWithBackfill_EmptyCache_NoExistingImages_ReturnsEmptyMap() {
        // Given
        Map<String, ImageHashRecord> emptyMap = new ConcurrentHashMap<>();
        when(hashRepository.loadHashes(COMIC_ID, COMIC_NAME, YEAR)).thenReturn(emptyMap);

        File yearDir = tempDir.resolve(COMIC_NAME).resolve(String.valueOf(YEAR)).toFile();
        when(hashRepository.getYearDirectory(COMIC_ID, COMIC_NAME, YEAR)).thenReturn(yearDir);

        // When
        Map<String, ImageHashRecord> result = service.loadHashesWithBackfill(COMIC_ID, COMIC_NAME, YEAR);

        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty when no images exist");
        verify(hashRepository, never()).replaceHashes(anyInt(), anyString(), anyInt(), any());
    }

    @Test
    void testLoadHashesWithBackfill_EmptyCache_WithExistingImages_BackfillsCache() throws IOException {
        // Given
        when(cacheProperties.getHashAlgorithm()).thenReturn(CURRENT_ALGORITHM);
        when(imageHasherFactory.getImageHasher()).thenReturn(imageHasher);

        Map<String, ImageHashRecord> emptyMap = new ConcurrentHashMap<>();
        when(hashRepository.loadHashes(COMIC_ID, COMIC_NAME, YEAR)).thenReturn(emptyMap);

        // Create test directory with images
        File yearDir = tempDir.resolve(COMIC_NAME).resolve(String.valueOf(YEAR)).toFile();
        yearDir.mkdirs();
        File image1 = new File(yearDir, "2023-01-15.png");
        File image2 = new File(yearDir, "2023-01-16.png");
        Files.write(image1.toPath(), "test image 1".getBytes());
        Files.write(image2.toPath(), "test image 2".getBytes());

        when(hashRepository.getYearDirectory(COMIC_ID, COMIC_NAME, YEAR)).thenReturn(yearDir);
        when(imageHasher.calculateHash(any(byte[].class))).thenReturn("hash1", "hash2");

        // When
        Map<String, ImageHashRecord> result = service.loadHashesWithBackfill(COMIC_ID, COMIC_NAME, YEAR);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should have backfilled 2 images");
        verify(hashRepository).replaceHashes(eq(COMIC_ID), eq(COMIC_NAME), eq(YEAR), any());
        verify(imageHasher, times(2)).calculateHash(any(byte[].class));
    }

    @Test
    void testLoadHashesWithBackfill_AlgorithmChanged_RebuildsCache() throws IOException {
        // Given
        when(cacheProperties.getHashAlgorithm()).thenReturn(CURRENT_ALGORITHM);
        when(imageHasherFactory.getImageHasher()).thenReturn(imageHasher);

        Map<String, ImageHashRecord> existingMap = new ConcurrentHashMap<>();
        ImageHashRecord oldRecord = ImageHashRecord.builder()
                .hash("old-hash")
                .date(LocalDate.of(2023, 1, 15))
                .filePath("/path/to/image.png")
                .algorithm(HashAlgorithm.MD5)  // Different algorithm
                .build();
        existingMap.put("old-hash", oldRecord);

        when(hashRepository.loadHashes(COMIC_ID, COMIC_NAME, YEAR)).thenReturn(existingMap);

        // Create test directory with images
        File yearDir = tempDir.resolve(COMIC_NAME).resolve(String.valueOf(YEAR)).toFile();
        yearDir.mkdirs();
        File image1 = new File(yearDir, "2023-01-15.png");
        Files.write(image1.toPath(), "test image 1".getBytes());

        when(hashRepository.getYearDirectory(COMIC_ID, COMIC_NAME, YEAR)).thenReturn(yearDir);
        when(imageHasher.calculateHash(any(byte[].class))).thenReturn("new-hash");

        // When
        Map<String, ImageHashRecord> result = service.loadHashesWithBackfill(COMIC_ID, COMIC_NAME, YEAR);

        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.containsKey("new-hash"), "Should have new hash with current algorithm");
        verify(hashRepository).replaceHashes(eq(COMIC_ID), eq(COMIC_NAME), eq(YEAR), any());
        verify(imageHasher).calculateHash(any(byte[].class));
    }

    @Test
    void testLoadHashesWithBackfill_SameAlgorithm_DoesNotRebuild() {
        // Given
        when(cacheProperties.getHashAlgorithm()).thenReturn(CURRENT_ALGORITHM);

        Map<String, ImageHashRecord> existingMap = new ConcurrentHashMap<>();
        ImageHashRecord existingRecord = ImageHashRecord.builder()
                .hash(TEST_HASH)
                .date(LocalDate.of(2023, 1, 15))
                .filePath("/path/to/image.png")
                .algorithm(CURRENT_ALGORITHM)  // Same algorithm
                .build();
        existingMap.put(TEST_HASH, existingRecord);

        when(hashRepository.loadHashes(COMIC_ID, COMIC_NAME, YEAR)).thenReturn(existingMap);

        // When
        Map<String, ImageHashRecord> result = service.loadHashesWithBackfill(COMIC_ID, COMIC_NAME, YEAR);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Should return existing cache");
        assertTrue(result.containsKey(TEST_HASH), "Should contain existing hash");
        verify(hashRepository, never()).replaceHashes(anyInt(), anyString(), anyInt(), any());
    }

    @Test
    void testFindByHash_LoadsWithBackfillAndFinds() {
        // Given
        when(cacheProperties.getHashAlgorithm()).thenReturn(CURRENT_ALGORITHM);

        Map<String, ImageHashRecord> existingMap = new ConcurrentHashMap<>();
        ImageHashRecord record = ImageHashRecord.builder()
                .hash(TEST_HASH)
                .date(LocalDate.of(2023, 1, 15))
                .filePath("/path/to/image.png")
                .algorithm(CURRENT_ALGORITHM)
                .build();
        existingMap.put(TEST_HASH, record);

        when(hashRepository.loadHashes(COMIC_ID, COMIC_NAME, YEAR)).thenReturn(existingMap);
        when(hashRepository.findByHash(COMIC_ID, COMIC_NAME, YEAR, TEST_HASH)).thenReturn(Optional.of(record));

        // When
        Optional<ImageHashRecord> result = service.findByHash(COMIC_ID, COMIC_NAME, YEAR, TEST_HASH);

        // Then
        assertTrue(result.isPresent(), "Should find the hash");
        assertEquals(TEST_HASH, result.get().getHash(), "Hash should match");
        verify(hashRepository).findByHash(COMIC_ID, COMIC_NAME, YEAR, TEST_HASH);
    }

    @Test
    void testFindByHash_NotFound_ReturnsEmpty() {
        // Given
        Map<String, ImageHashRecord> emptyMap = new ConcurrentHashMap<>();
        when(hashRepository.loadHashes(COMIC_ID, COMIC_NAME, YEAR)).thenReturn(emptyMap);

        File yearDir = tempDir.resolve(COMIC_NAME).resolve(String.valueOf(YEAR)).toFile();
        when(hashRepository.getYearDirectory(COMIC_ID, COMIC_NAME, YEAR)).thenReturn(yearDir);
        when(hashRepository.findByHash(COMIC_ID, COMIC_NAME, YEAR, "nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<ImageHashRecord> result = service.findByHash(COMIC_ID, COMIC_NAME, YEAR, "nonexistent");

        // Then
        assertFalse(result.isPresent(), "Should not find non-existent hash");
    }

    @Test
    void testAddHash_DelegatesToRepository() {
        // Given
        ImageHashRecord record = ImageHashRecord.builder()
                .hash(TEST_HASH)
                .date(LocalDate.of(2023, 1, 15))
                .filePath("/path/to/image.png")
                .algorithm(CURRENT_ALGORITHM)
                .build();

        // When
        service.addHash(COMIC_ID, COMIC_NAME, YEAR, record);

        // Then
        verify(hashRepository).addHash(COMIC_ID, COMIC_NAME, YEAR, record);
    }

    @Test
    void testAddImageToCache_SuccessfulHash_AddsToRepository() {
        // Given
        when(cacheProperties.getHashAlgorithm()).thenReturn(CURRENT_ALGORITHM);
        when(imageHasherFactory.getImageHasher()).thenReturn(imageHasher);

        LocalDate date = LocalDate.of(2023, 1, 15);
        byte[] imageData = "test image data".getBytes();
        String filePath = "/path/to/image.png";

        when(imageHasher.calculateHash(imageData)).thenReturn(TEST_HASH);

        // When
        service.addImageToCache(COMIC_ID, COMIC_NAME, date, imageData, filePath);

        // Then
        ArgumentCaptor<ImageHashRecord> recordCaptor = ArgumentCaptor.forClass(ImageHashRecord.class);
        verify(hashRepository).addHash(eq(COMIC_ID), eq(COMIC_NAME), eq(YEAR), recordCaptor.capture());

        ImageHashRecord capturedRecord = recordCaptor.getValue();
        assertEquals(TEST_HASH, capturedRecord.getHash(), "Hash should match");
        assertEquals(date, capturedRecord.getDate(), "Date should match");
        assertEquals(filePath, capturedRecord.getFilePath(), "File path should match");
        assertEquals(CURRENT_ALGORITHM, capturedRecord.getAlgorithm(), "Algorithm should match");
    }

    @Test
    void testAddImageToCache_HashCalculationFails_DoesNotAddToRepository() {
        // Given
        when(imageHasherFactory.getImageHasher()).thenReturn(imageHasher);

        LocalDate date = LocalDate.of(2023, 1, 15);
        byte[] imageData = "test image data".getBytes();
        String filePath = "/path/to/image.png";

        when(imageHasher.calculateHash(imageData)).thenReturn(null);

        // When
        service.addImageToCache(COMIC_ID, COMIC_NAME, date, imageData, filePath);

        // Then
        verify(hashRepository, never()).addHash(anyInt(), anyString(), anyInt(), any());
    }

    @Test
    void testAddImageToCache_UsesCorrectYear() {
        // Given
        when(cacheProperties.getHashAlgorithm()).thenReturn(CURRENT_ALGORITHM);
        when(imageHasherFactory.getImageHasher()).thenReturn(imageHasher);

        LocalDate date = LocalDate.of(2025, 12, 31);
        byte[] imageData = "test image data".getBytes();
        String filePath = "/path/to/image.png";

        when(imageHasher.calculateHash(imageData)).thenReturn(TEST_HASH);

        // When
        service.addImageToCache(COMIC_ID, COMIC_NAME, date, imageData, filePath);

        // Then
        verify(hashRepository).addHash(eq(COMIC_ID), eq(COMIC_NAME), eq(2025), any());
    }

    @Test
    void testBackfillExistingImages_IgnoresNonPngFiles() throws IOException {
        // Given
        when(cacheProperties.getHashAlgorithm()).thenReturn(CURRENT_ALGORITHM);
        when(imageHasherFactory.getImageHasher()).thenReturn(imageHasher);

        Map<String, ImageHashRecord> emptyMap = new ConcurrentHashMap<>();
        when(hashRepository.loadHashes(COMIC_ID, COMIC_NAME, YEAR)).thenReturn(emptyMap);

        // Create test directory with mixed files
        File yearDir = tempDir.resolve(COMIC_NAME).resolve(String.valueOf(YEAR)).toFile();
        yearDir.mkdirs();
        File pngFile = new File(yearDir, "2023-01-15.png");
        File jpgFile = new File(yearDir, "2023-01-16.jpg");
        File txtFile = new File(yearDir, "readme.txt");
        Files.write(pngFile.toPath(), "test image".getBytes());
        Files.write(jpgFile.toPath(), "test jpeg".getBytes());
        Files.write(txtFile.toPath(), "test text".getBytes());

        when(hashRepository.getYearDirectory(COMIC_ID, COMIC_NAME, YEAR)).thenReturn(yearDir);
        when(imageHasher.calculateHash(any(byte[].class))).thenReturn("hash1");

        // When
        Map<String, ImageHashRecord> result = service.loadHashesWithBackfill(COMIC_ID, COMIC_NAME, YEAR);

        // Then
        assertEquals(1, result.size(), "Should only backfill PNG files");
        verify(imageHasher, times(1)).calculateHash(any(byte[].class));
    }

    @Test
    void testBackfillExistingImages_HandlesInvalidFilenames() throws IOException {
        // Given
        Map<String, ImageHashRecord> emptyMap = new ConcurrentHashMap<>();
        when(hashRepository.loadHashes(COMIC_ID, COMIC_NAME, YEAR)).thenReturn(emptyMap);

        // Create test directory with invalid filename
        File yearDir = tempDir.resolve(COMIC_NAME).resolve(String.valueOf(YEAR)).toFile();
        yearDir.mkdirs();
        File invalidFile = new File(yearDir, "invalid-date.png");
        Files.write(invalidFile.toPath(), "test image".getBytes());

        when(hashRepository.getYearDirectory(COMIC_ID, COMIC_NAME, YEAR)).thenReturn(yearDir);

        // When
        Map<String, ImageHashRecord> result = service.loadHashesWithBackfill(COMIC_ID, COMIC_NAME, YEAR);

        // Then
        assertTrue(result.isEmpty(), "Should not backfill files with invalid date format");
        verify(imageHasher, never()).calculateHash(any(byte[].class));
    }

    @Test
    void testLoadHashesWithBackfill_NullAlgorithmInExistingRecord_DoesNotCrash() {
        // Given
        Map<String, ImageHashRecord> existingMap = new ConcurrentHashMap<>();
        ImageHashRecord recordWithNullAlgorithm = ImageHashRecord.builder()
                .hash(TEST_HASH)
                .date(LocalDate.of(2023, 1, 15))
                .filePath("/path/to/image.png")
                .algorithm(null)  // Null algorithm
                .build();
        existingMap.put(TEST_HASH, recordWithNullAlgorithm);

        when(hashRepository.loadHashes(COMIC_ID, COMIC_NAME, YEAR)).thenReturn(existingMap);

        // When
        Map<String, ImageHashRecord> result = service.loadHashesWithBackfill(COMIC_ID, COMIC_NAME, YEAR);

        // Then
        assertNotNull(result, "Should handle null algorithm gracefully");
        assertEquals(1, result.size(), "Should still return the existing cache");
    }
}
