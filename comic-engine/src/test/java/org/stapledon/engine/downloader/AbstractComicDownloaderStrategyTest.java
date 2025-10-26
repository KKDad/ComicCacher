package org.stapledon.engine.downloader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.service.ValidationService;

import java.time.LocalDate;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AbstractComicDownloaderStrategyTest {

    @Mock
    private InspectorService webInspector;

    @Mock
    private ValidationService imageValidationService;

    private TestComicDownloaderStrategy strategy;
    private final byte[] validImageData = "valid-image-data".getBytes();
    private final byte[] emptyImageData = new byte[0];

    @BeforeEach
    void setUp() {
        strategy = new TestComicDownloaderStrategy("test-source", webInspector, imageValidationService);
    }

    @Test
    void shouldDownloadComicSuccessfully() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Test Comic")
                .source("test-source")
                .sourceIdentifier("test-comic")
                .date(LocalDate.now())
                .build();

        ImageValidationResult validationResult = ImageValidationResult.success(
                ImageFormat.PNG, 800, 600, validImageData.length);

        strategy.setMockImageData(validImageData);
        when(imageValidationService.validate(validImageData)).thenReturn(validationResult);

        // Act
        ComicDownloadResult result = strategy.downloadComic(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getImageData());
        assertEquals(validImageData, result.getImageData());
        assertNotNull(result.getRequest());
        assertEquals(request, result.getRequest());
        assertEquals(ImageFormat.PNG, validationResult.getFormat());
        assertEquals(800, validationResult.getWidth());
        assertEquals(600, validationResult.getHeight());
    }

    @Test
    void shouldFailWhenImageDataIsNull() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Test Comic")
                .source("test-source")
                .sourceIdentifier("test-comic")
                .date(LocalDate.now())
                .build();

        strategy.setMockImageData(null);

        // Act
        ComicDownloadResult result = strategy.downloadComic(request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccessful());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("empty"));
        assertNotNull(result.getRequest());
        assertEquals(request, result.getRequest());
    }

    @Test
    void shouldFailWhenImageDataIsEmpty() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Test Comic")
                .source("test-source")
                .sourceIdentifier("test-comic")
                .date(LocalDate.now())
                .build();

        strategy.setMockImageData(emptyImageData);

        // Act
        ComicDownloadResult result = strategy.downloadComic(request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccessful());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("empty"));
        assertNotNull(result.getRequest());
        assertEquals(request, result.getRequest());
    }

    @Test
    void shouldFailWhenImageValidationFails() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Test Comic")
                .source("test-source")
                .sourceIdentifier("test-comic")
                .date(LocalDate.now())
                .build();

        ImageValidationResult validationResult = ImageValidationResult.failure(
                "Image is corrupted");

        strategy.setMockImageData(validImageData);
        when(imageValidationService.validate(validImageData)).thenReturn(validationResult);

        // Act
        ComicDownloadResult result = strategy.downloadComic(request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorMessage().contains("Invalid image"));
        assertTrue(result.getErrorMessage().contains("corrupted"));
    }

    @Test
    void shouldFailWhenDownloadThrowsException() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Test Comic")
                .source("test-source")
                .sourceIdentifier("test-comic")
                .date(LocalDate.now())
                .build();

        strategy.setThrowException(true);

        // Act
        ComicDownloadResult result = strategy.downloadComic(request);

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccessful());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Error downloading comic"));
        assertNotNull(result.getRequest());
        assertEquals(request, result.getRequest());
    }

    @Test
    void shouldDownloadAvatarSuccessfully() {
        // Arrange
        int comicId = 1;
        String comicName = "Test Comic";
        String sourceIdentifier = "test-comic";

        ImageValidationResult validationResult = ImageValidationResult.success(
                ImageFormat.PNG, 100, 100, validImageData.length);

        strategy.setMockAvatarData(validImageData);
        when(imageValidationService.validate(validImageData)).thenReturn(validationResult);

        // Act
        Optional<byte[]> result = strategy.downloadAvatar(comicId, comicName, sourceIdentifier);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(validImageData, result.get());
    }

    @Test
    void shouldReturnEmptyWhenAvatarDataIsNull() {
        // Arrange
        int comicId = 1;
        String comicName = "Test Comic";
        String sourceIdentifier = "test-comic";

        strategy.setMockAvatarData(null);

        // Act
        Optional<byte[]> result = strategy.downloadAvatar(comicId, comicName, sourceIdentifier);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenAvatarDataIsEmpty() {
        // Arrange
        int comicId = 1;
        String comicName = "Test Comic";
        String sourceIdentifier = "test-comic";

        strategy.setMockAvatarData(emptyImageData);

        // Act
        Optional<byte[]> result = strategy.downloadAvatar(comicId, comicName, sourceIdentifier);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenAvatarValidationFails() {
        // Arrange
        int comicId = 1;
        String comicName = "Test Comic";
        String sourceIdentifier = "test-comic";

        ImageValidationResult validationResult = ImageValidationResult.failure(
                "Invalid avatar format");

        strategy.setMockAvatarData(validImageData);
        when(imageValidationService.validate(validImageData)).thenReturn(validationResult);

        // Act
        Optional<byte[]> result = strategy.downloadAvatar(comicId, comicName, sourceIdentifier);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenAvatarDownloadThrowsException() {
        // Arrange
        int comicId = 1;
        String comicName = "Test Comic";
        String sourceIdentifier = "test-comic";

        strategy.setThrowException(true);

        // Act
        Optional<byte[]> result = strategy.downloadAvatar(comicId, comicName, sourceIdentifier);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnCorrectSource() {
        // Assert
        assertEquals("test-source", strategy.getSource());
    }

    /**
     * Test implementation of AbstractComicDownloaderStrategy for testing purposes
     */
    private static class TestComicDownloaderStrategy extends AbstractComicDownloaderStrategy {
        private byte[] mockImageData;
        private byte[] mockAvatarData;
        private boolean throwException = false;

        public TestComicDownloaderStrategy(String source,
                                          InspectorService webInspector,
                                          ValidationService imageValidationService) {
            super(source, webInspector, imageValidationService);
        }

        public void setMockImageData(byte[] data) {
            this.mockImageData = data;
        }

        public void setMockAvatarData(byte[] data) {
            this.mockAvatarData = data;
        }

        public void setThrowException(boolean throwException) {
            this.throwException = throwException;
        }

        @Override
        protected byte[] downloadComicImage(ComicDownloadRequest request) throws Exception {
            if (throwException) {
                throw new Exception("Test exception");
            }
            return mockImageData;
        }

        @Override
        protected byte[] downloadAvatarImage(int comicId, String comicName, String sourceIdentifier) throws Exception {
            if (throwException) {
                throw new Exception("Test exception");
            }
            return mockAvatarData;
        }
    }
}
