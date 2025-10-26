package org.stapledon.engine.downloader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.infrastructure.web.InspectorService;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

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
        assertNotNull(result);
        assertEquals(comic, result); // Verify fluent interface
    }

    @Test
    void shouldThrowWhenCacheRootIsNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> comic.setCacheRoot(null));
    }

    @Test
    void shouldSetComic() {
        // Arrange
        String comicName = "Test Comic";

        // Act
        IDailyComic result = comic.setComic(comicName);

        // Assert
        assertNotNull(result);
        assertEquals(comic, result); // Verify fluent interface
        assertEquals(comicName, comic.getComic());
    }

    @Test
    void shouldParseComicNameWithSpaces() {
        // Arrange
        String comicName = "Test Comic Name";

        // Act
        comic.setComic(comicName);

        // Assert
        assertEquals(comicName, comic.getComic());
        // comicNameParsed should have spaces removed
        String cacheLocation = comic.cacheLocation();
        assertNotNull(cacheLocation);
        assertTrue(cacheLocation.contains("TestComicName"));
    }

    @Test
    void shouldSetDate() {
        // Arrange
        LocalDate date = LocalDate.of(2024, 1, 15);

        // Act
        IDailyComic result = comic.setDate(date);

        // Assert
        assertNotNull(result);
        assertEquals(comic, result); // Verify fluent interface
        assertEquals(date, comic.getDate());
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
        assertEquals(expectedNextDate, nextDate);
        assertEquals(expectedNextDate, comic.getDate());
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
        assertEquals(lastDate, resultDate);
        assertEquals(lastDate, comic.getDate());
    }

    @Test
    void shouldGenerateCacheLocation() {
        // Arrange
        comic.setCacheRoot(tempDir.toString());
        comic.setComic("Test Comic");

        // Act
        String location = comic.cacheLocation();

        // Assert
        assertNotNull(location);
        // Should contain temp dir and parsed comic name
        assertTrue(location.contains(tempDir.toString()));
        assertTrue(location.contains("TestComic"));
    }

    @Test
    void shouldUseProvidedInspector() {
        // Arrange
        InspectorService customInspector = mock(InspectorService.class);

        // Act
        TestDailyComic customComic = new TestDailyComic(customInspector, "selector");

        // Assert
        assertNotNull(customComic);
        assertEquals("selector", customComic.getElementSelector());
    }

    @Test
    void shouldCreateInspectorWhenNull() {
        // Act
        TestDailyComic comicWithNullInspector = new TestDailyComic(null, "selector");

        // Assert
        assertNotNull(comicWithNullInspector);
        // Should create default JsoupInspectorService
        assertNotNull(comicWithNullInspector.getWebInspector());
    }

    @Test
    void shouldHaveValidToString() {
        // Arrange
        comic.setComic("Test Comic");
        comic.setDate(LocalDate.of(2024, 1, 15));

        // Act
        String toString = comic.toString();

        // Assert
        assertNotNull(toString);
        assertTrue(toString.contains("Test Comic"));
        assertTrue(toString.contains("2024-01-15"));
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
