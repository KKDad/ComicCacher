package org.stapledon.infrastructure.caching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.metrics.repository.AccessMetricsRepository;
import org.stapledon.metrics.collector.AccessMetricsCollector;

import java.io.File;
import java.time.LocalDate;

public class AccessMetricsCollectorTest {

    private AccessMetricsCollector accessMetricsCollector;
    private ComicItem comicItem;

    @BeforeEach
    void setUp() {
        File resourcesDirectory = getResourcesDirectory();
        MockComicStorageFacade mockStorageFacade = new MockComicStorageFacade();
        AccessMetricsRepository mockAccessMetricsRepository = mock(AccessMetricsRepository.class);

        // Create test comic item
        comicItem = ComicItem.builder()
                .id(42)
                .name("Fake Comic")
                .description("Comic for Unit Tests")
                .oldest(LocalDate.of(2008, 1, 10))
                .newest(LocalDate.of(2019, 3, 22))
                .build();

        // Set up mock data
        mockStorageFacade.setupComic(
                comicItem.getId(),
                comicItem.getName(),
                LocalDate.of(2008, 1, 10),    // oldest
                LocalDate.of(2019, 3, 22),    // newest
                LocalDate.of(2010, 6, 28)     // additional date between
        );

        accessMetricsCollector = new AccessMetricsCollector(resourcesDirectory.toString(), mockStorageFacade, mockAccessMetricsRepository);
    }

    @Test
    void findOldestTest() {
        File result = accessMetricsCollector.findOldest(comicItem);

        assertThat(result).isNotNull();
        assertThat(result.getAbsolutePath()).contains("2008-01-10");
    }

    @Test
    void findNewestTest() {
        File result = accessMetricsCollector.findNewest(comicItem);

        assertThat(result).isNotNull();
        assertThat(result.getAbsolutePath()).contains("2019-03-22");
    }

    @Test
    void findPreviousTest() {
        LocalDate dt = LocalDate.of(2010, 6, 29);
        File result = accessMetricsCollector.findPrevious(comicItem, dt);

        assertThat(result).isNotNull();
        assertThat(result.getAbsolutePath()).contains("2010-06-28");
    }

    @Test
    void findNextTest() {
        LocalDate dt = LocalDate.of(2008, 1, 11);
        File result = accessMetricsCollector.findNext(comicItem, dt);

        assertThat(result).isNotNull();
        assertThat(result.getAbsolutePath()).contains("2010-06-28");
    }

    /**
     * Helper method to the test resources directory
     *
     * @return File
     */
    public static File getResourcesDirectory() {
        File resourcesDirectory = new File("src/test/resources");
        if (!resourcesDirectory.exists())
            resourcesDirectory = new File("../src/test/resources");

        assertThat(resourcesDirectory).exists();
        return resourcesDirectory;
    }
}