package org.stapledon.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.stapledon.config.IComicsBootstrap;
import org.stapledon.config.JsonConfigManager;
import org.stapledon.config.properties.StartupConfig;
import org.stapledon.downloader.ComicCacher;
import org.stapledon.dto.Bootstrap;
import org.stapledon.dto.ComicConfig;
import org.stapledon.dto.ComicItem;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JsonBootstrapper implements Bootstrapper, CommandLineRunner {

    private final StartupConfig startupConfig;
    private final JsonConfigManager jsonConfigManager;
    private final ComicCacher comicCacher;
    private final ComicsService comicsService;

    @Override
    public boolean reconcile() {
        log.info("ComicApiApplication starting...");
        try {
            var comicConfig = jsonConfigManager.loadComics();
            reconcileBoostrapConfig(comicConfig);
            comicConfig = jsonConfigManager.loadComics();

            comicsService.getComics().addAll(comicConfig.getItems().values());
            log.info("Loaded: {} comics.", comicsService.getComics().size());
            return true;

        } catch (IOException fne) {
            log.error("Cannot load ComicList", fne);
        }
        return false;
    }

    /**
     * Reconcile all the entries in the BootstrapConfig with the ComicList
     * - New entries found in the BootstrapConfig will be added and immediately cached
     * - Entries found in the ComicList, but not in the BootstrapConfig will be removed
     */
    public void reconcileBoostrapConfig(ComicConfig comicConfig) {
        log.info("Begin Reconciliation of CacherBootstrapConfig and ComicConfig");
        Bootstrap config = comicCacher.bootstrapConfig();

        // Check for New GoComics
        for (IComicsBootstrap daily : config.getDailyComics()) {
            var comic = findComicItem(comicConfig, daily);
            if (comic == null) {
                if (log.isInfoEnabled())
                    log.info("Bootstrapping new DailyComic: {}", daily.stripName());
                comicCacher.cacheSingle(true, daily);
            }
        }

        // Check for New KingFeatures
        for (IComicsBootstrap king : config.getKingComics()) {
            var comic = findComicItem(comicConfig, king);
            if (comic == null) {
                if (log.isInfoEnabled())
                    log.info("Bootstrapping new KingFeatures: {}", king.stripName());
                comicCacher.cacheSingle(true, king);
            }
        }

        // Removed entries found in ComicConfig, but not in the CacherBootstrapConfig
        comicConfig.getItems().entrySet().removeIf(integerComicItemEntry -> findBootstrapComic(config, integerComicItemEntry.getValue()) == null);

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
            IComicsBootstrap dailyComics = config.getDailyComics().stream().filter(p -> p.getName().equalsIgnoreCase(comic.getName())).findFirst().orElse(null);
            if (dailyComics != null)
                return dailyComics;
        }
        if (!config.getKingComics().isEmpty()) {
            IComicsBootstrap kingComics = config.getKingComics().stream().filter(p -> p.getName().equalsIgnoreCase(comic.getName())).findFirst().orElse(null);
            if (kingComics != null)
                return kingComics;
        }
        if (log.isWarnEnabled())
            log.warn("{} was not found. Disabling", comic.getName());
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

        if (!config.getItems().isEmpty()) {
            Map.Entry<Integer, ComicItem> result = config.getItems().entrySet()
                    .stream()
                    .filter(p -> p.getValue().getName().equalsIgnoreCase(comic.stripName()))
                    .findFirst()
                    .orElse(null);
            if (result != null)
                return result.getValue();
        }
        return null;
    }

    @Override
    public void run(String... args) {
        if (startupConfig.isEnabled()) {
            log.info("JsonBootstrapper is starting ...");
            reconcile();
        } else {
            log.warn("JsonBootstrapper is disabled");
        }
    }
}
