package com.stapledon.config;

import com.stapledon.interop.ComicItem;
import org.apache.log4j.Logger;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;

import static org.junit.Assert.*;

public class JsonConfigWriterTest {
    private final Logger logger = Logger.getLogger(JsonConfigWriterTest.class);
    private Path path;

    @Before
    public void setup()throws IOException
    {
        path = Files.createTempDirectory("JsonConfigWriterTest");
    }

    @After
    public void teardown()throws IOException
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
    public void saveTest() throws IOException {
        // Arrange
        ComicItem item = generateTestComicItem("saveTest");

        String fileName = String.format("%s/%s.json", path.toString(), UUID.randomUUID());
        logger.info(String.format("Writing to %s", fileName));

        // Act
        JsonConfigWriter subject = new JsonConfigWriter();
        subject.save(item, fileName);

        // Assert
        File f = new File(fileName);
        Assert.assertTrue(f.exists());
        Assert.assertTrue(f.length()> 0);
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