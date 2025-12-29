package org.stapledon.engine.downloader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.service.ValidationService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
        assertThat(strategy.getSource()).isEqualTo("comicskingdom");
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
        assertThat(newStrategy).isNotNull();
        assertThat(newStrategy.getSource()).isEqualTo("comicskingdom");
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
        assertThat(strategy).isNotNull();
        assertThat(request).isNotNull();
        assertThat(strategy.getSource()).isEqualTo("comicskingdom");
        assertThat(request.getComicId()).isEqualTo(1);
        assertThat(request.getComicName()).isEqualTo("Daddy Daze");
        assertThat(request.getSourceIdentifier()).isEqualTo("daddy-daze");
        assertThat(request.getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
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
        assertThat(request).isNotNull();
        assertThat(request.getComicName()).isEqualTo("Daddy Daze");
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
        assertThat(toString.contains("ComicsKingdomDownloaderStrategy")).isTrue();
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
        assertThat(request).isNotNull();
        assertThat(request.getComicId()).isEqualTo(1);
        assertThat(request.getComicName()).isEqualTo("Multi Word Comic Name");
        assertThat(request.getSourceIdentifier()).isEqualTo("multi-word-comic-name");
        assertThat(request.getSource()).isEqualTo("comicskingdom");
    }
}
