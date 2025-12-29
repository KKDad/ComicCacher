package org.stapledon.engine.storage;

import com.google.gson.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ImageHashRecord;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DuplicateImageHashRepositoryTest {

    @TempDir
    Path tempDir;

    @Mock
    private CacheProperties cacheProperties;

    private DuplicateImageHashRepository repository;

    private final int comicId = 1;
    private final String comicName = "Test Comic";
    private final int year = 2024;
    private final String testHash = "abc123";

    @BeforeEach
    void setUp() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) -> LocalDate.parse(json.getAsString()))
                .create();

        lenient().when(cacheProperties.getLocation()).thenReturn(tempDir.toString());
        repository = new DuplicateImageHashRepository(gson, cacheProperties);
    }

    @Test
    void shouldFindHashInEmptyRepository() {
        // Act
        Optional<ImageHashRecord> result = repository.findByHash(comicId, comicName, year, testHash);

        // Assert
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    void shouldAddAndFindHash() {
        // Arrange
        ImageHashRecord record = ImageHashRecord.builder()
                .date(LocalDate.of(2024, 6, 15))
                .hash(testHash)
                .filePath("/comics/test/2024/2024-06-15.png")
                .build();

        // Act
        repository.addHash(comicId, comicName, year, record);
        Optional<ImageHashRecord> result = repository.findByHash(comicId, comicName, year, testHash);

        // Assert
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getHash()).isEqualTo(testHash);
        assertThat(result.get().getDate()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(result.get().getFilePath()).isEqualTo("/comics/test/2024/2024-06-15.png");
    }

    @Test
    void shouldPersistHashToDisk() {
        // Arrange
        ImageHashRecord record = ImageHashRecord.builder()
                .date(LocalDate.of(2024, 6, 15))
                .hash(testHash)
                .filePath("/comics/test/2024/2024-06-15.png")
                .build();

        // Act
        repository.addHash(comicId, comicName, year, record);

        // Clear cache to force reload from disk
        repository.clearCache();

        Optional<ImageHashRecord> result = repository.findByHash(comicId, comicName, year, testHash);

        // Assert
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isNotNull();
        assertThat(result.get().getHash()).isEqualTo(testHash);
        assertThat(result.get().getDate()).isNotNull();
        assertThat(result.get().getDate()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(result.get().getFilePath()).isNotNull();
        assertThat(result.get().getFilePath()).isEqualTo("/comics/test/2024/2024-06-15.png");
    }

    @Test
    void shouldLoadHashesFromCache() {
        // Arrange
        ImageHashRecord record = ImageHashRecord.builder()
                .date(LocalDate.of(2024, 6, 15))
                .hash(testHash)
                .filePath("/comics/test/2024/2024-06-15.png")
                .build();

        repository.addHash(comicId, comicName, year, record);

        // Act - load again (should use cache)
        Map<String, ImageHashRecord> hashes = repository.loadHashes(comicId, comicName, year);

        // Assert
        assertThat(hashes).isNotNull();
        assertThat(hashes.size()).isEqualTo(1);
        assertThat(hashes.containsKey(testHash)).isTrue();
        ImageHashRecord retrievedRecord = hashes.get(testHash);
        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getHash()).isEqualTo(testHash);
        assertThat(retrievedRecord.getDate()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(retrievedRecord.getFilePath()).isEqualTo("/comics/test/2024/2024-06-15.png");
    }

    @Test
    void shouldReplaceAllHashes() {
        // Arrange
        ImageHashRecord oldRecord = ImageHashRecord.builder()
                .date(LocalDate.of(2024, 6, 15))
                .hash("oldHash")
                .filePath("/comics/test/2024/2024-06-15.png")
                .build();

        repository.addHash(comicId, comicName, year, oldRecord);

        Map<String, ImageHashRecord> newHashes = new ConcurrentHashMap<>();
        ImageHashRecord newRecord = ImageHashRecord.builder()
                .date(LocalDate.of(2024, 6, 20))
                .hash(testHash)
                .filePath("/comics/test/2024/2024-06-20.png")
                .build();
        newHashes.put(testHash, newRecord);

        // Act
        repository.replaceHashes(comicId, comicName, year, newHashes);

        // Assert
        Optional<ImageHashRecord> oldResult = repository.findByHash(comicId, comicName, year, "oldHash");
        Optional<ImageHashRecord> newResult = repository.findByHash(comicId, comicName, year, testHash);

        assertThat(oldResult.isPresent()).isFalse();
        assertThat(newResult.isPresent()).isTrue();
        assertThat(newResult.get()).isNotNull();
        assertThat(newResult.get().getHash()).isEqualTo(testHash);
        assertThat(newResult.get().getDate()).isEqualTo(LocalDate.of(2024, 6, 20));
        assertThat(newResult.get().getFilePath()).isEqualTo("/comics/test/2024/2024-06-20.png");
    }

    @Test
    void shouldGetYearDirectory() {
        // Act
        File yearDir = repository.getYearDirectory(comicId, comicName, year);

        // Assert
        assertThat(yearDir).isNotNull();
        assertThat(yearDir.getPath().contains("TestComic")).isTrue();
        assertThat(yearDir.getPath().contains("2024")).isTrue();
    }

    @Test
    void shouldHandleNullComicName() {
        // Arrange
        ImageHashRecord record = ImageHashRecord.builder()
                .date(LocalDate.of(2024, 6, 15))
                .hash(testHash)
                .filePath("/comics/comic_1/2024/2024-06-15.png")
                .build();

        // Act
        repository.addHash(comicId, null, year, record);
        Optional<ImageHashRecord> result = repository.findByHash(comicId, null, year, testHash);

        // Assert
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isNotNull();
        assertThat(result.get().getHash()).isEqualTo(testHash);
        assertThat(result.get().getDate()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(result.get().getFilePath()).isEqualTo("/comics/comic_1/2024/2024-06-15.png");
    }

    @Test
    void shouldClearCache() {
        // Arrange
        ImageHashRecord record = ImageHashRecord.builder()
                .date(LocalDate.of(2024, 6, 15))
                .hash(testHash)
                .filePath("/comics/test/2024/2024-06-15.png")
                .build();

        repository.addHash(comicId, comicName, year, record);

        // Act
        repository.clearCache();

        // Should still find because it's persisted to disk
        Optional<ImageHashRecord> result = repository.findByHash(comicId, comicName, year, testHash);

        // Assert
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isNotNull();
        assertThat(result.get().getHash()).isEqualTo(testHash);
        assertThat(result.get().getDate()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(result.get().getFilePath()).isEqualTo("/comics/test/2024/2024-06-15.png");
    }

    @Test
    void shouldHandleSpacesInComicName() {
        // Arrange
        String comicNameWithSpaces = "Test Comic Name";
        ImageHashRecord record = ImageHashRecord.builder()
                .date(LocalDate.of(2024, 6, 15))
                .hash(testHash)
                .filePath("/comics/test/2024/2024-06-15.png")
                .build();

        // Act
        repository.addHash(comicId, comicNameWithSpaces, year, record);
        File yearDir = repository.getYearDirectory(comicId, comicNameWithSpaces, year);

        // Assert
        assertThat(yearDir).isNotNull();
        // Spaces should be removed
        assertThat(yearDir.getPath().contains(" ")).isFalse();
    }

    @Test
    void shouldSeparateHashesByYear() {
        // Arrange
        ImageHashRecord record2024 = ImageHashRecord.builder()
                .date(LocalDate.of(2024, 6, 15))
                .hash(testHash)
                .filePath("/comics/test/2024/2024-06-15.png")
                .build();

        ImageHashRecord record2023 = ImageHashRecord.builder()
                .date(LocalDate.of(2023, 6, 15))
                .hash(testHash)
                .filePath("/comics/test/2023/2023-06-15.png")
                .build();

        // Act
        repository.addHash(comicId, comicName, 2024, record2024);
        repository.addHash(comicId, comicName, 2023, record2023);

        // Assert - same hash in different years should both exist
        Optional<ImageHashRecord> result2024 = repository.findByHash(comicId, comicName, 2024, testHash);
        Optional<ImageHashRecord> result2023 = repository.findByHash(comicId, comicName, 2023, testHash);

        assertThat(result2024.isPresent()).isTrue();
        assertThat(result2023.isPresent()).isTrue();
        assertThat(result2024.get().getDate().getYear()).isEqualTo(2024);
        assertThat(result2023.get().getDate().getYear()).isEqualTo(2023);
        assertThat(result2024.get().getFilePath()).isEqualTo("/comics/test/2024/2024-06-15.png");
        assertThat(result2023.get().getFilePath()).isEqualTo("/comics/test/2023/2023-06-15.png");
    }

    @Test
    void shouldHaveValidToString() {
        // Act
        String toString = repository.toString();

        // Assert
        assertThat(toString).isNotNull();
    }
}
