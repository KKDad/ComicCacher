package org.stapledon.downloader;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stapledon.dto.ComicItem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

class GoComicsIntegrationTest {
    private static final Logger LOG = LoggerFactory.getLogger(GoComicsIntegrationTest.class);
    private static Path path;

    @BeforeAll
    static void setUp() throws Exception {
        path = Files.createTempDirectory("GoComicsTest");
        LOG.info("Using TempDirectory: " + path.toString());
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

    private GoComics getSubject(String name) {
        GoComics gc = new GoComics(null);
        gc.setComic(name);
        gc.setDate(LocalDate.of(2019, 1, 1));
        gc.setCacheRoot(path.toString());

        return gc;
    }

    @Test
    void ensureCacheTest() {
        File expectedFile = new File(path.toString() + "/AdamAtHome/2019/2019-01-01.png");
        LOG.info("Expecting to get file: " + expectedFile.toString());
        assertThat(expectedFile).doesNotExist();

        IDailyComic subject = getSubject("Adam at Home");

        // Act
        boolean result = subject.ensureCache();

        // Assert
        assertThat(result).isTrue();
        assertThat(expectedFile).exists();
    }

    @Test
    void advanceTest() {
        // Arrange
        GoComics subject = getSubject("Adam at Home");

        // Act
        LocalDate result = subject.advance();

        // Assert
        assertThat(result).isEqualTo(LocalDate.of(2019, 1, 2));
    }


    @ParameterizedTest
    @CsvSource({
            "Adam at Home,By Rob Harrell",
            "Agnes,By Tony Cochran",
            "AndyCap,By Reg Smythe",
            "BC,By Mastroianni and Hart",
            "CalvinAndHobbes,By Bill Watterson",
            "Cathy,By Cathy Guisewite",
            "CitizenDog,By Mark O'Hare",
            "Committed,By Gary Larson",
            "Doonesbury,By Garry Trudeau",
            "Drabble,By Kevin Fagan",
            "ForBetterorForWorse,By Lynn Johnston",
            "FoxTrot,By Bill Amend",
            "Frank-And-Ernest,By Thaves",
            "Garfield,By Jim Davis",
            "GetFuzzy,By Darby Conley",
            "Herman,By Jim Unger",
            "Luann,By Greg Evans",
            "NonSequitur,By Wiley Miller",
            "Overboard,By Chip Dunham",
            "OvertheHedge,By T Lewis and Michael Fry",
            "PCandPixel,By Tak Bui",
            "Peanuts,By Charles Schulz",
            "PearlsBeforeSwine,By Stephan Pastis",
            "Pickles,By Brian Crane",
            "RealityCheck,By Dave Whamond",
            "RoseisRose,By Don Wimmer and Pat Brady",
            "ScaryGary,By Mark Buford",
            "Shoe,By Gary Brookins and Susie MacNelly",
            "TheBoondocks,By Aaron McGruder",
            "TheBornLoser,By Art and Chip Sansom",
            "TheDuplex,By Glenn McCoy",
            "TheGrizzWells,By Bill Schorr",
            "WizardOfId,By Parker and Hart",
            "WorkingDaze,By John Zakour and Scott Roberts",
            "Ziggy,By Tom Wilson & Tom II"
               })
    void getComicDescriptionTest(String name, String expected) {
        // Arrange
        GoComics subject = getSubject(name);

        // Act
        ComicItem item = new ComicItem();
        subject.updateComicMetadata(item);

        // Assert
        assertThat(item.description).isNotNull();
        assertThat(item.avatarAvailable).isTrue();
        assertThat(item.author).contains(expected);
    }
}