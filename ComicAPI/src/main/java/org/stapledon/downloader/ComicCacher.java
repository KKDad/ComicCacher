package org.stapledon.downloader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.stapledon.caching.ICachable;
import org.stapledon.caching.ImageCacheStatsUpdater;
import org.stapledon.config.*;
import org.stapledon.dto.ComicItem;
import org.stapledon.web.DefaultTrustManager;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComicCacher {

    private final Bootstrap config;
    private final JsonConfigWriter statsUpdater;
    private final String cacheDirectory;

    public ComicCacher() throws NoSuchAlgorithmException, KeyManagementException {
        // configure the SSLContext with a TrustManager
        try {
            var ctx = SSLContext.getInstance("TLSv1.2");
            ctx.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());
            SSLContext.setDefault(ctx);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            // Ignore - Powermock issue during unit tests?
        }

        String directory = System.getenv("CACHE_DIRECTORY");
        if (directory == null) {
            log.error("CACHE_DIRECTORY not set. Defaulting to /comics");
            directory = "/comics";
        }
        this.cacheDirectory = directory;
        log.warn("Caching to {}", this.cacheDirectory);

        config = new CacherConfigLoader(new GsonProvider().gson()).load();
        log.info("BootStrapConfig - Loaded {} dailyComics comics, {} kingComics comics.", config.getDailyComics().size(), config.getKingComics().size());
        statsUpdater = new JsonConfigWriter(this.cacheDirectory + "/comics.json");
    }

    public Bootstrap bootstrapConfig() {
        return this.config;
    }

    /**
     * Attempt to cache all of the configured comics. Does not stop if one or more comics fail to be cached.
     *
     * @return True if all comics where successfully cached, false if one or more fail to be cached.
     */
    public boolean cacheAll() {
        var result = true;
        for (IComicsBootstrap dcc : config.getDailyComics())
            result = cacheSingle(result, dcc);
        for (IComicsBootstrap dcc : config.getKingComics())
            result = cacheSingle(result, dcc);
        return result;
    }

    public boolean cacheSingle(boolean result, IComicsBootstrap dcc) {
        try {
            cacheComic(dcc);
        } catch (Exception e) {
            log.error(String.format("Failed to cache %s : %s", dcc.stripName(), e.getMessage()), e);
            result = false;
        }
        return result;
    }

    /**
     * Cache all comics for a particular ComicItem.
     *
     * @param comic ComicItem to perform caching on
     * @return true if successful
     */
    public boolean cacheSingle(ComicItem comic) {
        IComicsBootstrap dailyComic = lookupGoComics(comic);
        if (dailyComic != null)
            return cacheComic(dailyComic);

        return false;
    }

    /**
     * Giving a ComicItem, Locate the corresponding IComicsBootstrap from the BootStrap configuration
     *
     * @param comic ComicItem to lookup
     * @return IComicsBootstrap or null if none could be located
     */
    IComicsBootstrap lookupGoComics(ComicItem comic) {
        if (!config.getDailyComics().isEmpty()) {
            IComicsBootstrap dailyComics = config.getDailyComics().stream().filter(p -> p.name.equalsIgnoreCase(comic.name)).findFirst().orElse(null);
            if (dailyComics != null)
                return dailyComics;
        }
        if (!config.getKingComics().isEmpty()) {
            return config.getKingComics().stream().filter(p -> p.name.equalsIgnoreCase(comic.name)).findFirst().orElse(null);
        }
        return null;
    }


    private boolean cacheComic(IComicsBootstrap dcc) {
        if (log.isInfoEnabled()) {
            log.info("***********************************************************************************************");
            log.info("Processing: {}", dcc.stripName());
            log.info("***********************************************************************************************");
        }

        // Only search back 7 days unless we are refilling
        LocalDate startDate = dcc.startDate();
        if (startDate.equals(LocalDate.of(2019, 4, 1)))
            startDate = LocalDate.now().minusDays(7);

        IDailyComic comics = dcc.getDownloader()
                .setCacheRoot(cacheDirectory)
                .setComic(dcc.stripName())
                .setDate(startDate);

        var comicItem = statsUpdater.fetch(dcc.stripName());
        if (comicItem == null) {
            comicItem = new ComicItem();
            comicItem.id = dcc.stripName().hashCode();
            comicItem.name = dcc.stripName();
            comicItem.oldest = dcc.startDate();
        }
        comics.updateComicMetadata(comicItem);
        while (!comics.advance().equals(comics.getLastStripOn()))
            comics.ensureCache();

        comicItem.newest = comics.getLastStripOn();
        comicItem.enabled = true;
        statsUpdater.save(comicItem);

        // Update statistics about the cached images
        var cache = new ImageCacheStatsUpdater(((ICachable) comics).cacheLocation(), statsUpdater);
        cache.updateStats();

        return true;
    }
}
