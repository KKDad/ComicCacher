package org.stapledon.core.comic.config;

import org.springframework.context.annotation.Configuration;
import org.stapledon.engine.downloader.ComicDownloaderStrategy;
import org.stapledon.engine.downloader.ComicsKingdomDownloaderStrategy;
import org.stapledon.engine.downloader.DownloaderFacade;
import org.stapledon.engine.downloader.FreefallDownloaderStrategy;
import org.stapledon.engine.downloader.GoComicsDownloaderStrategy;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class for registering comic downloader strategies.
 * <p>
 * Strategies use {@code @Component} for Spring lifecycle (dependency injection),
 * but must also be manually registered here because the {@link DownloaderFacade}
 * needs them keyed by source identifier, which Spring DI alone cannot provide.
 */
@Slf4j
@ToString
@Configuration
@RequiredArgsConstructor
public class ComicDownloaderConfig {

    private final DownloaderFacade downloaderFacade;
    private final GoComicsDownloaderStrategy goComicsStrategy;
    private final ComicsKingdomDownloaderStrategy comicsKingdomStrategy;
    private final FreefallDownloaderStrategy freefallStrategy;

    /**
     * Initializes the comic downloader configuration by registering all available
     * downloader strategies with the downloader facade.
     */
    @PostConstruct
    public void init() {
        log.info("Registering comic downloader strategies...");

        registerStrategy(goComicsStrategy);
        registerStrategy(comicsKingdomStrategy);
        registerStrategy(freefallStrategy);

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