package com.stapledon.cacher;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;

public class GoComicsTest {
    private final Logger log = Logger.getLogger(GoComicsTest.class);
    private Path path;

    @Before
    public void setUp() throws Exception {
        path = Files.createTempDirectory("GoComicsTest");

    }

    @After
    public void tearDown() throws Exception {
        if (!Files.exists(path))
            return;

        // Remote test directory and contents
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private IDailyComic getSubject()
    {
        GoComics gc = new GoComics();
        gc.setComic("Adam At Home");
        gc.setDate(LocalDate.of(2019, 1, 1 ));
        gc.setCacheDirectory(path.toString());

        return gc;
    }


    @Test
    public void ensureCacheTest() {
        File expectedFile = new File(path.toString() + "/AdamAtHome/2019/2019-01-01.png");
        log.info("Expecting to get file: " + expectedFile.toString());
        Assert.assertFalse(expectedFile.exists());

        IDailyComic subject = getSubject();

        // Act
        boolean result = subject.ensureCache();

        // Assert
        Assert.assertTrue(result);
        Assert.assertTrue(expectedFile.exists());
    }

    @Test
    public void advanceTest() {
        // Arrange
        IDailyComic subject = getSubject();

        // Act
        LocalDate result = subject.advance();

        // Assert
        Assert.assertEquals(LocalDate.of(2019, 1, 2), result);
    }
}