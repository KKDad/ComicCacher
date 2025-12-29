package org.stapledon.engine.downloader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.infrastructure.web.InspectorService;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

class DailyComicTest {

    @TempDir
    Path tempDir;

    private InspectorService mockInspector;
    private TestDailyComic comic;

    @BeforeEach
    void setUp() {
        mockInspector = mock(InspectorService.class);
        comic = new TestDailyComic(mockInspector, "test-selector");
    }

    @Test
    void shouldSetCacheRoot() {
        // Act
        IDailyComic result = comic.setCacheRoot(tempDir.toString());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(comic); // Verify fluent interface
    }

    @Test
    void shouldThrowWhenCacheRootIsNull() {
        // Act & Assert
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> comic.setCacheRoot(null));
    }

    @Test
    void shouldSetComic() {
        // Arrange
        String comicName = "Test Comic";

        // Act
        IDailyComic result = comic.setComic(comicName);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(comic); // Verify fluent interface
        assertThat(comic.getComic()).isEqualTo(comicName);
    }

    @Test
    void shouldParseComicNameWithSpaces() {
        // Arrange
        String comicName = "Test Comic Name";

        // Act
        comic.setComic(comicName);

        // Assert
        assertThat(comic.getComic()).isEqualTo(comicName);
        // comicNameParsed should have spaces removed
        String cacheLocation = comic.cacheLocation();
        assertThat(cacheLocation).isNotNull();
        assertThat(cacheLocation.contains("TestComicName")).isTrue();
    }

    @Test
    void shouldSetDate() {
        // Arrange
        LocalDate date = LocalDate.of(2024, 1, 15);

        // Act
        IDailyComic result = comic.setDate(date);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(comic); // Verify fluent interface
        assertThat(comic.getDate()).isEqualTo(date);
    }

    @Test
    void shouldAdvanceDate() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 15);
        LocalDate expectedNextDate = LocalDate.of(2024, 1, 16);

        comic.setDate(startDate);
        comic.setLastStripOn(LocalDate.of(2024, 12, 31));

        // Act
        LocalDate nextDate = comic.advance();

        // Assert
        assertThat(nextDate).isEqualTo(expectedNextDate);
        assertThat(comic.getDate()).isEqualTo(expectedNextDate);
    }

    @Test
    void shouldNotAdvancePastLastStripDate() {
        // Arrange
        LocalDate lastDate = LocalDate.of(2024, 1, 31);
        comic.setDate(lastDate);
        comic.setLastStripOn(lastDate);

        // Act
        LocalDate resultDate = comic.advance();

        // Assert
        assertThat(resultDate).isEqualTo(lastDate);
        assertThat(comic.getDate()).isEqualTo(lastDate);
    }

    @Test
    void shouldGenerateCacheLocation() {
        // Arrange
        comic.setCacheRoot(tempDir.toString());
        comic.setComic("Test Comic");

        // Act
        String location = comic.cacheLocation();

        // Assert
        assertThat(location).isNotNull();
        // Should contain temp dir and parsed comic name
        assertThat(location.contains(tempDir.toString())).isTrue();
        assertThat(location.contains("TestComic")).isTrue();
    }

    @Test
    void shouldUseProvidedInspector() {
        // Arrange
        InspectorService customInspector = mock(InspectorService.class);

        // Act
        TestDailyComic customComic = new TestDailyComic(customInspector, "selector");

        // Assert
        assertThat(customComic).isNotNull();
        assertThat(customComic.getElementSelector()).isEqualTo("selector");
    }

    @Test
    void shouldCreateInspectorWhenNull() {
        // Act
        TestDailyComic comicWithNullInspector = new TestDailyComic(null, "selector");

        // Assert
        assertThat(comicWithNullInspector).isNotNull();
        // Should create default JsoupInspectorService
        assertThat(comicWithNullInspector.getWebInspector()).isNotNull();
    }

    @Test
    void shouldHaveValidToString() {
        // Arrange
        comic.setComic("Test Comic");
        comic.setDate(LocalDate.of(2024, 1, 15));

        // Act
        String toString = comic.toString();

        // Assert
        assertThat(toString).isNotNull();
        assertThat(toString.contains("Test Comic")).isTrue();
        assertThat(toString.contains("2024-01-15")).isTrue();
    }

    /**
     * Test implementation of DailyComic
     */
    private static class TestDailyComic extends DailyComic {
        private LocalDate lastStripOn = LocalDate.now().plusYears(1);

        public TestDailyComic(InspectorService inspector, String elementSelector) {
            super(inspector, elementSelector);
        }

        @Override
        protected String generateSiteURL() {
            return "https://test.com";
        }

        @Override
        protected Optional<String> extractComicImage(String comicUrl) {
            return Optional.of("https://test.com/image.png");
        }

        @Override
        public LocalDate getLastStripOn() {
            return lastStripOn;
        }

        @Override
        public void updateComicMetadata(ComicItem comicItem) {
            // No-op for test
        }

        @Override
        public void close() throws Exception {
            // No-op for test
        }

        public void setLastStripOn(LocalDate date) {
            this.lastStripOn = date;
        }

        public String getElementSelector() {
            return this.elementSelector;
        }

        public InspectorService getWebInspector() {
            return this.webInspector;
        }
    }
}
