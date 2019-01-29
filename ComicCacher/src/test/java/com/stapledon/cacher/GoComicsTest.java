package com.stapledon.cacher;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Comparator;

public class GoComicsTest {
    private static final Logger log = Logger.getLogger(GoComicsTest.class);
    private static Path path;

    @BeforeClass
    public static void setUp() throws Exception {
        Path currentRelativePath = Paths.get("","build/tests");
        currentRelativePath.toFile().mkdirs();

        path = Files.createTempDirectory(currentRelativePath, "GoComicsTest");
        log.info("Expecting to get file: " + path.toString());
        log.info(String.format("%s", System.getProperty("user.dir")));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (!Files.exists(path))
            return;

        // Remote test directory and contents
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private GoComics getSubject(String name)
    {
        GoComics gc = new GoComics();
        gc.setComic(name);
        gc.setDate(LocalDate.of(2019, 1, 1 ));
        gc.setCacheDirectory(path.toString());

        return gc;
    }


    @Test
    public void ensureCacheTest() {
        File expectedFile = new File(path.toString() + "/AdamAtHome/2019/2019-01-01.png");
        log.info("Expecting to get file: " + expectedFile.toString());
        Assert.assertFalse("expectedFile should not exist before the subject acts.", expectedFile.exists());

        IDailyComic subject = getSubject("Adam at Home");

        // Act
        boolean result = subject.ensureCache();

        // Assert
        Assert.assertTrue("ensureCache() expected to return true", result);
        Assert.assertTrue("expectedFile does not exist", expectedFile.exists());
    }

    @Test
    public void advanceTest()
    {
        // Arrange
        GoComics subject = getSubject("Adam at Home");

        // Act
        LocalDate result = subject.advance();

        // Assert
        Assert.assertEquals(LocalDate.of(2019, 1, 2), result);
    }

    @Test
    public void getAdamComicDescription() {
        // Arrange
        GoComics subject = getSubject("Adam at Home");

        // Act
        String result = subject.getComicDescription();

        // Assert
        Assert.assertTrue(result.contains("humor of Rob Harrell"));
    }

    @Test
    public void getHermanComicDescription() {
        // Arrange
        GoComics subject = getSubject("Herman");

        // Act
        String result = subject.getComicDescription();

        // Assert
        Assert.assertTrue(result.contains("It was his greatest wish that HERMAN live on and continue to make us laugh."));
    }
}