package org.stapledon;

import org.junit.jupiter.api.Test;
import org.stapledon.dto.ComicItem;
import org.stapledon.utils.CacheUtils;
import org.stapledon.utils.Direction;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheUtilsTest {

    ComicItem comicItem() {
        ComicItem item1 = new ComicItem();
        item1.id = 42;
        item1.name = "Fake Comic";
        item1.description = "Comic for Unit Tests";
        item1.oldest = LocalDate.of(1995, 05, 31);
        item1.newest = LocalDate.of(2007, 12, 8);

        return item1;
    }

    private CacheUtils getSubject() {
        File resourcesDirectory = getResourcesDirectory();
        return new CacheUtils(resourcesDirectory.toString());
    }

    @Test
    void findOldestTest() {
        CacheUtils subject = getSubject();
        File result = subject.findOldest(comicItem());

        assertThat(result).isNotNull();
        assertThat(result.getAbsolutePath()).contains("2008-01-10");
    }

    @Test
    void findNewestTest() {
        CacheUtils subject = getSubject();
        File result = subject.findNewest(comicItem());

        assertThat(result).isNotNull();
        assertThat(result.getAbsolutePath()).contains("2019-03-22");
    }

    @Test
    void findFirstForwardTest() {
        CacheUtils subject = getSubject();
        File result = subject.findFirst(comicItem(), Direction.FORWARD);

        assertThat(result).isNotNull();
        assertThat(result.getAbsolutePath()).contains("2008-01-10");
    }


    @Test
    void findFirstBackwardsTest() throws IOException {
        CacheUtils subject = getSubject();
        File result = subject.findFirst(comicItem(), Direction.BACKWARD);

        assertThat(result).isNotNull();
        assertThat(result.getAbsolutePath()).contains("2019-03-22");
    }


    @Test
    void findPreviousTest() throws IOException {
        LocalDate dt = LocalDate.of(2008, 01, 11);
        CacheUtils subject = getSubject();
        File result = subject.findPrevious(comicItem(), dt);

        assertThat(result).isNotNull();
        assertThat(result.getAbsolutePath()).contains("2008-01-10");
    }

    @Test
    void findNextTest() throws IOException {
        LocalDate dt = LocalDate.of(2008, 01, 11);
        CacheUtils subject = getSubject();
        File result = subject.findNext(comicItem(), dt);

        assertThat(result).isNotNull();
        assertThat(result.getAbsolutePath()).contains("2010-06-28");
    }


    /**
     * Helper method to the test resources directory
     *
     * @return File
     */
    public static File getResourcesDirectory() {
        File resourcesDirectory = new File("src/test/resources");
        if (!resourcesDirectory.exists())
            resourcesDirectory = new File("../src/test/resources");

        assertThat(resourcesDirectory).exists();
        return resourcesDirectory;
    }
}