package org.stapledon.downloader;

import org.stapledon.utils.DefaultTrustManager;
import org.stapledon.config.CacherConfig;
import org.stapledon.config.CacherConfigLoader;
import org.stapledon.config.JsonConfigWriter;
import org.stapledon.dto.ComicItem;
import org.apache.log4j.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ComicCacher
{

    private final CacherConfig config;
    private final JsonConfigWriter statsUpdater;
    private final Logger logger = Logger.getLogger(ComicCacher.class);

    public ComicCacher() throws NoSuchAlgorithmException, KeyManagementException
    {
        // configure the SSLContext with a TrustManager
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
        SSLContext.setDefault(ctx);

        config = new CacherConfigLoader().load();
        statsUpdater = new JsonConfigWriter(config.cacheDirectory + "/comics.json");
    }


    public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException
    {
        ComicCacher program = new ComicCacher();
        program.cacheAll();
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
        logger.info("Processing: " + dcc.name);
        logger.info("***********************************************************************************************");

        IDailyComic comics = new GoComics()
                .setCacheDirectory(config.cacheDirectory)
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
