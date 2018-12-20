package org.stapledon.config;

import com.google.common.io.CharSource;
import org.apache.commons.io.input.ReaderInputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.LocalDate;

public class YamlConfigLoaderTest {

    @Test
    public void loadSimpleTest() throws IOException
    {

        // Arrange
        String initialString = "cacheDirectory: \"myCacheDir\"\ndailyComics:\n  - { name: \"Adam At Home\", startDate: 2008-01-09 }";
        InputStream targetStream =   new ReaderInputStream(CharSource.wrap(initialString).openStream(), Charset.defaultCharset());

        // Act
        ComicCacherConfig results = new YamlConfigLoader().load(targetStream);

        // Assert
        Assert.assertEquals("myCacheDir", results.getCacheDirectory());
        Assert.assertEquals(1, results.getDailyComics().size());
        Assert.assertEquals("Adam At Home", results.getDailyComics().get(0).getName());
        Assert.assertEquals(LocalDate.of(2008, 1, 9), results.getDailyComics().get(0).getStartDate());
    }
}