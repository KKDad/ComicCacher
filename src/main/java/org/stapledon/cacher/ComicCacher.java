package org.stapledon.cacher;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;

public class ComicCacher {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, KeyManagementException {

        // configure the SSLContext with a TrustManager
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
        SSLContext.setDefault(ctx);


        GoComics comics = new GoComics()
                .setComic("Adam At Home")
                .setDate(LocalDate.of(2018, 01, 01))
                .ensureCacheDirectoryExists();

        while (!comics.advance().equals(comics.getLastStripOn()))
            comics.ensureCache();


        comics.ensureCache();
    }
}
