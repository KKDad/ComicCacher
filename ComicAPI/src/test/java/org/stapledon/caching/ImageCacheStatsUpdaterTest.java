package org.stapledon.caching;

import org.junit.jupiter.api.Test;
import org.stapledon.CacheUtilsTest;
import org.stapledon.config.JsonConfigManager;
import org.stapledon.dto.ImageCacheStats;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ImageCacheStatsUpdaterTest {
    @Test
    void UpdateStatsTest() {
        // Arrange
        File resourcesDirectory = CacheUtilsTest.getResourcesDirectory();

        JsonConfigManager mockWriter = mock(JsonConfigManager.class);
        when(mockWriter.save(any(ImageCacheStats.class), any(String.class))).thenReturn(true);

        ImageCacheStatsUpdater subject = new ImageCacheStatsUpdater(resourcesDirectory.getPath() + "/FakeComic", mockWriter);

        // Act
        boolean result = subject.updateStats();
        ImageCacheStats stats = subject.cacheStats();

        // Assert
        assertThat(result).isTrue();
        assertThat(stats.getYears()).hasSize(3);
        assertThat(stats.getOldestImage()).contains("2008-01-10.png");
        assertThat(stats.getNewestImage()).contains("2019-03-22.png");
    }
}