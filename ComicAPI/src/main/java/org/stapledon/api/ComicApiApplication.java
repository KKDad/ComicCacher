package org.stapledon.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.stapledon.config.Bootstrap;
import org.stapledon.config.IComicsBootstrap;
import org.stapledon.config.JsonConfigWriter;
import org.stapledon.downloader.ComicCacher;
import org.stapledon.downloader.DailyRunner;
import org.stapledon.dto.ComicConfig;
import org.stapledon.dto.ComicItem;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
@ComponentScan(basePackages = {"org.stapledon.config"})
public class ComicApiApplication {
    private final String cacheLocation;

    @PostConstruct
    public void PostConstructSetup() {
        log.info("ComicApiApplication starting...");

//		String dir = System.getenv("CACHE_DIRECTORY");
//		if (dir == null) {
//			log.error("CACHE_DIRECTORY not set. Defaulting to /comics");
//			dir = "/comics";
//		}

        var directory = new File(cacheLocation);
        if (!directory.exists() || directory.isDirectory()) {
            directory.mkdirs();
        }
        log.warn("Serving from {}", cacheLocation);

        try {
            var jsonConfigWriter = new JsonConfigWriter(cacheLocation + "/comics.json");
            var comicConfig = jsonConfigWriter.loadComics();
            reconcileBoostrapConfig(comicConfig);
            comicConfig = jsonConfigWriter.loadComics();

            ComicsServiceImpl.getComics().addAll(comicConfig.items.values());
            log.info("Loaded: {} comics.", ComicsServiceImpl.getComics().size());

        } catch (IOException fne) {
            log.error("Cannot load ComicList", fne);
        }

        // Ensure we cache comics once a day
        DailyRunner.ensureDailyCaching();
    }

    /**
     * Reconcile all the entries in the CacherBootstrapConfig with the ComicList
     * - New entries found in the CacherBootstrapConfig will be added and immediately cached
     * - Entries found in the ComicList, but not in the CacherBootstrapConfig will be removed
     */
    public void reconcileBoostrapConfig(ComicConfig comicConfig) {
        log.info("Begin Reconciliation of CacherBootstrapConfig and ComicConfig");
        try {
            var cacher = new ComicCacher();
            Bootstrap config = cacher.bootstrapConfig();

            // Check for New GoComics
            for (IComicsBootstrap daily : config.getDailyComics()) {
                var comic = findComicItem(comicConfig, daily);
                if (comic == null) {
                    if (log.isInfoEnabled())
                        log.info("Bootstrapping new DailyComic: {}", daily.stripName());
                    cacher.cacheSingle(true, daily);
                }
            }

            // Check for New KingFeatures
            for (IComicsBootstrap king : config.getKingComics()) {
                var comic = findComicItem(comicConfig, king);
                if (comic == null) {
                    if (log.isInfoEnabled())
                        log.info("Bootstrapping new KingFeatures: {}", king.stripName());
                    cacher.cacheSingle(true, king);
                }
            }

            // Removed entries found in ComicConfig, but not in the CacherBootstrapConfig
            comicConfig.items.entrySet().removeIf(integerComicItemEntry -> findBootstrapComic(config, integerComicItemEntry.getValue()) == null);


        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error(e.getMessage());
        }
        log.info("Reconciliation complete");
    }

    /**
     * Giving a ComicItem, Locate the corresponding IComicsBootstrap from the BootStrap configuration
     *
     * @param comic ComicItem to lookup
     * @return IComicsBootstrap or null if none could be located
     */
    IComicsBootstrap findBootstrapComic(Bootstrap config, ComicItem comic) {
        if (!config.getDailyComics().isEmpty()) {
            IComicsBootstrap dailyComics = config.getDailyComics().stream().filter(p -> p.name.equalsIgnoreCase(comic.name)).findFirst().orElse(null);
            if (dailyComics != null)
                return dailyComics;
        }
        if (!config.getKingComics().isEmpty()) {
            IComicsBootstrap kingComics = config.getKingComics().stream().filter(p -> p.name.equalsIgnoreCase(comic.name)).findFirst().orElse(null);
            if (kingComics != null)
                return kingComics;
        }
        if (log.isWarnEnabled())
            log.warn("{} was not found. Disabling", comic.name);
        return null;
    }

    /**
     * Giving a IComicsBootstrap, Locate the corresponding ComicItem from the BootStrap configuration
     *
     * @param config List<ComicConfig> to search in
     * @param comic  IComicsBootstrap to lookup
     * @return ComicItem or null if none could be located
     */
    ComicItem findComicItem(ComicConfig config, IComicsBootstrap comic) {

        if (!config.items.isEmpty()) {
            Map.Entry<Integer, ComicItem> result = config.items.entrySet()
                    .stream()
                    .filter(p -> p.getValue().name.equalsIgnoreCase(comic.stripName()))
                    .findFirst()
                    .orElse(null);
            if (result != null)
                return result.getValue();
        }
        return null;
    }


    public static void main(String[] args) {
        SpringApplication.run(ComicApiApplication.class, args);
    }

}

