package org.stapledon.engine.storage;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicDateIndex;
import org.stapledon.common.util.GsonUtils;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ComicIndexServiceTest {

    @TempDir
    Path tempDir;

    private ComicIndexService indexService;
    private final Gson gson = GsonUtils.createGson();

    @Mock
    private CacheProperties cacheProperties;

    @Mock
    private ImageMetadataRepository metadataRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(cacheProperties.getLocation()).thenReturn(tempDir.toString());
        indexService = new ComicIndexService(gson, cacheProperties, metadataRepository);
    }

    @Test
    void getNextDate_shouldReturnNextDate() {
        // Arrange
        int comicId = 1;
        String comicName = "TestComic";
        LocalDate d1 = LocalDate.of(2023, 1, 1);
        LocalDate d2 = LocalDate.of(2023, 1, 5);
        LocalDate d3 = LocalDate.of(2023, 1, 10);

        setupIndex(comicId, comicName, Arrays.asList(d1, d2, d3));

        // Act
        Optional<LocalDate> next = indexService.getNextDate(comicId, comicName, d1);

        // Assert
        assertThat(next).hasValue(d2);
    }

    @Test
    void getNextDate_shouldHandleMissingDate() {
        // Arrange
        int comicId = 1;
        String comicName = "TestComic";
        LocalDate d1 = LocalDate.of(2023, 1, 1);
        LocalDate d3 = LocalDate.of(2023, 1, 10);

        setupIndex(comicId, comicName, Arrays.asList(d1, d3));

        // Act
        Optional<LocalDate> next = indexService.getNextDate(comicId, comicName, LocalDate.of(2023, 1, 5));

        // Assert
        assertThat(next).hasValue(d3);
    }

    @Test
    void getPreviousDate_shouldReturnPreviousDate() {
        // Arrange
        int comicId = 1;
        String comicName = "TestComic";
        LocalDate d1 = LocalDate.of(2023, 1, 1);
        LocalDate d2 = LocalDate.of(2023, 1, 5);
        LocalDate d3 = LocalDate.of(2023, 1, 10);

        setupIndex(comicId, comicName, Arrays.asList(d1, d2, d3));

        // Act
        Optional<LocalDate> prev = indexService.getPreviousDate(comicId, comicName, d3);

        // Assert
        assertThat(prev).hasValue(d2);
    }

    @Test
    void addDateToIndex_shouldUpdateAndSave() {
        // Arrange
        int comicId = 1;
        String comicName = "TestComic";
        LocalDate d1 = LocalDate.of(2023, 1, 1);
        LocalDate d2 = LocalDate.of(2023, 1, 10);

        setupIndex(comicId, comicName, Arrays.asList(d1));

        // Act
        indexService.addDateToIndex(comicId, comicName, d2);

        // Assert
        Optional<LocalDate> next = indexService.getNextDate(comicId, comicName, d1);
        assertThat(next).hasValue(d2);

        // Check file exists
        File indexFile = new File(tempDir.toFile(), "TestComic/available-dates.json");
        assertThat(indexFile).exists();
    }

    private void setupIndex(int comicId, String comicName, List<LocalDate> dates) {
        ComicDateIndex index = ComicDateIndex.builder()
                .comicId(comicId)
                .comicName(comicName)
                .availableDates(dates)
                .lastUpdated(LocalDate.now())
                .build();

        File comicDir = new File(tempDir.toFile(), comicName);
        comicDir.mkdirs();
        File indexFile = new File(comicDir, "available-dates.json");

        try (FileWriter writer = new FileWriter(indexFile)) {
            gson.toJson(index, writer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
