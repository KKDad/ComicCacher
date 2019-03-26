package com.stapledon;

import com.stapledon.cache.CacheUtils;
import com.stapledon.cache.Direction;
import com.stapledon.comic.ComicApiApplication;
import com.stapledon.config.JsonConfigLoader;
import com.stapledon.interop.ComicItem;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.time.LocalDate;

public class CacheUtilsTest {

    ComicItem comicItem()
    {
        ComicItem item1 = new ComicItem();
        item1.id = 42;
        item1.name = "Adam At Home";
        item1.description = "Sample Test";
        item1.oldest = LocalDate.of(1995, 05, 31);
        item1.newest = LocalDate.of(2007, 12, 8);

        return item1;
    }

    @Test
    public void findOldestTest()
    {
        ComicApiApplication.config = new JsonConfigLoader().load();
        CacheUtils subject = new CacheUtils(ComicApiApplication.config.cacheDirectoryAlternate);

        File result = subject.findFirst(comicItem(), Direction.FORWARD);

        Assert.assertEquals("z:\\ComicCache\\AdamAtHome\\2008\\2008-01-10.png", result.getAbsolutePath());
    }

    @Test
    public void findNewestTest()
    {
        ComicApiApplication.config = new JsonConfigLoader().load();
        CacheUtils subject = new CacheUtils(ComicApiApplication.config.cacheDirectoryAlternate);

        File result = subject.findFirst(comicItem(), Direction.BACKWARD);

        Assert.assertTrue(result.getAbsolutePath().startsWith("z:\\ComicCache\\AdamAtHome\\2019\\2019-03"));
    }


}