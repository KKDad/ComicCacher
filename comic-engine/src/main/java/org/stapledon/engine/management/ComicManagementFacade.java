package org.stapledon.engine.management;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.service.ComicConfigurationService;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.common.service.RetrievalStatusService;
import org.stapledon.common.util.Direction;
import org.stapledon.engine.downloader.DownloaderFacade;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the ManagementFacade interface.
 * Acts as the central coordinator between all other facades in the application.
 */
@Slf4j
@ToString
@Component
public class ComicManagementFacade implements ManagementFacade {

    private final ComicStorageFacade storageFacade;
    private final ComicConfigurationService configFacade;
    private final DownloaderFacade downloaderFacade;
    private final RetrievalStatusService retrievalStatusService;

    // Thread-safe collection for comics
    private final Map<Integer, ComicItem> comics = new ConcurrentHashMap<>();

    public ComicManagementFacade(
            ComicStorageFacade storageFacade,
            ComicConfigurationService configFacade,
            DownloaderFacade downloaderFacade,
            RetrievalStatusService retrievalStatusService) {
        this.storageFacade = storageFacade;
        this.configFacade = configFacade;
        this.downloaderFacade = downloaderFacade;
        this.retrievalStatusService = retrievalStatusService;

        // Load comics from configuration
        refreshComicList();

        log.info("Comic configuration loaded.");
    }

    @Override
    @Cacheable(value = "comicMetadata", key = "'allComics'")
    public List<ComicItem> getAllComics() {
        List<ComicItem> result = new ArrayList<>(comics.values());
        Collections.sort(result);
        return result;
    }

    @Override
    @Cacheable(value = "comicMetadata", key = "'comic:' + #comicId")
    public Optional<ComicItem> getComic(int comicId) {
        return Optional.ofNullable(comics.get(comicId));
    }

    @Override
    public Optional<ComicItem> getComicByName(String comicName) {
        if (comicName == null) {
            return Optional.empty();
        }
        
        return comics.values().stream()
                .filter(comic -> {
                    String name = comic.getName();
                    return name != null && name.equalsIgnoreCase(comicName);
                })
                .findFirst();
    }

    @Override
    @CacheEvict(value = "comicMetadata", key = "'allComics'")
    public Optional<ComicItem> createComic(ComicItem comicItem) {
        // Don't create if already exists
        if (comics.containsKey(comicItem.getId())) {
            return Optional.empty();
        }

        comics.put(comicItem.getId(), comicItem);

        // Save to configuration
        ComicConfig config = configFacade.loadComicConfig();
        config.getItems().put(comicItem.getId(), comicItem);
        configFacade.saveComicConfig(config);

        return Optional.of(comicItem);
    }

    @Override
    @CacheEvict(value = "comicMetadata", key = "'comic:' + #comicId")
    public Optional<ComicItem> updateComic(int comicId, ComicItem comicItem) {
        // Ensure ID in comics matches the request ID
        if (comicItem.getId() != comicId) {
            comicItem = ComicItem.builder()
                    .id(comicId)
                    .name(comicItem.getName())
                    .description(comicItem.getDescription())
                    .author(comicItem.getAuthor())
                    .avatarAvailable(comicItem.isAvatarAvailable())
                    .enabled(comicItem.isEnabled())
                    .newest(comicItem.getNewest())
                    .oldest(comicItem.getOldest())
                    .source(comicItem.getSource())
                    .sourceIdentifier(comicItem.getSourceIdentifier())
                    .build();
        }
        
        comics.put(comicId, comicItem);
        
        // Save to configuration
        ComicConfig config = configFacade.loadComicConfig();
        config.getItems().put(comicId, comicItem);
        configFacade.saveComicConfig(config);
        
        return Optional.of(comicItem);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "comicMetadata", allEntries = true),
        @CacheEvict(value = "comicNavigation", allEntries = true),
        @CacheEvict(value = "boundaryDates", allEntries = true),
        @CacheEvict(value = "navigationDates", allEntries = true)
    })
    public boolean deleteComic(int comicId) {
        ComicItem removed = comics.remove(comicId);

        if (removed != null) {
            // Also remove from storage and configuration
            storageFacade.deleteComic(comicId, removed.getName());

            ComicConfig config = configFacade.loadComicConfig();
            config.getItems().remove(comicId);
            configFacade.saveComicConfig(config);

            return true;
        }

        return false;
    }

    @Override
    public ComicNavigationResult getComicStrip(int comicId, Direction direction) {
        Optional<ComicItem> comicOpt = getComic(comicId);
        if (comicOpt.isEmpty()) {
            return ComicNavigationResult.notFound("NO_COMICS_AVAILABLE", null, null, null);
        }

        ComicItem comic = comicOpt.get();

        try {
            // Get the date based on direction
            LocalDate date;
            if (direction == Direction.FORWARD) {
                Optional<LocalDate> oldestDate = storageFacade.getOldestDateWithComic(comicId, comic.getName());
                if (oldestDate.isEmpty()) {
                    return ComicNavigationResult.notFound("NO_COMICS_AVAILABLE", null, null, null);
                }
                date = oldestDate.get();
            } else {
                Optional<LocalDate> newestDate = storageFacade.getNewestDateWithComic(comicId, comic.getName());
                if (newestDate.isEmpty()) {
                    return ComicNavigationResult.notFound("NO_COMICS_AVAILABLE", null, null, null);
                }
                date = newestDate.get();
            }

            // Get the image for the date
            Optional<ImageDto> imageOpt = storageFacade.getComicStrip(comicId, comic.getName(), date);
            if (imageOpt.isEmpty()) {
                return ComicNavigationResult.notFound("NO_COMICS_AVAILABLE", date, null, null);
            }

            // Get boundary dates for navigation hints
            LocalDate nearestPrev = storageFacade.getPreviousDateWithComic(comicId, comic.getName(), date).orElse(null);
            LocalDate nearestNext = storageFacade.getNextDateWithComic(comicId, comic.getName(), date).orElse(null);

            return ComicNavigationResult.found(imageOpt.get(), nearestPrev, nearestNext);
        } catch (Exception e) {
            log.error("Error retrieving comic strip: {}", e.getMessage(), e);
            return ComicNavigationResult.notFound("ERROR", null, null, null);
        }
    }

    @Override
    public ComicNavigationResult getComicStrip(int comicId, Direction direction, LocalDate from) {
        Optional<ComicItem> comicOpt = getComic(comicId);
        if (comicOpt.isEmpty()) {
            return ComicNavigationResult.notFound("NO_COMICS_AVAILABLE", from, null, null);
        }

        ComicItem comic = comicOpt.get();

        // Delegate to the cached implementation with comic name
        return getComicStripCached(comicId, comic.getName(), direction, from);
    }

    /**
     * Cached implementation of getComicStrip that includes comic name in the cache key.
     * This ensures that each comic has its own cache entries.
     */
    @Cacheable(value = "comicNavigation", key = "#comicId + ':' + #comicName + ':' + #from + ':' + #direction")
    private ComicNavigationResult getComicStripCached(int comicId, String comicName, Direction direction, LocalDate from) {
        log.info("getComicStrip: comicId={}, comicName={}, direction={}, from={}", comicId, comicName, direction, from);

        try {
            // Get the next/previous date
            Optional<LocalDate> dateOpt;
            if (direction == Direction.FORWARD) {
                dateOpt = storageFacade.getNextDateWithComic(comicId, comicName, from);
                log.info("getNextDateWithComic returned: {} (from: {})", dateOpt.orElse(null), from);
            } else {
                dateOpt = storageFacade.getPreviousDateWithComic(comicId, comicName, from);
                log.info("getPreviousDateWithComic returned: {} (from: {})", dateOpt.orElse(null), from);
            }

            if (dateOpt.isEmpty()) {
                log.info("No comic found going {} from {}, reason={}", direction, from,
                        direction == Direction.FORWARD ? "AT_END" : "AT_BEGINNING");

                // Determine if we're at the beginning or end
                String reason;
                if (direction == Direction.FORWARD) {
                    reason = "AT_END";
                } else {
                    reason = "AT_BEGINNING";
                }

                // Get the nearest dates for navigation hints
                LocalDate nearestPrev = storageFacade.getPreviousDateWithComic(comicId, comicName, from).orElse(null);
                LocalDate nearestNext = storageFacade.getNextDateWithComic(comicId, comicName, from).orElse(null);

                log.info("Returning navigation result: found=false, currentDate=null, nearestPrev={}, nearestNext={}",
                        nearestPrev, nearestNext);
                return ComicNavigationResult.notFound(reason, from, nearestPrev, nearestNext);
            }

            // Get the image for the found date
            LocalDate targetDate = dateOpt.get();
            log.info("Found comic at {}, loading image and calculating boundaries...", targetDate);
            Optional<ImageDto> imageOpt = storageFacade.getComicStrip(comicId, comicName, targetDate);

            if (imageOpt.isEmpty()) {
                log.warn("Image date exists but image couldn't be loaded for {} on {}", comicName, targetDate);
                // Image date exists but image couldn't be loaded
                return ComicNavigationResult.notFound("ERROR", targetDate, null, null);
            }

            // Get boundary dates for navigation hints
            LocalDate nearestPrev = storageFacade.getPreviousDateWithComic(comicId, comicName, targetDate).orElse(null);
            LocalDate nearestNext = storageFacade.getNextDateWithComic(comicId, comicName, targetDate).orElse(null);

            log.info("Returning navigation result: found=true, currentDate={}, nearestPrev={}, nearestNext={}",
                    targetDate, nearestPrev, nearestNext);
            return ComicNavigationResult.found(imageOpt.get(), nearestPrev, nearestNext);
        } catch (Exception e) {
            log.error("Error retrieving comic strip: {}", e.getMessage(), e);
            return ComicNavigationResult.notFound("ERROR", from, null, null);
        }
    }

    @Override
    public Optional<ImageDto> getComicStripOnDate(int comicId, LocalDate date) {
        return getComic(comicId)
                .flatMap(comic -> storageFacade.getComicStrip(comicId, comic.getName(), date));
    }

    @Override
    public Optional<ImageDto> getComicStripOnDate(String comicName, LocalDate date) {
        return getComicByName(comicName)
                .flatMap(comic -> storageFacade.getComicStrip(comic.getId(), comic.getName(), date));
    }

    @Override
    public Optional<ImageDto> getAvatar(int comicId) {
        return getComic(comicId)
                .flatMap(comic -> storageFacade.getAvatar(comicId, comic.getName()));
    }

    @Override
    public Optional<ImageDto> getAvatar(String comicName) {
        return getComicByName(comicName)
                .flatMap(comic -> storageFacade.getAvatar(comic.getId(), comic.getName()));
    }

    @Override
    public boolean updateAllComics() {
        try {
            updateComicsForDate(LocalDate.now());
            return true;
        } catch (Exception e) {
            log.error("Error occurred while updating all comics", e);
            return true;
        }
    }

    @Override
    public List<ComicDownloadResult> updateComicsForDate(LocalDate date) {
        List<ComicDownloadResult> results = new ArrayList<>();

        try {
            // Log if attempting to download future dates
            if (date.isAfter(LocalDate.now())) {
                log.warn("⚠️ FUTURE DATE DETECTED: Attempting to download comics for {} which is AFTER today ({})",
                        date, LocalDate.now());
            } else {
                log.info("Downloading comics for date: {} (today: {})", date, LocalDate.now());
            }

            // Get the current comic configuration
            ComicConfig config = configFacade.loadComicConfig();

            if (config == null || config.getComics() == null) {
                log.warn("Comic configuration is null or empty");
                return results;
            }

            DayOfWeek dayOfWeek = date.getDayOfWeek();

            // Process each comic individually - check if exists before downloading
            for (ComicItem comic : config.getComics()) {
                // Skip comics with null or empty source
                if (comic.getSource() == null || comic.getSource().isEmpty()) {
                    log.warn("Skipping comic '{}' - has null or empty source", comic.getName());
                    continue;
                }

                // Skip inactive comics (discontinued/delisted)
                if (!comic.isActive()) {
                    log.info("Skipping comic '{}' - comic is inactive/discontinued", comic.getName());
                    continue;
                }

                // Skip if comic doesn't publish on this day
                if (comic.getPublicationDays() != null && !comic.getPublicationDays().isEmpty()) {
                    if (!comic.getPublicationDays().contains(dayOfWeek)) {
                        log.info("Skipping comic '{}' - not published on {} (publishes: {})",
                                comic.getName(), dayOfWeek, comic.getPublicationDays());
                        continue;
                    }
                }

                // Check if comic already exists on disk
                if (storageFacade.comicStripExists(comic.getId(), comic.getName(), date)) {
                    log.info("Skipping comic '{}' for {} - already cached", comic.getName(), date);
                    continue;
                }

                // Comic doesn't exist - download it
                ComicDownloadRequest request = ComicDownloadRequest.builder()
                        .comicId(comic.getId())
                        .comicName(comic.getName())
                        .source(comic.getSource())
                        .sourceIdentifier(comic.getSourceIdentifier())
                        .date(date)
                        .build();

                ComicDownloadResult result = downloaderFacade.downloadComic(request);
                results.add(result);

                if (result.isSuccessful()) {
                    // Save the comic to storage
                    boolean saved = storageFacade.saveComicStrip(
                            comic.getId(),
                            comic.getName(),
                            date,
                            result.getImageData());

                    if (!saved) {
                        log.error("Failed to save comic {} to storage", comic.getName());
                        continue;
                    }

                    // Update comic item metadata
                    ComicItem updated = ComicItem.builder()
                            .id(comic.getId())
                            .name(comic.getName())
                            .description(comic.getDescription())
                            .author(comic.getAuthor())
                            .avatarAvailable(comic.isAvatarAvailable())
                            .enabled(comic.isEnabled())
                            .newest(date) // Update newest date
                            .oldest(comic.getOldest())
                            .source(comic.getSource())
                            .sourceIdentifier(comic.getSourceIdentifier())
                            .build();

                    updateComic(comic.getId(), updated);
                } else {
                    log.error("Failed to download comic {}: {}",
                            comic.getName(), result.getErrorMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while updating comics for date {}", date, e);
        }

        return results;
    }

    @Override
    public boolean updateComic(int comicId) {
        // Check if comic exists, return false if not
        Optional<ComicItem> comicOpt = getComic(comicId);
        if (comicOpt.isEmpty()) {
            log.warn("Comic with ID {} not found, cannot update", comicId);
            return false;
        }
        
        try {
            ComicItem comic = comicOpt.get();
            // Create download request
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
                boolean saved = storageFacade.saveComicStrip(
                        comic.getId(),
                        comic.getName(),
                        request.getDate(),
                        result.getImageData());
                
                if (saved) {
                    // Update comic item metadata
                    ComicItem updated = ComicItem.builder()
                            .id(comic.getId())
                            .name(comic.getName())
                            .description(comic.getDescription())
                            .author(comic.getAuthor())
                            .avatarAvailable(comic.isAvatarAvailable())
                            .enabled(comic.isEnabled())
                            .newest(request.getDate()) // Update newest date
                            .oldest(comic.getOldest())
                            .source(comic.getSource())
                            .sourceIdentifier(comic.getSourceIdentifier())
                            .build();
                    
                    updateComic(comic.getId(), updated);
                } else {
                    log.error("Failed to save comic {} to storage", comic.getName());
                }
            } else {
                log.error("Failed to download comic {}: {}", 
                        comic.getName(), result.getErrorMessage());
            }
            
            // Always return true if the comic exists and we attempted to update it,
            // regardless of whether the update succeeded
            return true;
            
        } catch (Exception e) {
            log.error("Error occurred while updating comic with ID {}", comicId, e);
            // Still return true since we successfully triggered the update for an existing comic
            return true;
        }
    }

    @Override
    public boolean updateComic(String comicName) {
        return getComicByName(comicName)
                .map(comic -> updateComic(comic.getId()))
                .orElse(false);
    }

    @Override
    public void refreshComicList() {
        try {
            // Load comic configuration
            ComicConfig comicConfig = configFacade.loadComicConfig();
            
            // Clear and reload comics
            comics.clear();
            if (comicConfig.getItems() != null) {
                comics.putAll(comicConfig.getItems());
            }
            
            log.info("Refreshed comic list: loaded {} comics", comics.size());
        } catch (Exception e) {
            log.error("Error refreshing comic list: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean purgeOldImages(int daysToKeep) {
        boolean allSucceeded = true;
        
        for (ComicItem comic : comics.values()) {
            boolean success = storageFacade.purgeOldImages(comic.getId(), comic.getName(), daysToKeep);
            if (!success) {
                log.error("Failed to purge old images for comic {}", comic.getName());
                allSucceeded = false;
            }
        }
        
        return allSucceeded;
    }

    @Override
    public Optional<LocalDate> getNewestDateWithComic(int comicId) {
        return getComic(comicId)
                .flatMap(comic -> storageFacade.getNewestDateWithComic(comicId, comic.getName()));
    }

    @Override
    public Optional<LocalDate> getOldestDateWithComic(int comicId) {
        return getComic(comicId)
                .flatMap(comic -> storageFacade.getOldestDateWithComic(comicId, comic.getName()));
    }

    @Override
    public List<ComicRetrievalRecord> getRetrievalRecords(String comicName, int limit) {
        return retrievalStatusService.getRetrievalRecords(comicName, null, null, null, limit);
    }
    
    @Override
    public List<ComicRetrievalRecord> getRetrievalRecordsForDate(String comicName, LocalDate date) {
        return retrievalStatusService.getRetrievalRecords(comicName, null, date, date, 100);
    }
    
    @Override
    public List<ComicRetrievalRecord> getFilteredRetrievalRecords(
            String comicName, 
            ComicRetrievalStatus status, 
            LocalDate fromDate, 
            LocalDate toDate, 
            int limit) {
        return retrievalStatusService.getRetrievalRecords(comicName, status, fromDate, toDate, limit);
    }
    
    @Override
    public Map<String, Object> getRetrievalSummary(LocalDate fromDate, LocalDate toDate) {
        return retrievalStatusService.getRetrievalSummary(fromDate, toDate);
    }
    
    @Override
    public int purgeOldRetrievalRecords(int daysToKeep) {
        return retrievalStatusService.purgeOldRecords(daysToKeep);
    }
}