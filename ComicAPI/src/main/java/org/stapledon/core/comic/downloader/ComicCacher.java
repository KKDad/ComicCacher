package org.stapledon.core.comic.downloader;

import org.springframework.stereotype.Component;
import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.common.util.Bootstrap;
import org.stapledon.core.comic.dto.ComicDownloadRequest;
import org.stapledon.core.comic.dto.ComicDownloadResult;
import org.stapledon.infrastructure.config.CacheConfiguration;
import org.stapledon.infrastructure.config.IComicsBootstrap;
import org.stapledon.infrastructure.config.JsonConfigWriter;
import org.stapledon.infrastructure.storage.ComicStorageFacade;
import org.stapledon.infrastructure.web.DefaultTrustManager;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComicCacher {

    private final Bootstrap config;
    private final JsonConfigWriter statsUpdater;
    private final CacheConfiguration cacheConfiguration;
    private final ComicStorageFacade storageFacade;
    private final ComicDownloaderFacade downloaderFacade;

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
        boolean result = true;
        List<ComicDownloadResult> downloadResults = downloaderFacade.downloadLatestComics(config.getComicConfig());

        for (ComicDownloadResult downloadResult : downloadResults) {
            ComicDownloadRequest request = downloadResult.getRequest();
            log.info("Processing: {}", request.getComicName());

            if (downloadResult.isSuccessful()) {
                // Save the comic to storage
                boolean savedSuccessfully = storageFacade.saveComicStrip(
                        request.getComicId(),
                        request.getComicName(),
                        request.getDate(),
                        downloadResult.getImageData()
                );

                if (!savedSuccessfully) {
                    log.error("Failed to save comic {} to storage", request.getComicName());
                    result = false;
                }

                // Update metadata in ComicItem
                updateComicItemMetadata(request.getComicName(), request.getComicId(), request.getDate());
            } else {
                log.error("Failed to download comic {}: {}", request.getComicName(), downloadResult.getErrorMessage());
                result = false;
            }
        }

        return result;
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
        // Create download request for the comic
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(comic.getId())
                .comicName(comic.getName())
                .source(comic.getSource())
                .sourceIdentifier(comic.getSourceIdentifier())
                .date(LocalDate.now())
                .build();

        // Download the comic
        ComicDownloadResult result = downloaderFacade.downloadComic(request);
        
        if (result.isSuccessful()) {
            // Save the comic to storage
            boolean savedSuccessfully = storageFacade.saveComicStrip(
                    comic.getId(),
                    comic.getName(),
                    request.getDate(),
                    result.getImageData()
            );
            
            if (savedSuccessfully) {
                // Update metadata in ComicItem
                updateComicItemMetadata(comic.getName(), comic.getId(), request.getDate());
                return true;
            } else {
                log.error("Failed to save comic {} to storage", comic.getName());
                return false;
            }
        } else {
            log.error("Failed to download comic {}: {}", comic.getName(), result.getErrorMessage());
            return false;
        }
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

    /**
     * Update the metadata for a comic item and save it
     *
     * @param comicName The name of the comic
     * @param comicId The ID of the comic
     * @param date The date of the latest comic
     */
    private void updateComicItemMetadata(String comicName, int comicId, LocalDate date) {
        // Get or create ComicItem
        var comicItem = statsUpdater.fetch(comicName);
        if (comicItem == null) {
            comicItem = ComicItem.builder()
                    .id(comicId)
                    .name(comicName)
                    .oldest(date)
                    .build();
        }
        
        // Download avatar if needed
        if (!comicItem.isAvatarAvailable()) {
            // Try to get source and sourceIdentifier from bootstrap config
            IComicsBootstrap bootstrap = findBootstrapForComic(config, comicName);
            
            if (bootstrap != null) {
                String source = bootstrap.getSource();
                String sourceIdentifier = bootstrap.getSourceIdentifier();
                
                Optional<byte[]> avatarData = downloaderFacade.downloadAvatar(
                        comicId, comicName, source, sourceIdentifier);
                
                if (avatarData.isPresent()) {
                    storageFacade.saveAvatar(comicId, comicName, avatarData.get());
                    comicItem.setAvatarAvailable(true);
                }
            }
        }
        
        // Update comic item metadata
        comicItem.setNewest(date);
        comicItem.setEnabled(true);
        
        // Save updated comic item
        statsUpdater.save(comicItem);
        
        // Update storage metrics
        long storageSize = storageFacade.getStorageSize(comicId, comicName);
        log.debug("Storage size for comic {}: {} bytes", comicName, storageSize);
    }

    /**
     * Legacy method to support transition to new facade pattern
     * This method will be deprecated in future versions
     */
    private boolean cacheComic(IComicsBootstrap dcc) {
        if (log.isInfoEnabled()) {
            log.info("***********************************************************************************************");
            log.info("Processing: {}", dcc.stripName());
            log.info("***********************************************************************************************");
        }

        // Only search back 7 days unless we are refilling
        LocalDate startDate = dcc.startDate();
        if (startDate.equals(LocalDate.of(2019, 4, 1))) {
            startDate = LocalDate.now().minusDays(7);
        }

        LocalDate endDate = LocalDate.now();
        LocalDate currentDate = startDate;
        
        boolean result = true;
        while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
            // Create download request
            ComicDownloadRequest request = ComicDownloadRequest.builder()
                    .comicId(dcc.stripName().hashCode())
                    .comicName(dcc.stripName())
                    .source(dcc.getSource())
                    .sourceIdentifier(dcc.getSourceIdentifier())
                    .date(currentDate)
                    .build();
            
            // Download the comic
            ComicDownloadResult downloadResult = downloaderFacade.downloadComic(request);
            
            if (downloadResult.isSuccessful()) {
                // Save the comic to storage
                boolean savedSuccessfully = storageFacade.saveComicStrip(
                        request.getComicId(),
                        request.getComicName(),
                        request.getDate(),
                        downloadResult.getImageData()
                );
                
                if (!savedSuccessfully) {
                    log.error("Failed to save comic {} for date {} to storage", 
                            request.getComicName(), request.getDate());
                    result = false;
                }
            } else {
                log.error("Failed to download comic {} for date {}: {}", 
                        request.getComicName(), request.getDate(), downloadResult.getErrorMessage());
                result = false;
            }
            
            // Move to next date
            currentDate = currentDate.plusDays(1);
        }
        
        // Update metadata in ComicItem
        updateComicItemMetadata(dcc.stripName(), dcc.stripName().hashCode(), endDate);
        
        return result;
    }
}