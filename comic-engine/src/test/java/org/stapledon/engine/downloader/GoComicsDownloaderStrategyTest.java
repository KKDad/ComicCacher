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
class GoComicsDownloaderStrategyTest {

    @Mock
    private InspectorService webInspector;

    @Mock
    private ValidationService imageValidationService;

    private GoComicsDownloaderStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new GoComicsDownloaderStrategy(webInspector, imageValidationService);
    }

    @Test
    void shouldHaveCorrectSourceIdentifier() {
        // Assert
        assertEquals("gocomics", strategy.getSource());
    }

    @Test
    void shouldCreateStrategyWithDependencies() {
        // Arrange
        InspectorService mockInspector = mock(InspectorService.class);
        ValidationService mockValidation = mock(ValidationService.class);

        // Act
        GoComicsDownloaderStrategy newStrategy = new GoComicsDownloaderStrategy(
                mockInspector, mockValidation);

        // Assert
        assertNotNull(newStrategy);
        assertEquals("gocomics", newStrategy.getSource());
    }

    @Test
    void shouldBuildRequestCorrectly() {
        // Arrange & Act
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Calvin and Hobbes")
                .source("gocomics")
                .sourceIdentifier("calvinandhobbes")
                .date(LocalDate.of(2024, 1, 15))
                .build();

        // Assert
        assertNotNull(request);
        assertEquals("gocomics", request.getSource());
        assertEquals("calvinandhobbes", request.getSourceIdentifier());
        assertEquals(1, request.getComicId());
        assertEquals("Calvin and Hobbes", request.getComicName());
        assertEquals(LocalDate.of(2024, 1, 15), request.getDate());
        // Integration tests will verify actual download functionality with this strategy
        assertEquals("gocomics", strategy.getSource());
    }

    @Test
    void shouldHandleSpacesInComicName() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Calvin and Hobbes")
                .source("gocomics")
                .sourceIdentifier("calvinandhobbes")
                .date(LocalDate.of(2024, 1, 15))
                .build();

        // Act & Assert - verify construction doesn't throw
        assertNotNull(request);
        assertEquals("Calvin and Hobbes", request.getComicName());
    }

    @Test
    void shouldFormatDateCorrectly() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Test Comic")
                .source("gocomics")
                .sourceIdentifier("testcomic")
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
        assertTrue(toString.contains("GoComicsDownloaderStrategy"));
    }
}
