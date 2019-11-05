package org.stapledon.caching;

import org.junit.Assert;
import org.junit.Test;
import org.stapledon.CacheUtilsTest;
import org.stapledon.TestUtils;
import org.stapledon.config.JsonConfigWriter;
import org.stapledon.dto.ImageCacheStats;

import java.io.File;

import static org.mockito.Mockito.*;

public class ImageCacheStatsUpdaterTest
{
    @Test
    public void UpdateStatsTest()
    {
        // Arrange
        File resourcesDirectory = CacheUtilsTest.getResourcesDirectory();
        TestUtils.setEnv("CACHE_DIRECTORY", resourcesDirectory.toString());

        JsonConfigWriter mockWriter = mock(JsonConfigWriter.class);
        when(mockWriter.save(any(ImageCacheStats.class), any(String.class))).thenReturn(true);

        ImageCacheStatsUpdater subject =  new ImageCacheStatsUpdater(resourcesDirectory.getPath() + "/FakeComic", mockWriter);

        // Act
        boolean result = subject.updateStats();
        ImageCacheStats stats = subject.cacheStats();

        // Assert
        Assert.assertTrue(result);
        Assert.assertEquals(stats.years.size(), 3);
        Assert.assertTrue(stats.oldestImage.contains("2008-01-10.png"));
        Assert.assertTrue(stats.newestImage.contains("2019-03-22.png"));
    }
}