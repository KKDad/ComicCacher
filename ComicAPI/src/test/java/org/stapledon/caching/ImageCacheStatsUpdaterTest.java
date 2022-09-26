package org.stapledon.caching;

import org.junit.jupiter.api.Test;
import org.stapledon.CacheUtilsTest;
import org.stapledon.TestUtils;
import org.stapledon.config.JsonConfigWriter;
import org.stapledon.dto.ImageCacheStats;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ImageCacheStatsUpdaterTest
{
    @Test
    void UpdateStatsTest()
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
        assertThat(result).isTrue();
        assertThat(stats.years).hasSize(3);
        assertThat(stats.oldestImage).contains("2008-01-10.png");
        assertThat(stats.newestImage).contains("2019-03-22.png");
    }
}