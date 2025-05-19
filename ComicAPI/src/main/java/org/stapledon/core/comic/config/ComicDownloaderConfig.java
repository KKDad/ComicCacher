package org.stapledon.core.comic.config;

import org.springframework.context.annotation.Configuration;
import org.stapledon.core.comic.downloader.ComicDownloaderFacade;
import org.stapledon.core.comic.downloader.ComicDownloaderStrategy;
import org.stapledon.core.comic.downloader.ComicsKingdomDownloaderStrategy;
import org.stapledon.core.comic.downloader.GoComicsDownloaderStrategy;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class for registering comic downloader strategies.
 * This class ensures that all available downloader strategies are registered
 * with the downloader facade during application startup.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ComicDownloaderConfig {

    private final ComicDownloaderFacade downloaderFacade;
    private final GoComicsDownloaderStrategy goComicsStrategy;
    private final ComicsKingdomDownloaderStrategy comicsKingdomStrategy;

    /**
     * Initializes the comic downloader configuration by registering all available
     * downloader strategies with the downloader facade.
     */
    @PostConstruct
    public void init() {
        log.info("Registering comic downloader strategies...");
        
        registerStrategy(goComicsStrategy);
        registerStrategy(comicsKingdomStrategy);
        
        log.info("Comic downloader strategies registered successfully");
    }
    
    /**
     * Registers a downloader strategy with the downloader facade.
     *
     * @param strategy The strategy to register
     */
    private void registerStrategy(ComicDownloaderStrategy strategy) {
        log.debug("Registering downloader strategy for source: {}", strategy.getSource());
        downloaderFacade.registerDownloaderStrategy(strategy.getSource(), strategy);
    }
}