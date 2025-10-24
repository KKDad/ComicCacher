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
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.dto.ImageMetadata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

class ImageMetadataRepositoryTest {

    @TempDir
    Path tempDir;

    private ImageMetadataRepository repository;
    private Gson gson;

    @BeforeEach
    void setUp() {
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .setPrettyPrinting()
                .create();

        repository = new ImageMetadataRepository(gson);
    }

    @Test
    void shouldSaveMetadata() throws IOException {
        // Given
        String imagePath = tempDir.resolve("2023-01-15.png").toString();
        createEmptyFile(imagePath); // Create image file

        ImageMetadata metadata = createTestMetadata(imagePath);

        // When
        boolean result = repository.saveMetadata(metadata);

        // Then
        assertTrue(result);
        assertTrue(new File(tempDir.resolve("2023-01-15.json").toString()).exists());
    }

    @Test
    void shouldLoadMetadata() throws IOException {
        // Given
        String imagePath = tempDir.resolve("2023-01-15.png").toString();
        createEmptyFile(imagePath);

        ImageMetadata originalMetadata = createTestMetadata(imagePath);
        repository.saveMetadata(originalMetadata);

        // When
        Optional<ImageMetadata> loadedMetadata = repository.loadMetadata(imagePath);

        // Then
        assertTrue(loadedMetadata.isPresent());
        assertEquals(originalMetadata.getFilePath(), loadedMetadata.get().getFilePath());
        assertEquals(originalMetadata.getFormat(), loadedMetadata.get().getFormat());
        assertEquals(originalMetadata.getWidth(), loadedMetadata.get().getWidth());
        assertEquals(originalMetadata.getHeight(), loadedMetadata.get().getHeight());
        assertEquals(originalMetadata.getSizeInBytes(), loadedMetadata.get().getSizeInBytes());
        assertEquals(originalMetadata.getColorMode(), loadedMetadata.get().getColorMode());
    }

    @Test
    void shouldReturnEmptyWhenMetadataDoesNotExist() {
        // Given
        String imagePath = tempDir.resolve("nonexistent.png").toString();

        // When
        Optional<ImageMetadata> metadata = repository.loadMetadata(imagePath);

        // Then
        assertTrue(metadata.isEmpty());
    }

    @Test
    void shouldCheckIfMetadataExists() throws IOException {
        // Given
        String imagePath = tempDir.resolve("test.png").toString();
        createEmptyFile(imagePath);

        // When/Then - Before saving
        assertFalse(repository.metadataExists(imagePath));

        // When/Then - After saving
        repository.saveMetadata(createTestMetadata(imagePath));
        assertTrue(repository.metadataExists(imagePath));
    }

    @Test
    void shouldDeleteMetadata() throws IOException {
        // Given
        String imagePath = tempDir.resolve("test.png").toString();
        createEmptyFile(imagePath);

        ImageMetadata metadata = createTestMetadata(imagePath);
        repository.saveMetadata(metadata);
        assertTrue(repository.metadataExists(imagePath));

        // When
        boolean result = repository.deleteMetadata(imagePath);

        // Then
        assertTrue(result);
        assertFalse(repository.metadataExists(imagePath));
    }

    @Test
    void shouldReturnTrueWhenDeletingNonExistentMetadata() {
        // Given
        String imagePath = tempDir.resolve("nonexistent.png").toString();

        // When
        boolean result = repository.deleteMetadata(imagePath);

        // Then
        assertTrue(result); // Already deleted
    }

    @Test
    void shouldHandleDifferentImageExtensions() throws IOException {
        // Test PNG
        String pngPath = tempDir.resolve("image.png").toString();
        createEmptyFile(pngPath);
        repository.saveMetadata(createTestMetadata(pngPath));
        assertTrue(repository.metadataExists(pngPath));
        assertTrue(new File(tempDir.resolve("image.json").toString()).exists());

        // Test JPG
        String jpgPath = tempDir.resolve("image.jpg").toString();
        createEmptyFile(jpgPath);
        repository.saveMetadata(createTestMetadata(jpgPath));
        assertTrue(repository.metadataExists(jpgPath));
        assertTrue(new File(tempDir.resolve("image.json").toString()).exists());

        // Test JPEG
        String jpegPath = tempDir.resolve("photo.jpeg").toString();
        createEmptyFile(jpegPath);
        repository.saveMetadata(createTestMetadata(jpegPath));
        assertTrue(repository.metadataExists(jpegPath));
    }

    @Test
    void shouldHandleGifExtension() throws IOException {
        // Given
        String gifPath = tempDir.resolve("animation.gif").toString();
        createEmptyFile(gifPath);

        // When
        repository.saveMetadata(createTestMetadata(gifPath));

        // Then
        assertTrue(repository.metadataExists(gifPath));
        assertTrue(new File(tempDir.resolve("animation.json").toString()).exists());
    }

    @Test
    void shouldHandleWebpExtension() throws IOException {
        // Given
        String webpPath = tempDir.resolve("modern.webp").toString();
        createEmptyFile(webpPath);

        // When
        repository.saveMetadata(createTestMetadata(webpPath));

        // Then
        assertTrue(repository.metadataExists(webpPath));
        assertTrue(new File(tempDir.resolve("modern.json").toString()).exists());
    }

    @Test
    void shouldHandleTiffExtension() throws IOException {
        // Given
        String tiffPath = tempDir.resolve("photo.tiff").toString();
        createEmptyFile(tiffPath);

        // When
        repository.saveMetadata(createTestMetadata(tiffPath));

        // Then
        assertTrue(repository.metadataExists(tiffPath));
        assertTrue(new File(tempDir.resolve("photo.json").toString()).exists());
    }

    @Test
    void shouldHandleTifExtension() throws IOException {
        // Given
        String tifPath = tempDir.resolve("photo.tif").toString();
        createEmptyFile(tifPath);

        // When
        repository.saveMetadata(createTestMetadata(tifPath));

        // Then
        assertTrue(repository.metadataExists(tifPath));
        assertTrue(new File(tempDir.resolve("photo.json").toString()).exists());
    }

    @Test
    void shouldHandleBmpExtension() throws IOException {
        // Given
        String bmpPath = tempDir.resolve("bitmap.bmp").toString();
        createEmptyFile(bmpPath);

        // When
        repository.saveMetadata(createTestMetadata(bmpPath));

        // Then
        assertTrue(repository.metadataExists(bmpPath));
        assertTrue(new File(tempDir.resolve("bitmap.json").toString()).exists());
    }

    @Test
    void shouldHandleFilesWithoutExtension() throws IOException {
        // Given
        String noExtPath = tempDir.resolve("noextension").toString();
        createEmptyFile(noExtPath);

        // When
        repository.saveMetadata(createTestMetadata(noExtPath));

        // Then
        assertTrue(repository.metadataExists(noExtPath));
        assertTrue(new File(noExtPath + ".json").exists());
    }

    @Test
    void shouldPreserveAllMetadataFields() throws IOException {
        // Given
        String imagePath = tempDir.resolve("complete.png").toString();
        createEmptyFile(imagePath);

        ImageMetadata metadata = ImageMetadata.builder()
                .filePath(imagePath)
                .format(ImageFormat.PNG)
                .width(1920)
                .height(1080)
                .sizeInBytes(524288)
                .colorMode(ImageMetadata.ColorMode.COLOR)
                .samplePercentage(5.0)
                .captureTimestamp(LocalDateTime.of(2023, 1, 15, 10, 30, 45))
                .sourceUrl("http://example.com/image.png")
                .build();

        // When
        repository.saveMetadata(metadata);
        Optional<ImageMetadata> loaded = repository.loadMetadata(imagePath);

        // Then
        assertTrue(loaded.isPresent());
        ImageMetadata loadedMetadata = loaded.get();
        assertEquals("http://example.com/image.png", loadedMetadata.getSourceUrl());
        assertEquals(5.0, loadedMetadata.getSamplePercentage());
        assertEquals(LocalDateTime.of(2023, 1, 15, 10, 30, 45), loadedMetadata.getCaptureTimestamp());
    }

    @Test
    void shouldHandleGrayscaleColorMode() throws IOException {
        // Given
        String imagePath = tempDir.resolve("grayscale.png").toString();
        createEmptyFile(imagePath);

        ImageMetadata metadata = ImageMetadata.builder()
                .filePath(imagePath)
                .format(ImageFormat.PNG)
                .width(100)
                .height(100)
                .sizeInBytes(1000)
                .colorMode(ImageMetadata.ColorMode.GRAYSCALE)
                .samplePercentage(5.0)
                .captureTimestamp(LocalDateTime.now())
                .sourceUrl(null)
                .build();

        // When
        repository.saveMetadata(metadata);
        Optional<ImageMetadata> loaded = repository.loadMetadata(imagePath);

        // Then
        assertTrue(loaded.isPresent());
        assertEquals(ImageMetadata.ColorMode.GRAYSCALE, loaded.get().getColorMode());
    }

    @Test
    void shouldNotSaveInvalidMetadata() throws IOException {
        // Given
        String imagePath = tempDir.resolve("invalid.png").toString();
        createEmptyFile(imagePath);

        ImageMetadata invalidMetadata = ImageMetadata.builder()
                .filePath(imagePath)
                .format(ImageFormat.UNKNOWN)
                .width(0)
                .height(0)
                .sizeInBytes(0)
                .colorMode(ImageMetadata.ColorMode.UNKNOWN)
                .samplePercentage(0.0)
                .captureTimestamp(LocalDateTime.now())
                .sourceUrl(null)
                .build();

        // When
        boolean result = repository.saveMetadata(invalidMetadata);

        // Then
        assertFalse(result);
        assertFalse(repository.metadataExists(imagePath));
    }

    @Test
    void shouldSaveMetadataWithUnknownColorMode() throws IOException {
        // Given - Valid metadata except ColorMode is UNKNOWN (acceptable)
        String imagePath = tempDir.resolve("grayscale-unknown.png").toString();
        createEmptyFile(imagePath);

        ImageMetadata metadata = ImageMetadata.builder()
                .filePath(imagePath)
                .format(ImageFormat.PNG)
                .width(100)
                .height(100)
                .sizeInBytes(1000)
                .colorMode(ImageMetadata.ColorMode.UNKNOWN)
                .samplePercentage(5.0)
                .captureTimestamp(LocalDateTime.now())
                .sourceUrl(null)
                .build();

        // When
        boolean result = repository.saveMetadata(metadata);

        // Then
        assertTrue(result);
        assertTrue(repository.metadataExists(imagePath));
        Optional<ImageMetadata> loaded = repository.loadMetadata(imagePath);
        assertTrue(loaded.isPresent());
        assertEquals(ImageMetadata.ColorMode.UNKNOWN, loaded.get().getColorMode());
    }

    @Test
    void shouldNotSaveMetadataWithZeroDimensions() throws IOException {
        // Given
        String imagePath = tempDir.resolve("zero-dims.png").toString();
        createEmptyFile(imagePath);

        ImageMetadata metadata = ImageMetadata.builder()
                .filePath(imagePath)
                .format(ImageFormat.PNG)
                .width(0)
                .height(0)
                .sizeInBytes(1000)
                .colorMode(ImageMetadata.ColorMode.COLOR)
                .samplePercentage(5.0)
                .captureTimestamp(LocalDateTime.now())
                .sourceUrl(null)
                .build();

        // When
        boolean result = repository.saveMetadata(metadata);

        // Then
        assertFalse(result);
        assertFalse(repository.metadataExists(imagePath));
    }

    @Test
    void shouldNotSaveMetadataWithZeroFileSize() throws IOException {
        // Given
        String imagePath = tempDir.resolve("zero-size.png").toString();
        createEmptyFile(imagePath);

        ImageMetadata metadata = ImageMetadata.builder()
                .filePath(imagePath)
                .format(ImageFormat.PNG)
                .width(100)
                .height(100)
                .sizeInBytes(0)
                .colorMode(ImageMetadata.ColorMode.COLOR)
                .samplePercentage(5.0)
                .captureTimestamp(LocalDateTime.now())
                .sourceUrl(null)
                .build();

        // When
        boolean result = repository.saveMetadata(metadata);

        // Then
        assertFalse(result);
        assertFalse(repository.metadataExists(imagePath));
    }

    // Helper methods

    private ImageMetadata createTestMetadata(String imagePath) {
        return ImageMetadata.builder()
                .filePath(imagePath)
                .format(ImageFormat.PNG)
                .width(100)
                .height(100)
                .sizeInBytes(1024)
                .colorMode(ImageMetadata.ColorMode.COLOR)
                .samplePercentage(5.0)
                .captureTimestamp(LocalDateTime.now())
                .sourceUrl("http://example.com/test.png")
                .build();
    }

    private void createEmptyFile(String path) throws IOException {
        File file = new File(path);
        file.getParentFile().mkdirs();
        file.createNewFile();
    }

    // Gson adapter
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
