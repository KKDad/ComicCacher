package org.stapledon.infrastructure.config;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stapledon.api.dto.comic.ComicConfig;
import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.infrastructure.config.properties.CacheProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class JsonConfigWriterTest {
    private static final Logger LOG = LoggerFactory.getLogger(JsonConfigWriterTest.class);
    private Path path;
    
    @Mock
    private ConfigurationFacade configurationFacade;

    @BeforeEach
    void setup() throws IOException {
        path = Files.createTempDirectory("JsonConfigWriterTest");
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
        
        // Mock the configuration facade behavior
        ComicConfig comicConfig = new ComicConfig();
        when(configurationFacade.loadComicConfig()).thenReturn(comicConfig);
        when(configurationFacade.saveComicConfig(any(ComicConfig.class))).thenReturn(true);
        
        JsonConfigWriter subject = new JsonConfigWriter(new GsonProvider().gson(), cacheProperties, configurationFacade);
        subject.save(item);

        // Assert
        verify(configurationFacade).loadComicConfig();
        verify(configurationFacade).saveComicConfig(any(ComicConfig.class));
    }


    private ComicItem generateTestComicItem(String name) {
        return ComicItem.builder()
                .id(42)
                .name("test Comic")
                .description("Comic for Unit Tests")
                .oldest(LocalDate.of(1995, 05, 31))
                .newest(LocalDate.of(2007, 12, 8))
                .source("gocomics")
                .sourceIdentifier("testcomic")
                .enabled(true)
                .avatarAvailable(false)
                .build();
    }
}