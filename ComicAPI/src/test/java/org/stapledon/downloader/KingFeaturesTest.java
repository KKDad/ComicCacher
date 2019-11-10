package org.stapledon.downloader;

import org.apache.log4j.Logger;
import org.junit.*;
import org.stapledon.dto.ComicItem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public class KingFeaturesTest {
    private static final Logger log = Logger.getLogger(KingFeaturesTest.class);
    private static Path path;

    @BeforeClass
    public static void setUp() throws Exception {
        path = Files.createTempDirectory("KingFeaturesTest");
        log.info("Using TempDirectory: " + path.toString());
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

    private KingFeatures getSubject(String name, String website, LocalDate fetchDate)
    {
        KingFeatures kingFeatures = new KingFeatures(null, website);
        kingFeatures.setComic(name);
        // Note: KingFeatures only allows retrieval of the last 5 days.
        kingFeatures.setDate(fetchDate);
        kingFeatures.setCacheRoot(path.toString());

        return kingFeatures;
    }


    @Test
    //@Ignore // Fails on bitbucket
    public void ensureCacheTest() {
        LocalDate fetchDate = LocalDate.now().minusDays(3);


        File expectedFile = new File(String.format("%s/BabyBlues/%s.png", path.toString(), fetchDate.format(DateTimeFormatter.ofPattern("yyyy/yyyy-MM-dd"))));
        log.info("Expecting to get file: " + expectedFile.toString());
        Assert.assertFalse("expectedFile should not exist before the subject acts.", expectedFile.exists());

        IDailyComic subject = getSubject("Baby Blues", "https://www.comicskingdom.com/baby-blues", fetchDate);

        // Act
        boolean result = subject.ensureCache();

        // Assert
        Assert.assertTrue("ensureCache() expected to return true", result);
        Assert.assertTrue("expectedFile does not exist", expectedFile.exists());
    }

    @Test
    public void getBabyBluesComicMetadataTest() {
        // Arrange
        LocalDate fetchDate = LocalDate.now().minusDays(3);
        KingFeatures subject = getSubject("Baby Blues", "https://www.comicskingdom.com/baby-blues", fetchDate);

        // Act
        ComicItem item = new ComicItem();
        subject.updateComicMetadata(item);

        // Assert
        Assert.assertTrue(item.author.contains("Baby Blues BY RICK KIRKMAN AND JERRY SCOTT"));
    }

}