package org.stapledon.caching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stapledon.config.JsonConfigWriter;
import org.stapledon.dto.ImageCacheStats;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

public class ImageCacheStatsUpdater
{
    private final JsonConfigWriter statsUpdater;
    private final Logger logger = LoggerFactory.getLogger(ImageCacheStatsUpdater.class);

    private final String cacheDirectory;

    private ImageCacheStats cacheStats;

    public ImageCacheStatsUpdater(String targetDirectory, JsonConfigWriter statsUpdater)
    {
        this.cacheDirectory = targetDirectory;
        this.statsUpdater = statsUpdater;
    }

    public ImageCacheStats cacheStats()
    {
        if (cacheStats == null)
            updateStats();
        return cacheStats;
    }

    /**
     * Generate Statistics about the Images cached in a particular directory to speed up retrieval at a later date.
     * @return True if successful
     */
    public boolean updateStats()
    {
        File root = new File(cacheDirectory);
        if (!root.exists()) {
            logger.error("{} doesn't exist", cacheDirectory);
            return false;
        }
        cacheStats = new ImageCacheStats();

        // Images are stored by year, gather a list of all years that we have stored
        String[] yearFolders = root.list((dir, name) ->  new File(dir, name).isDirectory());
        if (yearFolders != null && yearFolders.length > 0) {
            cacheStats.years = Arrays.asList(yearFolders);
            Arrays.sort(yearFolders, Comparator.comparing(Integer::valueOf));

            // Find the first image in the first year. This is the oldest image available
            String year = yearFolders[0];
            cacheStats.oldestImage = expand(year, firstImage(String.format("%s/%s", root.getAbsolutePath(), year)));

            // Find the last image in the last year. This is the newest image available
            year = yearFolders[yearFolders.length-1];
            cacheStats.newestImage = expand(year, lastImage(String.format("%s/%s", root.getAbsolutePath(), year)));
        }

        return statsUpdater.save(cacheStats, cacheDirectory);
    }

    private String expand(String path, String image)
    {
        // image will be null if there are no matching images in the directory (eg: Caching failed).
        if (image == null)
            return "";
        return Paths.get(cacheDirectory, path, image).toFile().getAbsolutePath();
    }

    /**
     * Find the first image in the folder
     * @param location Directory to look into
     * @return First item in the directory when sorted by filename
     */
    private String firstImage(String location)
    {
        // Find the first image in the folder
        String[] cachedStrips = images(location);
        if (cachedStrips.length < 1)
            return null;
        return cachedStrips[0];
    }

    /**
     * Find the last image in the folder
     * @param location Directory to look into
     * @return Last item in the directory when sorted by filename
     */
    private String lastImage(String location)
    {
        String[] cachedStrips = images(location);
        if (cachedStrips.length < 1)
            return null;
        return cachedStrips[cachedStrips.length-1];
    }


    /**
     * Get the list of Images in the selected folder
     * @param location Path to search
     * @return List of folders or null if none were found
     */
    private String[] images(String location) {
        File folder = new File(location);
        String[] cachedStrips = folder.list();
        if (cachedStrips == null || cachedStrips.length == 0)
            return new String[] {};
        Arrays.sort(cachedStrips, String::compareTo);
        return cachedStrips;
    }
}
