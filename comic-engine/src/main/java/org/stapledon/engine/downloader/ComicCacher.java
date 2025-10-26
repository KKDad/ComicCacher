package org.stapledon.engine.downloader;

import org.springframework.stereotype.Component;
import org.stapledon.common.config.IComicsBootstrap;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.infrastructure.web.DefaultTrustManager;
import org.stapledon.common.util.Bootstrap;
import org.stapledon.engine.management.ManagementFacade;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Legacy component for comic caching operations.
 * Now delegates to ManagementFacade for actual comic operations.
 *
 * @deprecated This class is maintained for backward compatibility.
 *             New code should use ManagementFacade directly.
 */
@Deprecated(since = "1.2.0")
@Slf4j
@ToString
@Component
@RequiredArgsConstructor
public class ComicCacher {

    private final Bootstrap config;
    private final ManagementFacade comicManagementFacade;

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
     * @deprecated Use {@link ManagementFacade#updateAllComics()} instead
     */
    @Deprecated(since = "1.2.0")
    public boolean cacheAll() {
        log.debug("ComicCacher.cacheAll() delegating to ManagementFacade.updateAllComics()");
        return comicManagementFacade.updateAllComics();
    }

    /**
     * Cache a single comic from bootstrap config
     *
     * @param result Current result status
     * @param dcc Comic bootstrap configuration
     * @return Updated result status
     * @deprecated Use {@link ManagementFacade#updateComic(int)} instead
     */
    @Deprecated(since = "1.2.0")
    public boolean cacheSingle(boolean result, IComicsBootstrap dcc) {
        try {
            int comicId = dcc.stripName().hashCode();
            return comicManagementFacade.updateComic(comicId);
        } catch (Exception e) {
            log.error(String.format("Failed to cache %s : %s", dcc.stripName(), e.getMessage()), e);
            return false;
        }
    }

    /**
     * Cache all comics for a particular ComicItem.
     *
     * @param comic ComicItem to perform caching on
     * @return true if successful
     * @deprecated Use {@link ManagementFacade#updateComic(int)} instead
     */
    @Deprecated(since = "1.2.0")
    public boolean cacheSingle(ComicItem comic) {
        log.debug("ComicCacher.cacheSingle() delegating to ManagementFacade.updateComic()");
        return comicManagementFacade.updateComic(comic.getId());
    }

    /**
     * Given a ComicItem, locate the corresponding IComicsBootstrap from the Bootstrap configuration
     *
     * @param comic ComicItem to lookup
     * @return IComicsBootstrap or null if none could be located
     */
    IComicsBootstrap lookupGoComics(ComicItem comic) {
        return findBootstrapForComic(config, comic.getName());
    }
    
    /**
     * Finds a bootstrap configuration entry for the specified comic name.
     *
     * @param config The bootstrap configuration
     * @param comicName The name of the comic to look for
     * @return The IComicsBootstrap instance if found, null otherwise
     */
    private IComicsBootstrap findBootstrapForComic(Bootstrap config, String comicName) {
        // Check daily comics
        if (config.getDailyComics() != null) {
            for (IComicsBootstrap bootstrap : config.getDailyComics()) {
                if (bootstrap.stripName().equalsIgnoreCase(comicName)) {
                    return bootstrap;
                }
            }
        }
        
        // Check king comics
        if (config.getKingComics() != null) {
            for (IComicsBootstrap bootstrap : config.getKingComics()) {
                if (bootstrap.stripName().equalsIgnoreCase(comicName)) {
                    return bootstrap;
                }
            }
        }
        
        return null;
    }

}