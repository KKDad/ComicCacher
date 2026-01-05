package org.stapledon.engine.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.DuplicateValidationResult;
import org.stapledon.common.dto.ImageHashRecord;
import org.stapledon.common.service.ImageHasher;

import java.time.LocalDate;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class DuplicateImageValidationServiceTest {

    @Mock
    private DuplicateHashCacheService hashCacheService;

    @Mock
    private ImageHasherFactory imageHasherFactory;

    @Mock
    private ImageHasher imageHasher;

    @Mock
    private CacheProperties cacheProperties;

    private DuplicateImageValidationService service;

    private final byte[] testImageData = "test-image-data".getBytes();
    private final String testHash = "abc123def456";
    private final int comicId = 1;
    private final String comicName = "Test Comic";
    private final LocalDate testDate = LocalDate.of(2024, 6, 15);

    @BeforeEach
    void setUp() {
        service = new DuplicateImageValidationService(hashCacheService, imageHasherFactory, cacheProperties);
    }

    @Test
    void shouldReturnUniqueWhenDuplicateDetectionDisabled() {
        // Arrange
        when(cacheProperties.isDuplicateDetectionEnabled()).thenReturn(false);

        // Act
        DuplicateValidationResult result = service.validateNoDuplicate(
                comicId, comicName, testDate, testImageData);

        // Assert
        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.getHash()).isEqualTo("disabled");
        verify(imageHasherFactory, never()).getImageHasher();
        verify(hashCacheService, never()).findByHash(anyInt(), anyString(), anyInt(), anyString());
    }

    @Test
    void shouldReturnUniqueWhenHashCalculationFails() {
        // Arrange
        when(cacheProperties.isDuplicateDetectionEnabled()).thenReturn(true);
        when(imageHasherFactory.getImageHasher()).thenReturn(imageHasher);
        when(imageHasher.calculateHash(testImageData)).thenReturn(null);

        // Act
        DuplicateValidationResult result = service.validateNoDuplicate(
                comicId, comicName, testDate, testImageData);

        // Assert
        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.getHash()).isEqualTo("hash-failed");
        verify(hashCacheService, never()).findByHash(anyInt(), anyString(), anyInt(), anyString());
    }

    @Test
    void shouldReturnUniqueWhenNoExistingHashFound() {
        // Arrange
        when(cacheProperties.isDuplicateDetectionEnabled()).thenReturn(true);
        when(imageHasherFactory.getImageHasher()).thenReturn(imageHasher);
        when(imageHasher.calculateHash(testImageData)).thenReturn(testHash);
        when(hashCacheService.findByHash(comicId, comicName, testDate.getYear(), testHash))
                .thenReturn(Optional.empty());

        // Act
        DuplicateValidationResult result = service.validateNoDuplicate(
                comicId, comicName, testDate, testImageData);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.getHash()).isNotNull();
        assertThat(result.getHash()).isEqualTo(testHash);
    }

    @Test
    void shouldReturnUniqueWhenHashMatchesSameDate() {
        // Arrange
        ImageHashRecord existingRecord = ImageHashRecord.builder()
                .date(testDate)
                .hash(testHash)
                .filePath("/comics/test/2024/2024-06-15.png")
                .build();

        when(cacheProperties.isDuplicateDetectionEnabled()).thenReturn(true);
        when(imageHasherFactory.getImageHasher()).thenReturn(imageHasher);
        when(imageHasher.calculateHash(testImageData)).thenReturn(testHash);
        when(hashCacheService.findByHash(comicId, comicName, testDate.getYear(), testHash))
                .thenReturn(Optional.of(existingRecord));

        // Act
        DuplicateValidationResult result = service.validateNoDuplicate(
                comicId, comicName, testDate, testImageData);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.getHash()).isNotNull();
        assertThat(result.getHash()).isEqualTo(testHash);
    }

    @Test
    void shouldReturnDuplicateWhenHashMatchesDifferentDate() {
        // Arrange
        LocalDate existingDate = LocalDate.of(2024, 6, 10);
        String existingFilePath = "/comics/test/2024/2024-06-10.png";

        ImageHashRecord existingRecord = ImageHashRecord.builder()
                .date(existingDate)
                .hash(testHash)
                .filePath(existingFilePath)
                .build();

        when(cacheProperties.isDuplicateDetectionEnabled()).thenReturn(true);
        when(imageHasherFactory.getImageHasher()).thenReturn(imageHasher);
        when(imageHasher.calculateHash(testImageData)).thenReturn(testHash);
        when(hashCacheService.findByHash(comicId, comicName, testDate.getYear(), testHash))
                .thenReturn(Optional.of(existingRecord));

        // Act
        DuplicateValidationResult result = service.validateNoDuplicate(
                comicId, comicName, testDate, testImageData);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.getHash()).isNotNull();
        assertThat(result.getHash()).isEqualTo(testHash);
        assertThat(result.getDuplicateDate()).isNotNull();
        assertThat(result.getDuplicateDate()).isEqualTo(existingDate);
        assertThat(result.getDuplicateFilePath()).isNotNull();
        assertThat(result.getDuplicateFilePath()).isEqualTo(existingFilePath);
    }

    @Test
    void shouldUseConfiguredYear() {
        // Arrange
        LocalDate dateInDifferentYear = LocalDate.of(2023, 12, 25);
        when(cacheProperties.isDuplicateDetectionEnabled()).thenReturn(true);
        when(imageHasherFactory.getImageHasher()).thenReturn(imageHasher);
        when(imageHasher.calculateHash(testImageData)).thenReturn(testHash);
        when(hashCacheService.findByHash(comicId, comicName, 2023, testHash))
                .thenReturn(Optional.empty());

        // Act
        DuplicateValidationResult result = service.validateNoDuplicate(
                comicId, comicName, dateInDifferentYear, testImageData);

        // Assert
        assertThat(result.isDuplicate()).isFalse();
        verify(hashCacheService).findByHash(comicId, comicName, 2023, testHash);
    }

    @Test
    void shouldHandleEmptyImageData() {
        // Arrange
        byte[] emptyData = new byte[0];
        when(cacheProperties.isDuplicateDetectionEnabled()).thenReturn(true);
        when(imageHasherFactory.getImageHasher()).thenReturn(imageHasher);
        when(imageHasher.calculateHash(emptyData)).thenReturn(testHash);
        when(hashCacheService.findByHash(anyInt(), anyString(), anyInt(), anyString()))
                .thenReturn(Optional.empty());

        // Act
        DuplicateValidationResult result = service.validateNoDuplicate(
                comicId, comicName, testDate, emptyData);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.getHash()).isNotNull();
        verify(imageHasher).calculateHash(emptyData);
    }

    @Test
    void shouldHaveValidToString() {
        // Act
        String toString = service.toString();

        // Assert
        assertThat(toString).isNotNull();
    }
}
