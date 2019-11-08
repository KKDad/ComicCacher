package org.stapledon.downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stapledon.caching.ImageCacheStatsUpdater;
import org.stapledon.config.CacherConfig;
import org.stapledon.config.CacherConfigLoader;
import org.stapledon.config.JsonConfigWriter;
import org.stapledon.dto.ComicItem;
import org.stapledon.web.DefaultTrustManager;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;

public class ComicCacher
{

    private final CacherConfig config;
    private final JsonConfigWriter statsUpdater;
    private final Logger logger = LoggerFactory.getLogger(ComicCacher.class);

    private final String cacheDirectory;

    public ComicCacher() throws NoSuchAlgorithmException, KeyManagementException
    {
        // configure the SSLContext with a TrustManager
        SSLContext ctx = SSLContext.getInstance("TLSv1.2");
        ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
        SSLContext.setDefault(ctx);

        String directory = System.getenv("CACHE_DIRECTORY");
        if (directory == null) {
            logger.error("CACHE_DIRECTORY not set. Defaulting to /comics");
            directory = "/comics";
        }
        this.cacheDirectory = directory;
        logger.warn("Caching to {}", this.cacheDirectory);

        config = new CacherConfigLoader().load();
        statsUpdater = new JsonConfigWriter(this.cacheDirectory + "/comics.json");
    }

    /**
     * Attempt to cache all of the configured comics. Does not stop if one or more comics fail to be cached.
     * @return True if all comics where successfully cached, false if one or more fail to be cached.
     */
    public boolean cacheAll()
    {
        boolean result = true;
        for (CacherConfig.GoComics dcc : config.dailyComics)
        {
            try {
                cacheComic(dcc);
            } catch (Exception e) {
                logger.error(String.format("Failed to cache %s : %s", dcc.name, e.getMessage()), e);
                result = false;
            }
        }
        return result;
    }

    public boolean cacheComic(ComicItem comic)
    {
        CacherConfig.GoComics dailyComic = lookupGoComics(comic);
        if (dailyComic != null)
            return cacheComic(dailyComic);

        return false;
    }

    CacherConfig.GoComics lookupGoComics(ComicItem comic)
    {
        if (config.dailyComics.isEmpty())
            return null;
        return config.dailyComics.stream().filter(p -> p.name.equalsIgnoreCase(comic.name)).findFirst().orElse(null);
    }


    public boolean cacheComic(CacherConfig.GoComics dcc)
    {
        logger.info("***********************************************************************************************");
        logger.info("Processing: {}", dcc.name);
        logger.info("***********************************************************************************************");

        // Only search back 7 days unless we are refilling
        LocalDate startDate = dcc.startDate;
        if (startDate.equals(LocalDate.of(2019, 4, 1)))
            startDate = LocalDate.now().minusDays(7);

        IDailyComic comics = new GoComics(null)
                .setCacheRoot(cacheDirectory)
                .setComic(dcc.name)
                .setDate(startDate);

        ComicItem comicItem = statsUpdater.fetch(dcc.name);
        if (comicItem == null) {
            comicItem = new ComicItem();
            comicItem.id = dcc.name.hashCode();
            comicItem.name = dcc.name;
            comicItem.oldest = dcc.startDate;
        }
        comics.updateComicMetadata(comicItem);
        while (!comics.advance().equals(comics.getLastStripOn()))
            comics.ensureCache();


        comicItem.newest = comics.getLastStripOn();
        comicItem.enabled = true;
        statsUpdater.save(comicItem);

        // Update statistics about the cached images
        ImageCacheStatsUpdater cache = new ImageCacheStatsUpdater(((GoComics)comics).cacheLocation(), statsUpdater);
        cache.updateStats();


        return true;
    }
}
