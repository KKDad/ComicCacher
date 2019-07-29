package org.stapledon;

import org.stapledon.utils.CacheUtils;
import org.stapledon.utils.Direction;
import org.stapledon.api.ComicApiApplication;
import org.stapledon.config.ApiConfigLoader;
import org.stapledon.dto.ComicItem;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

public class CacheUtilsTest {

    ComicItem comicItem()
    {
        ComicItem item1 = new ComicItem();
        item1.id = 42;
        item1.name = "Fake Comic";
        item1.description = "Comic for Unit Tests";
        item1.oldest = LocalDate.of(1995, 05, 31);
        item1.newest = LocalDate.of(2007, 12, 8);

        return item1;
    }

    private CacheUtils getSubject()
    {
        File resourcesDirectory = getResourcesDirectory();

        ComicApiApplication.getConfig();
        return new CacheUtils(resourcesDirectory.getAbsolutePath());
    }

    @Test
     public void findOldestTest()
    {
        CacheUtils subject = getSubject();
        File result = subject.findOldest(comicItem());

        Assert.assertNotNull(result);
        Assert.assertTrue(String.format("Checking if '%s' contains '2008-01-10'", result.getAbsolutePath()), result.getAbsolutePath().contains("2008-01-10"));
    }

    @Test
    public void findNewestTest()
    {
        CacheUtils subject = getSubject();
        File result = subject.findNewest(comicItem());

        Assert.assertNotNull(result);
        Assert.assertTrue(String.format("Checking if '%s' contains '2019-03-22'", result.getAbsolutePath()), result.getAbsolutePath().contains("2019-03-22"));
    }

    @Test
    public void findFirstForwardTest()
    {
        CacheUtils subject = getSubject();
        File result = subject.findFirst(comicItem(), Direction.FORWARD);

        Assert.assertNotNull(result);
        Assert.assertTrue(String.format("Checking if '%s' contains '2008-01-10'", result.getAbsolutePath()), result.getAbsolutePath().contains("2008-01-10"));
    }


    @Test
    public void findFirstBackwardsTest() throws IOException
    {
        CacheUtils subject = getSubject();
        File result = subject.findFirst(comicItem(), Direction.BACKWARD);

        Assert.assertNotNull(result);
        Assert.assertTrue(String.format("Checking if '%s' contains '2019-03-22'", result.getAbsolutePath()), result.getAbsolutePath().contains("2019-03-22"));
    }


    @Test
    public void findPreviousTest() throws IOException
    {
        LocalDate dt =  LocalDate.of(2008, 01, 11);
        CacheUtils subject = getSubject();
        File result = subject.findPrevious(comicItem(), dt);

        Assert.assertNotNull(result);
        Assert.assertTrue(String.format("Checking if '%s' contains '2008-01-10'", result.getAbsolutePath()), result.getAbsolutePath().contains("2008-01-10"));
    }

    @Test
    public void findNextTest() throws IOException
    {
        LocalDate dt =  LocalDate.of(2008, 01, 11);
        CacheUtils subject = getSubject();
        File result = subject.findNext(comicItem(), dt);

        Assert.assertNotNull(result);
        Assert.assertTrue(String.format("Checking if '%s' contains '2010-06-28'", result.getAbsolutePath()), result.getAbsolutePath().contains("2010-06-28"));
    }




    /**
     * Helper method to the test resources directory
     * @return File
     */
    public static File getResourcesDirectory() {
        File resourcesDirectory = new File("src/test/resources");
        if (!resourcesDirectory.exists())
            resourcesDirectory = new File("../src/test/resources");

        Assert.assertTrue(resourcesDirectory.exists());
        return resourcesDirectory;
    }
}