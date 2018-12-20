package org.stapledon.cacher;

import org.stapledon.config.ComicCacherConfig;
import org.stapledon.config.DailyComicConfig;
import org.stapledon.config.YamlConfigLoader;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ComicCacher {

    public static void main(String[] args) throws NoSuchAlgorithmException, KeyManagementException {

        // configure the SSLContext with a TrustManager
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
        SSLContext.setDefault(ctx);

        ComicCacherConfig config = new YamlConfigLoader().load();

        for (DailyComicConfig dcc : config.getDailyComics()) {
            GoComics comics = new GoComics()
                    .setComic(dcc.getName())
                    .setDate(dcc.getStartDate())
                    .ensureCacheDirectoryExists();

            while (!comics.advance().equals(comics.getLastStripOn()))
                comics.ensureCache();
        }
    }
}
