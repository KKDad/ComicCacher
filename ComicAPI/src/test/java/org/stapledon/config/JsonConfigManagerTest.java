package org.stapledon.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stapledon.config.properties.CacheProperties;
import org.stapledon.dto.ComicItem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JsonConfigManagerTest {
    private static final Logger LOG = LoggerFactory.getLogger(JsonConfigManagerTest.class);
    private Path path;

    @BeforeEach
    void setup() throws IOException {
        path = Files.createTempDirectory("JsonConfigManagerTest");
    }

    @AfterEach
    void teardown() throws IOException {
        if (!Files.exists(path))
            return;

        // Remote test directory and contents
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void saveTest() {
        // Arrange
        ComicItem item = generateTestComicItem("saveTest");

        var uuid = UUID.randomUUID().toString();
        String fileName = String.format("%s/%s.json", path.toString(), uuid);
        LOG.info(String.format("Writing to %s", fileName));

        // Act
        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.setLocation(path.toString());
        cacheProperties.setConfig(String.format("%s.json", uuid));
        JsonConfigManager subject = new JsonConfigManager(new GsonProvider().gson(), cacheProperties);
        subject.save(item);

        // Assert
        File f = new File(fileName);
        assertThat(f).exists().isNotEmpty();
    }


    private ComicItem generateTestComicItem(String name) {
        return ComicItem.builder()
                .id(42)
                .name("test Comic")
                .description("Comic for Unit Tests")
                .oldest(LocalDate.of(1995, 05, 31))
                .newest(LocalDate.of(2007, 12, 8))
                .build();
    }
}