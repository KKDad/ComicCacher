package org.stapledon.engine.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicErrorRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

class JsonErrorTrackingRepositoryTest {

    @TempDir
    Path tempDir;

    private JsonErrorTrackingRepository repository;
    private CacheProperties cacheProperties;
    private Gson gson;

    @BeforeEach
    void setUp() {
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .setPrettyPrinting()
                .create();

        cacheProperties = new CacheProperties();
        cacheProperties.setLocation(tempDir.toString());

        repository = new JsonErrorTrackingRepository(gson, cacheProperties, 5);
        repository.resetErrors(); // Ensure clean state
    }

    @Test
    void shouldRecordError() {
        // Given
        ComicErrorRecord error = createTestError("TestComic", "Test error message");

        // When
        repository.recordError(error);

        // Then
        List<ComicErrorRecord> errors = repository.getErrors("TestComic");
        assertEquals(1, errors.size());
        assertEquals("TestComic", errors.get(0).getComicName());
        assertEquals("Test error message", errors.get(0).getErrorMessage());
    }

    @Test
    void shouldKeepOnlyLastNErrors() {
        // Given
        String comicName = "TestComic";

        // When - Record 7 errors (max is 5)
        for (int i = 1; i <= 7; i++) {
            ComicErrorRecord error = createTestError(comicName, "Error " + i);
            repository.recordError(error);
        }

        // Then - Should only keep last 5
        List<ComicErrorRecord> errors = repository.getErrors(comicName);
        assertEquals(5, errors.size());
        // Most recent should be first
        assertEquals("Error 7", errors.get(0).getErrorMessage());
        assertEquals("Error 3", errors.get(4).getErrorMessage());
    }

    @Test
    void shouldClearErrorsForComic() {
        // Given
        ComicErrorRecord error1 = createTestError("Comic1", "Error 1");
        ComicErrorRecord error2 = createTestError("Comic2", "Error 2");
        repository.recordError(error1);
        repository.recordError(error2);

        // When
        repository.clearErrors("Comic1");

        // Then
        assertTrue(repository.getErrors("Comic1").isEmpty());
        assertFalse(repository.getErrors("Comic2").isEmpty());
    }

    @Test
    void shouldReturnEmptyListForComicWithNoErrors() {
        // When
        List<ComicErrorRecord> errors = repository.getErrors("NonExistentComic");

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldGetAllErrors() {
        // Given
        repository.recordError(createTestError("Comic1", "Error 1"));
        repository.recordError(createTestError("Comic1", "Error 2"));
        repository.recordError(createTestError("Comic2", "Error 3"));

        // When
        Map<String, List<ComicErrorRecord>> allErrors = repository.getAllErrors();

        // Then
        assertEquals(2, allErrors.size());
        assertTrue(allErrors.containsKey("Comic1"));
        assertTrue(allErrors.containsKey("Comic2"));
        assertEquals(2, allErrors.get("Comic1").size());
        assertEquals(1, allErrors.get("Comic2").size());
    }

    @Test
    void shouldGetComicErrorCount() {
        // Given
        repository.recordError(createTestError("Comic1", "Error 1"));
        repository.recordError(createTestError("Comic2", "Error 2"));
        repository.recordError(createTestError("Comic3", "Error 3"));

        // When
        int count = repository.getComicErrorCount();

        // Then
        assertEquals(3, count);
    }

    @Test
    void shouldPersistErrorsToFile() {
        // Given
        ComicErrorRecord error = createTestError("TestComic", "Test error");
        repository.recordError(error);

        // Create new repository instance to test persistence
        JsonErrorTrackingRepository newRepository = new JsonErrorTrackingRepository(
                gson, cacheProperties, 5);

        // When
        List<ComicErrorRecord> errors = newRepository.getErrors("TestComic");

        // Then
        assertEquals(1, errors.size());
        assertEquals("Test error", errors.get(0).getErrorMessage());
    }

    @Test
    void shouldHandleMultipleErrorsForSameComic() {
        // Given
        String comicName = "TestComic";

        // When
        repository.recordError(createTestError(comicName, "Network error"));
        repository.recordError(createTestError(comicName, "Parsing error"));
        repository.recordError(createTestError(comicName, "Storage error"));

        // Then
        List<ComicErrorRecord> errors = repository.getErrors(comicName);
        assertEquals(3, errors.size());
        // Most recent first
        assertEquals("Storage error", errors.get(0).getErrorMessage());
        assertEquals("Network error", errors.get(2).getErrorMessage());
    }

    @Test
    void shouldSortErrorsByTimestampDescending() {
        // Given
        String comicName = "TestComic";
        repository.recordError(createTestError(comicName, "First error"));

        // Add small delay to ensure different timestamps
        try { Thread.sleep(10); } catch (InterruptedException e) { }

        repository.recordError(createTestError(comicName, "Second error"));

        try { Thread.sleep(10); } catch (InterruptedException e) { }

        repository.recordError(createTestError(comicName, "Third error"));

        // When
        Map<String, List<ComicErrorRecord>> allErrors = repository.getAllErrors();
        List<ComicErrorRecord> errors = allErrors.get(comicName);

        // Then
        assertEquals(3, errors.size());
        assertTrue(errors.get(0).getTimestamp().isAfter(errors.get(1).getTimestamp()) ||
                   errors.get(0).getTimestamp().isEqual(errors.get(1).getTimestamp()));
        assertTrue(errors.get(1).getTimestamp().isAfter(errors.get(2).getTimestamp()) ||
                   errors.get(1).getTimestamp().isEqual(errors.get(2).getTimestamp()));
    }

    @Test
    void shouldHandleEmptyFileGracefully() {
        // Given - Repository has been reset, so it has an empty error map
        // (file will exist but be empty JSON object)

        // When
        List<ComicErrorRecord> errors = repository.getErrors("AnyComic");

        // Then
        assertTrue(errors.isEmpty());
        assertEquals(0, repository.getComicErrorCount());
    }

    private ComicErrorRecord createTestError(String comicName, String errorMessage) {
        return ComicErrorRecord.builder()
                .comicName(comicName)
                .comicDate(LocalDate.of(2023, 1, 15))
                .source("gocomics")
                .status(ComicRetrievalStatus.NETWORK_ERROR)
                .errorMessage(errorMessage)
                .httpStatusCode(404)
                .timestamp(LocalDateTime.now())
                .retrievalDurationMs(100L)
                .build();
    }

    // Gson adapters
    static class LocalDateAdapter extends TypeAdapter<LocalDate> {
        @Override
        public void write(final JsonWriter jsonWriter, final LocalDate localDate) throws IOException {
            if (localDate == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(localDate.toString());
            }
        }

        @Override
        public LocalDate read(final JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            } else {
                return LocalDate.parse(jsonReader.nextString());
            }
        }
    }

    static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public void write(JsonWriter jsonWriter, LocalDateTime localDateTime) throws IOException {
            if (localDateTime == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(formatter.format(localDateTime));
            }
        }

        @Override
        public LocalDateTime read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            String dateTimeStr = jsonReader.nextString();
            return LocalDateTime.parse(dateTimeStr, formatter);
        }
    }
}
