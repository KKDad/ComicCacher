package org.stapledon.downloader;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.stapledon.dto.ComicItem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

class ComicsKingdomIntegrationTest {

    private static Path path;

    @BeforeAll
    static void setUp() throws Exception {
        path = Files.createTempDirectory("ComicsKingdomTest");
        //log.info("Using TempDirectory: " + path.toString());
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (!Files.exists(path))
            return;

        // Remote test directory and contents
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private ComicsKingdom getSubject(String name, String website, LocalDate fetchDate) {
        ComicsKingdom comicsKingdom = new ComicsKingdom(null, website);
        comicsKingdom.setComic(name);

        // Note: ComicsKingdom shows the previous 6 days be default, but seems to allow any date
        comicsKingdom.setDate(fetchDate);
        comicsKingdom.setCacheRoot(path.toString());

        return comicsKingdom;
    }


    @Test
    void ensureCacheTest() {
        LocalDate fetchDate = LocalDate.now().minusDays(3);


        File expectedFile = new File(String.format("%s/DaddyDaze/%s.png", path.toString(), fetchDate.format(DateTimeFormatter.ofPattern("yyyy/yyyy-MM-dd"))));
        assertThat(expectedFile).doesNotExist();

        IDailyComic subject = getSubject("Daddy Daze", "https://www.comicskingdom.com/daddy-daze", fetchDate);

        // Act
        boolean result = subject.ensureCache();

        // Assert
        assertThat(result).isTrue();
        assertThat(expectedFile).exists();
    }

    @Test
    void getDaddyDazeMetadataTest() {
        // Arrange
        LocalDate fetchDate = LocalDate.now().minusDays(3);
        IDailyComic subject = getSubject("Daddy Daze", "https://www.comicskingdom.com/daddy-daze", fetchDate);

        // Act
        ComicItem item = new ComicItem();
        subject.updateComicMetadata(item);

        // Assert
        assertThat(item.getAuthor()).isNotNull().contains("Daddy Daze by John Kovaleski");
    }
}