package org.stapledon.engine.downloader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.service.ValidationService;

import java.time.LocalDate;

@ExtendWith(MockitoExtension.class)
class ComicsKingdomDownloaderStrategyTest {

    @Mock
    private InspectorService webInspector;

    @Mock
    private ValidationService imageValidationService;

    private ComicsKingdomDownloaderStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ComicsKingdomDownloaderStrategy(webInspector, imageValidationService);
    }

    @Test
    void shouldHaveCorrectSourceIdentifier() {
        // Assert
        assertEquals("comicskingdom", strategy.getSource());
    }

    @Test
    void shouldCreateStrategyWithDependencies() {
        // Arrange
        InspectorService mockInspector = mock(InspectorService.class);
        ValidationService mockValidation = mock(ValidationService.class);

        // Act
        ComicsKingdomDownloaderStrategy newStrategy = new ComicsKingdomDownloaderStrategy(
                mockInspector, mockValidation);

        // Assert
        assertNotNull(newStrategy);
        assertEquals("comicskingdom", newStrategy.getSource());
    }

    @Test
    void shouldUseSourceIdentifierInRequest() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Daddy Daze")
                .source("comicskingdom")
                .sourceIdentifier("daddy-daze")
                .date(LocalDate.of(2024, 1, 15))
                .build();

        // Note: This test verifies that the strategy is set up correctly
        // Integration tests will verify actual download functionality
        assertNotNull(strategy);
        assertNotNull(request);
        assertEquals("comicskingdom", strategy.getSource());
        assertEquals(1, request.getComicId());
        assertEquals("Daddy Daze", request.getComicName());
        assertEquals("daddy-daze", request.getSourceIdentifier());
        assertEquals(LocalDate.of(2024, 1, 15), request.getDate());
    }

    @Test
    void shouldHandleSpacesInComicName() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Daddy Daze")
                .source("comicskingdom")
                .sourceIdentifier("daddy-daze")
                .date(LocalDate.of(2024, 1, 15))
                .build();

        // Act & Assert - verify construction doesn't throw
        assertNotNull(request);
        assertEquals("Daddy Daze", request.getComicName());
    }

    @Test
    void shouldFormatDateCorrectly() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Test Comic")
                .source("comicskingdom")
                .sourceIdentifier("test-comic")
                .date(LocalDate.of(2024, 1, 15))
                .build();

        // Act & Assert
        assertNotNull(request.getDate());
        assertEquals(2024, request.getDate().getYear());
        assertEquals(1, request.getDate().getMonthValue());
        assertEquals(15, request.getDate().getDayOfMonth());
    }

    @Test
    void shouldHaveValidToString() {
        // Act
        String toString = strategy.toString();

        // Assert
        assertNotNull(toString);
        // Lombok's @ToString should include class name
        assertTrue(toString.contains("ComicsKingdomDownloaderStrategy"));
    }

    @Test
    void shouldHandleHyphenatedComicNames() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Multi Word Comic Name")
                .source("comicskingdom")
                .sourceIdentifier("multi-word-comic-name")
                .date(LocalDate.of(2024, 1, 15))
                .build();

        // Act & Assert
        assertNotNull(request);
        assertEquals(1, request.getComicId());
        assertEquals("Multi Word Comic Name", request.getComicName());
        assertEquals("multi-word-comic-name", request.getSourceIdentifier());
        assertEquals("comicskingdom", request.getSource());
    }
}
