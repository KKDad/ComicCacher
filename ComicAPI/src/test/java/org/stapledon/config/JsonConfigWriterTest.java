package org.stapledon.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stapledon.dto.ComicItem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JsonConfigWriterTest {
    private static final Logger LOG = LoggerFactory.getLogger(JsonConfigWriterTest.class);
    private Path path;

    @BeforeEach
    void setup()throws IOException
    {
        path = Files.createTempDirectory("JsonConfigWriterTest");
    }

    @AfterEach
    void teardown()throws IOException
    {
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

        String fileName = String.format("%s/%s.json", path.toString(), UUID.randomUUID());
        LOG.info(String.format("Writing to %s", fileName));

        // Act
        JsonConfigWriter subject = new JsonConfigWriter(fileName);
        subject.save(item);

        // Assert
        File f = new File(fileName);
        assertThat(f).exists();
        assertThat(f).isNotEmpty();
    }


    private ComicItem generateTestComicItem(String name) {
        ComicItem item = new ComicItem();
        item.name = name;
        item.description = "test description";
        item.newest = LocalDate.of(2018, 1, 1);
        item.oldest = LocalDate.of(2017, 1, 1);
        return item;
    }
}