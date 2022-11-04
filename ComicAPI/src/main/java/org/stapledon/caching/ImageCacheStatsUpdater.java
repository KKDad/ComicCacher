package org.stapledon.caching;

import lombok.extern.slf4j.Slf4j;
import org.stapledon.config.JsonConfigWriter;
import org.stapledon.dto.ImageCacheStats;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

@Slf4j
public class ImageCacheStatsUpdater {
    private final JsonConfigWriter statsUpdater;
    private final String cacheDirectory;

    private ImageCacheStats cacheStats;

    public ImageCacheStatsUpdater(String targetDirectory, JsonConfigWriter statsUpdater) {
        this.cacheDirectory = targetDirectory;
        this.statsUpdater = statsUpdater;
    }

    public ImageCacheStats cacheStats() {
        if (cacheStats == null)
            updateStats();
        return cacheStats;
    }

    /**
     * Generate Statistics about the Images cached in a particular directory to speed up retrieval at a later date.
     *
     * @return True if successful
     */
    public boolean updateStats() {
        var root = new File(cacheDirectory);
        if (!root.exists()) {
            log.error("{} doesn't exist", cacheDirectory);
            return false;
        }
        cacheStats = new ImageCacheStats();

        // Images are stored by year, gather a list of all years that we have stored
        String[] yearFolders = root.list((dir, name) -> new File(dir, name).isDirectory());
        if (yearFolders != null && yearFolders.length > 0) {
            var years = Arrays.asList(yearFolders);
            Arrays.sort(yearFolders, Comparator.comparing(Integer::valueOf));

            cacheStats = ImageCacheStats.builder()
                    .years(years)
                    // Find the first image in the first year. This is the oldest image available
                    .oldestImage(expand(yearFolders[0], firstImage(String.format("%s/%s", root.getAbsolutePath(), yearFolders[0]))))
                    // Find the first image in the first year. This is the oldest image available
                    .newestImage(expand(yearFolders[yearFolders.length - 1], lastImage(String.format("%s/%s", root.getAbsolutePath(), yearFolders[yearFolders.length - 1]))))
                    .build();
        }
        return statsUpdater.save(cacheStats, cacheDirectory);
    }

    private String expand(String path, String image) {
        // image will be null if there are no matching images in the directory (eg: Caching failed).
        if (image == null)
            return "";
        return Paths.get(cacheDirectory, path, image).toFile().getAbsolutePath();
    }

    /**
     * Find the first image in the folder
     *
     * @param location Directory to look into
     * @return First item in the directory when sorted by filename
     */
    private String firstImage(String location) {
        // Find the first image in the folder
        String[] cachedStrips = images(location);
        if (cachedStrips.length < 1)
            return null;
        return cachedStrips[0];
    }

    /**
     * Find the last image in the folder
     *
     * @param location Directory to look into
     * @return Last item in the directory when sorted by filename
     */
    private String lastImage(String location) {
        String[] cachedStrips = images(location);
        if (cachedStrips.length < 1)
            return null;
        return cachedStrips[cachedStrips.length - 1];
    }


    /**
     * Get the list of Images in the selected folder
     *
     * @param location Path to search
     * @return List of folders or null if none were found
     */
    private String[] images(String location) {
        var folder = new File(location);
        var cachedStrips = folder.list();
        if (cachedStrips == null || cachedStrips.length == 0)
            return new String[]{};
        Arrays.sort(cachedStrips, String::compareTo);
        return cachedStrips;
    }
}
