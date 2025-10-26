package org.stapledon.engine.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

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
        assertFalse(result.isPresent());
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
        assertTrue(result.isPresent());
        assertEquals(testHash, result.get().getHash());
        assertEquals(LocalDate.of(2024, 6, 15), result.get().getDate());
        assertEquals("/comics/test/2024/2024-06-15.png", result.get().getFilePath());
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
        assertTrue(result.isPresent());
        assertNotNull(result.get());
        assertEquals(testHash, result.get().getHash());
        assertNotNull(result.get().getDate());
        assertEquals(LocalDate.of(2024, 6, 15), result.get().getDate());
        assertNotNull(result.get().getFilePath());
        assertEquals("/comics/test/2024/2024-06-15.png", result.get().getFilePath());
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
        assertNotNull(hashes);
        assertEquals(1, hashes.size());
        assertTrue(hashes.containsKey(testHash));
        ImageHashRecord retrievedRecord = hashes.get(testHash);
        assertNotNull(retrievedRecord);
        assertEquals(testHash, retrievedRecord.getHash());
        assertEquals(LocalDate.of(2024, 6, 15), retrievedRecord.getDate());
        assertEquals("/comics/test/2024/2024-06-15.png", retrievedRecord.getFilePath());
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

        assertFalse(oldResult.isPresent());
        assertTrue(newResult.isPresent());
        assertNotNull(newResult.get());
        assertEquals(testHash, newResult.get().getHash());
        assertEquals(LocalDate.of(2024, 6, 20), newResult.get().getDate());
        assertEquals("/comics/test/2024/2024-06-20.png", newResult.get().getFilePath());
    }

    @Test
    void shouldGetYearDirectory() {
        // Act
        File yearDir = repository.getYearDirectory(comicId, comicName, year);

        // Assert
        assertNotNull(yearDir);
        assertTrue(yearDir.getPath().contains("TestComic"));
        assertTrue(yearDir.getPath().contains("2024"));
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
        assertTrue(result.isPresent());
        assertNotNull(result.get());
        assertEquals(testHash, result.get().getHash());
        assertEquals(LocalDate.of(2024, 6, 15), result.get().getDate());
        assertEquals("/comics/comic_1/2024/2024-06-15.png", result.get().getFilePath());
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
        assertTrue(result.isPresent());
        assertNotNull(result.get());
        assertEquals(testHash, result.get().getHash());
        assertEquals(LocalDate.of(2024, 6, 15), result.get().getDate());
        assertEquals("/comics/test/2024/2024-06-15.png", result.get().getFilePath());
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
        assertNotNull(yearDir);
        // Spaces should be removed
        assertFalse(yearDir.getPath().contains(" "));
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

        assertTrue(result2024.isPresent());
        assertTrue(result2023.isPresent());
        assertEquals(2024, result2024.get().getDate().getYear());
        assertEquals(2023, result2023.get().getDate().getYear());
        assertEquals("/comics/test/2024/2024-06-15.png", result2024.get().getFilePath());
        assertEquals("/comics/test/2023/2023-06-15.png", result2023.get().getFilePath());
    }

    @Test
    void shouldHaveValidToString() {
        // Act
        String toString = repository.toString();

        // Assert
        assertNotNull(toString);
    }
}
