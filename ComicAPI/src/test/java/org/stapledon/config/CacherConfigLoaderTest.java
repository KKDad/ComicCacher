package org.stapledon.config;

import com.google.common.io.CharSource;
import com.google.gson.Gson;
import org.apache.commons.io.input.ReaderInputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.Month;

public class CacherConfigLoaderTest {

    @Test
    public void loadSimpleTest() throws IOException
    {

        // Arrange
        String initialString = "{'dailyComics':[{'name':'Adam At Home','startDate':{'year':2008,'month':1,'day':9}}]}\n";
        InputStream targetStream =   new ReaderInputStream(CharSource.wrap(initialString.replace('\'', '\"')).openStream(), Charset.defaultCharset());

        // Act
        CacherConfig results = new CacherConfigLoader().load(targetStream);

        // Assert
        Assert.assertEquals(1, results.dailyComics.size());
        Assert.assertEquals("Adam At Home", results.dailyComics.get(0).name);
        Assert.assertEquals(LocalDate.of(2008, 1, 9), results.dailyComics.get(0).startDate);
    }


    @Test
    public void saveSimpleTest()
    {
        // Arrange
        CacherConfig config = new CacherConfig();
        CacherConfig.GoComics comic = config.new GoComics();
        comic.name = "Adam At Home";
        comic.startDate = LocalDate.of(2019, Month.JANUARY, 1);
        config.dailyComics.add(comic);

        // Act
        Gson gson = new Gson();
        String serialized = gson.toJson(config);

        Assert.assertNotNull(serialized);
    }

}