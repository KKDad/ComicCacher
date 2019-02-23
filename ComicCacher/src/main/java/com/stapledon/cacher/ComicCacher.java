package com.stapledon.cacher;

import com.stapledon.config.ComicCacherConfig;
import com.stapledon.config.JsonConfigLoader;
import com.stapledon.config.JsonConfigWriter;
import com.stapledon.interop.ComicItem;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ComicCacher {

    public static void main(String[] args) throws NoSuchAlgorithmException, KeyManagementException, InterruptedException {

        // configure the SSLContext with a TrustManager
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
        SSLContext.setDefault(ctx);

        ComicCacherConfig config = new JsonConfigLoader().load();
        JsonConfigWriter statsUpdater = new JsonConfigWriter(config.cacheDirectory + "/comics.json");

        for (ComicCacherConfig.GoComics dcc : config.dailyComics) {

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
            if (comicItem.description == null || comicItem.description.length() == 0)
                comicItem.description = comics.getComicDescription();


            while (!comics.advance().equals(comics.getLastStripOn()))
                comics.ensureCache();

            //comics.up
            comicItem.newest = comics.getLastStripOn();
            statsUpdater.save(comicItem);
        }
    }
}
