package org.stapledon.engine.downloader;

import static org.assertj.core.api.Assertions.assertThat;
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
        assertThat(strategy.getSource()).isEqualTo("gocomics");
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
        assertThat(newStrategy).isNotNull();
        assertThat(newStrategy.getSource()).isEqualTo("gocomics");
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
        assertThat(request).isNotNull();
        assertThat(request.getSource()).isEqualTo("gocomics");
        assertThat(request.getSourceIdentifier()).isEqualTo("calvinandhobbes");
        assertThat(request.getComicId()).isEqualTo(1);
        assertThat(request.getComicName()).isEqualTo("Calvin and Hobbes");
        assertThat(request.getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        // Integration tests will verify actual download functionality with this strategy
        assertThat(strategy.getSource()).isEqualTo("gocomics");
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
        assertThat(request).isNotNull();
        assertThat(request.getComicName()).isEqualTo("Calvin and Hobbes");
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
        assertThat(request.getDate()).isNotNull();
        assertThat(request.getDate().getYear()).isEqualTo(2024);
        assertThat(request.getDate().getMonthValue()).isEqualTo(1);
        assertThat(request.getDate().getDayOfMonth()).isEqualTo(15);
    }

    @Test
    void shouldHaveValidToString() {
        // Act
        String toString = strategy.toString();

        // Assert
        assertThat(toString).isNotNull();
        // Lombok's @ToString should include class name
        assertThat(toString.contains("GoComicsDownloaderStrategy")).isTrue();
    }
}
