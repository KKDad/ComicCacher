package org.stapledon.downloader;

//import org.junit.Assert;
//import org.junit.jupiter.api.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mockito;
//import org.powermock.api.mockito.PowerMockito;
//import org.powermock.core.classloader.annotations.PowerMockIgnore;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
//import org.stapledon.api.ComicApiApplication;
//import org.stapledon.api.ComicsService;
//import org.stapledon.config.IComicsBootstrap;
//import org.stapledon.dto.ComicItem;
//
//import java.security.KeyManagementException;
//import java.security.NoSuchAlgorithmException;
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.List;

//@RunWith(PowerMockRunner.class)
//@PrepareForTest({ ComicsService.class, ComicApiApplication.class})
//@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "sun.security.*"})
public class ComicCacherTest
{
//
//    @Test
//    public void lookupGoComicsTest()
//    {
//        List<ComicItem> comics = comicItem();
//        PowerMockito.mockStatic(ComicsService.class);
//        Mockito.when(ComicsService.getComics()).thenReturn(comics);
//
//        ComicCacher subject = getSubject();
//        IComicsBootstrap result = subject.lookupGoComics(comics.get(0));
//
//        Assert.assertNotNull(result);
//        Assert.assertEquals("Found expected Comic", result.stripName(), comics.get(0).name);
//    }
//
//    private ComicCacher getSubject()
//    {
//        try {
//            return new ComicCacher();
//        } catch (NoSuchAlgorithmException | KeyManagementException e) {
//            Assert.fail(e.getMessage());
//        }
//        return null;
//    }
//
//    List<ComicItem> comicItem()
//    {
//        List<ComicItem> ret = new ArrayList<>();
//        ComicItem item1 = new ComicItem();
//        item1.id = 42;
//        item1.name = "Fake Comic";
//        item1.description = "Comic for Unit Tests";
//        item1.oldest = LocalDate.of(1995, 05, 31);
//        item1.newest = LocalDate.of(2007, 12, 8);
//        ret.add(item1);
//
//        return ret;
//    }

}