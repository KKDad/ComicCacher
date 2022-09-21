package org.stapledon.downloader;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stapledon.dto.ComicItem;
import org.junit.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

public class GoComicsTest {
    private static final Logger LOG = LoggerFactory.getLogger(GoComicsTest.class);
    private static Path path;

    @BeforeClass
    public static void setUp() throws Exception {
        path = Files.createTempDirectory("GoComicsTest");
        LOG.info("Using TempDirectory: " + path.toString());
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
        GoComics gc = new GoComics(null);
        gc.setComic(name);
        gc.setDate(LocalDate.of(2019, 1, 1 ));
        gc.setCacheRoot(path.toString());

        return gc;
    }


    @Test
    @Ignore("Fails on bitbucket")
    public void ensureCacheTest() {
        File expectedFile = new File(path.toString() + "/AdamAtHome/2019/2019-01-01.png");
        LOG.info("Expecting to get file: " + expectedFile.toString());
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
        ComicItem item = new ComicItem();
        subject.updateComicMetadata(item);

        // Assert
        Assert.assertTrue(item.description.contains("humor of Rob Harrell"));
    }

    @Test
    public void getHermanComicDescription() {
        // Arrange
        GoComics subject = getSubject("Herman");

        // Act
        ComicItem item = new ComicItem();
        subject.updateComicMetadata(item);

        // Assert
        Assert.assertTrue(item.description.contains("It was his greatest wish that HERMAN live on and continue to make us laugh."));
    }

    @ParameterizedTest
    @CsvSource({"Herman,It was his greatest wish that HERMAN live on and continue to make us laugh."})
    public void getComicDescriptionTest(String name, String expected) {
        // Arrange
        GoComics subject = getSubject(name);

        // Act
        ComicItem item = new ComicItem();
        subject.updateComicMetadata(item);

        // Assert
        assertThat(item.description).isNotNull().contains(expected);
    }
}