package org.stapledon.engine.storage;

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

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(errors.size()).isEqualTo(1);
        assertThat(errors.get(0).getComicName()).isEqualTo("TestComic");
        assertThat(errors.get(0).getErrorMessage()).isEqualTo("Test error message");
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
        assertThat(errors.size()).isEqualTo(5);
        // Most recent should be first
        assertThat(errors.get(0).getErrorMessage()).isEqualTo("Error 7");
        assertThat(errors.get(4).getErrorMessage()).isEqualTo("Error 3");
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
        assertThat(repository.getErrors("Comic1").isEmpty()).isTrue();
        assertThat(repository.getErrors("Comic2").isEmpty()).isFalse();
    }

    @Test
    void shouldReturnEmptyListForComicWithNoErrors() {
        // When
        List<ComicErrorRecord> errors = repository.getErrors("NonExistentComic");

        // Then
        assertThat(errors.isEmpty()).isTrue();
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
        assertThat(allErrors.size()).isEqualTo(2);
        assertThat(allErrors.containsKey("Comic1")).isTrue();
        assertThat(allErrors.containsKey("Comic2")).isTrue();
        assertThat(allErrors.get("Comic1").size()).isEqualTo(2);
        assertThat(allErrors.get("Comic2").size()).isEqualTo(1);
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
        assertThat(count).isEqualTo(3);
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
        assertThat(errors.size()).isEqualTo(1);
        assertThat(errors.get(0).getErrorMessage()).isEqualTo("Test error");
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
        assertThat(errors.size()).isEqualTo(3);
        // Most recent first
        assertThat(errors.get(0).getErrorMessage()).isEqualTo("Storage error");
        assertThat(errors.get(2).getErrorMessage()).isEqualTo("Network error");
    }

    @Test
    void shouldSortErrorsByTimestampDescending() {
        // Given
        String comicName = "TestComic";
        repository.recordError(createTestError(comicName, "First error"));

        // Add small delay to ensure different timestamps
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore - test timing delay, not critical
        }

        repository.recordError(createTestError(comicName, "Second error"));

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore - test timing delay, not critical
        }

        repository.recordError(createTestError(comicName, "Third error"));

        // When
        Map<String, List<ComicErrorRecord>> allErrors = repository.getAllErrors();
        List<ComicErrorRecord> errors = allErrors.get(comicName);

        // Then
        assertThat(errors.size()).isEqualTo(3);
        assertThat(errors.get(0).getTimestamp().isAfter(errors.get(1).getTimestamp()) ||
                errors.get(0).getTimestamp().isEqual(errors.get(1).getTimestamp())).isTrue();
        assertThat(errors.get(1).getTimestamp().isAfter(errors.get(2).getTimestamp()) ||
                errors.get(1).getTimestamp().isEqual(errors.get(2).getTimestamp())).isTrue();
    }

    @Test
    void shouldHandleEmptyFileGracefully() {
        // Given - Repository has been reset, so it has an empty error map
        // (file will exist but be empty JSON object)

        // When
        List<ComicErrorRecord> errors = repository.getErrors("AnyComic");

        // Then
        assertThat(errors.isEmpty()).isTrue();
        assertThat(repository.getComicErrorCount()).isEqualTo(0);
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
