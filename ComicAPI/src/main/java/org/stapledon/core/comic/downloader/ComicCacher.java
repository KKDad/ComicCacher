package org.stapledon.core.comic.downloader;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.stapledon.infrastructure.caching.ICachable;
import org.stapledon.infrastructure.caching.ImageCacheStatsUpdater;
import org.stapledon.infrastructure.config.CacheConfiguration;
import org.stapledon.infrastructure.config.IComicsBootstrap;
import org.stapledon.infrastructure.config.JsonConfigWriter;
import org.stapledon.common.util.Bootstrap;
import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.infrastructure.web.DefaultTrustManager;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComicCacher {

    private final Bootstrap config;
    private final JsonConfigWriter statsUpdater;

    private final CacheConfiguration cacheConfiguration;

    @PostConstruct
    public void setup() {
        // configure the SSLContext with a TrustManager
        try {
            var ctx = SSLContext.getInstance("TLSv1.2");
            ctx.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());
            SSLContext.setDefault(ctx);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            // Ignore - Powermock issue during unit tests?
        }
        log.info("BootStrapConfig - Loaded {} dailyComics comics, {} kingComics comics.", config.getDailyComics().size(), config.getKingComics().size());
    }

    public Bootstrap bootstrapConfig() {
        return this.config;
    }

    /**
     * Attempt to cache all the configured comics. Does not stop if one or more comics fail to be cached.
     *
     * @return True if all comics where successfully cached, false if one or more fail to be cached.
     */
    public boolean cacheAll() {
        // Combine both comic sources and process using streams
        return Stream.concat(
                config.getDailyComics().stream(),
                config.getKingComics().stream()
            )
            .map(this::cacheSingleComic)
            .reduce(true, Boolean::logicalAnd);
    }

    /**
     * Cache a single comic, handling exceptions
     *
     * @param dcc Comic bootstrap configuration
     * @return true if successful, false if failed
     */
    private boolean cacheSingleComic(IComicsBootstrap dcc) {
        try {
            return cacheComic(dcc);
        } catch (Exception e) {
            log.error("Failed to cache {} : {}", dcc.stripName(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Cache a single comic from bootstrap config
     *
     * @param result Current result status
     * @param dcc Comic bootstrap configuration
     * @return Updated result status
     */
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
        return Optional.ofNullable(lookupGoComics(comic))
                .map(this::cacheSingleComic)
                .orElse(false);
    }

    /**
     * Given a ComicItem, locate the corresponding IComicsBootstrap from the Bootstrap configuration
     *
     * @param comic ComicItem to lookup
     * @return IComicsBootstrap or null if none could be located
     */
    IComicsBootstrap lookupGoComics(ComicItem comic) {
        // Check daily comics first, then king comics, using a more streamlined approach
        return Stream.of(
                    config.getDailyComics().stream(),
                    config.getKingComics().stream()
                )
                .flatMap(stream -> stream)
                .filter(bootstrap -> bootstrap.stripName().equalsIgnoreCase(comic.getName()))
                .findFirst()
                .orElse(null);
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
                .setCacheRoot(cacheConfiguration.cacheLocation())
                .setComic(dcc.stripName())
                .setDate(startDate);

        var comicItem = statsUpdater.fetch(dcc.stripName());
        if (comicItem == null) {
            comicItem = ComicItem.builder()
                    .id(dcc.stripName().hashCode())
                    .name(dcc.stripName())
                    .oldest(dcc.startDate())
                    .build();
        }
        comics.updateComicMetadata(comicItem);
        while (!comics.advance().equals(comics.getLastStripOn()))
            comics.ensureCache();

        comicItem.setNewest(comics.getLastStripOn());
        comicItem.setEnabled(true);
        statsUpdater.save(comicItem);

        // Update statistics about the cached images
        var cache = new ImageCacheStatsUpdater(((ICachable) comics).cacheLocation(), statsUpdater);
        cache.updateStats();

        return true;
    }
}
