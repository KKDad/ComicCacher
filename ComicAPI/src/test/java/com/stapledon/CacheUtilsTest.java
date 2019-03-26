package com.stapledon;

import com.stapledon.cache.CacheUtils;
import com.stapledon.cache.Direction;
import com.stapledon.comic.ComicApiApplication;
import com.stapledon.config.JsonConfigLoader;
import com.stapledon.interop.ComicItem;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;

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

    private CacheUtils getSubject() throws IOException {
        File resourcesDirectory = new File("src/test/resources");
        if (!resourcesDirectory.exists())
            resourcesDirectory = new File("../src/test/resources");

        Assert.assertTrue(resourcesDirectory.exists());

        ComicApiApplication.config = new JsonConfigLoader().load();
        return new CacheUtils(resourcesDirectory.getAbsolutePath());
    }

    @Test
    public void findOldestTest() throws IOException
    {
        CacheUtils subject = getSubject();
        File result = subject.findFirst(comicItem(), Direction.FORWARD);

        Assert.assertNotNull(result);
        Assert.assertTrue(String.format("Checking if '%s' contains '\"FakeComic\\\\2008\\2008-01-10.png\"'", result.getAbsolutePath()), result.getAbsolutePath().contains("FakeComic\\2008\\2008-01-10"));
    }


    @Test
    public void findNewestTest() throws IOException
    {
        CacheUtils subject = getSubject();
        File result = subject.findFirst(comicItem(), Direction.BACKWARD);

        Assert.assertNotNull(result);
        Assert.assertTrue(String.format("Checking if '%s' contains '\"FakeComic\\\\2019\\\\2019-03-22\"'", result.getAbsolutePath()), result.getAbsolutePath().contains("FakeComic\\2019\\2019-03-22"));
    }


}