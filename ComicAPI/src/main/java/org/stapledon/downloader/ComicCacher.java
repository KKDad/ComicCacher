package org.stapledon.downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stapledon.api.ComicApiApplication;
import org.stapledon.config.CacherConfig;
import org.stapledon.config.CacherConfigLoader;
import org.stapledon.config.JsonConfigWriter;
import org.stapledon.dto.ComicItem;
import org.stapledon.utils.DefaultTrustManager;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.File;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ComicCacher
{

    private final CacherConfig config;
    private final JsonConfigWriter statsUpdater;
    private final Logger logger = LoggerFactory.getLogger(ComicCacher.class);

    private final String cacheDirectory;

    public ComicCacher() throws NoSuchAlgorithmException, KeyManagementException
    {
        // configure the SSLContext with a TrustManager
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
        SSLContext.setDefault(ctx);

        File directory=new File(ComicApiApplication.getConfig().cacheDirectory);
        cacheDirectory = directory.exists() ? ComicApiApplication.getConfig().cacheDirectory : ComicApiApplication.getConfig().cacheDirectoryAlternate;
        logger.warn("Caching to {}", ComicApiApplication.getConfig().cacheDirectory);

        config = new CacherConfigLoader().load();
        statsUpdater = new JsonConfigWriter(cacheDirectory + "/comics.json");
    }

    public boolean cacheAll()
    {
        for (CacherConfig.GoComics dcc : config.dailyComics)
        {
            cacheComic(dcc);
        }
        return true;
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

        IDailyComic comics = new GoComics()
                .setCacheDirectory(cacheDirectory)
                .setComic(dcc.name)
                .setDate(dcc.startDate);

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

        return true;
    }
}
