package org.stapledon.cacher;

import org.stapledon.utils.DefaultTrustManager;
import org.stapledon.config.ComicCacherConfig;
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

public class ComicCacher {

    public static void main(String[] args) throws NoSuchAlgorithmException, KeyManagementException
    {
        final Logger logger = Logger.getLogger(ComicCacher.class);

        // configure the SSLContext with a TrustManager
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
        SSLContext.setDefault(ctx);

        ComicCacherConfig config = new CacherConfigLoader().load();
        JsonConfigWriter statsUpdater = new JsonConfigWriter(config.cacheDirectory + "/comics.json");

        for (ComicCacherConfig.GoComics dcc : config.dailyComics) {

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
        }
    }
}
