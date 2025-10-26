package org.stapledon.engine.management;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import org.stapledon.common.config.IComicsBootstrap;
import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.infrastructure.config.TaskExecutionTracker;
import org.stapledon.common.service.ComicConfigurationService;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.common.service.RetrievalStatusService;
import org.stapledon.common.util.Bootstrap;
import org.stapledon.common.util.Direction;
import org.stapledon.engine.downloader.ComicDownloaderFacade;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the ComicManagementFacade interface.
 * Acts as the central coordinator between all other facades in the application.
 */
@Slf4j
@ToString
@Component
public class ComicManagementFacadeImpl implements ComicManagementFacade {

    private static final String TASK_NAME = "ComicReconciliation";

    private final ComicStorageFacade storageFacade;
    private final ComicConfigurationService configFacade;
    private final ComicDownloaderFacade downloaderFacade;
    private final TaskExecutionTracker taskExecutionTracker;
    private final RetrievalStatusService retrievalStatusService;

    // Thread-safe collection for comics
    private final Map<Integer, ComicItem> comics = new ConcurrentHashMap<>();

    public ComicManagementFacadeImpl(
            ComicStorageFacade storageFacade,
            ComicConfigurationService configFacade,
            ComicDownloaderFacade downloaderFacade,
            TaskExecutionTracker taskExecutionTracker,
            RetrievalStatusService retrievalStatusService) {
        this.storageFacade = storageFacade;
        this.configFacade = configFacade;
        this.downloaderFacade = downloaderFacade;
        this.taskExecutionTracker = taskExecutionTracker;
        this.retrievalStatusService = retrievalStatusService;

        // Load comics from configuration
        refreshComicList();

        // NOTE: Reconciliation scheduling is now handled by Spring Batch (ComicReconciliationJobScheduler)
        // The old ScheduledExecutorService approach has been replaced with Spring Batch jobs
        log.info("Comic configuration loaded. Reconciliation will be handled by Spring Batch scheduler.");
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
    @Cacheable(value = "comicNavigation", key = "#comicId + ':' + #from + ':' + #direction")
    public ComicNavigationResult getComicStrip(int comicId, Direction direction, LocalDate from) {
        Optional<ComicItem> comicOpt = getComic(comicId);
        if (comicOpt.isEmpty()) {
            return ComicNavigationResult.notFound("NO_COMICS_AVAILABLE", from, null, null);
        }

        ComicItem comic = comicOpt.get();

        try {
            // Get the next/previous date
            Optional<LocalDate> dateOpt;
            if (direction == Direction.FORWARD) {
                dateOpt = storageFacade.getNextDateWithComic(comicId, comic.getName(), from);
            } else {
                dateOpt = storageFacade.getPreviousDateWithComic(comicId, comic.getName(), from);
            }

            if (dateOpt.isEmpty()) {
                log.debug("No comic found for {} in direction {} from {}",
                        comic.getName(), direction, from);

                // Determine if we're at the beginning or end
                String reason;
                if (direction == Direction.FORWARD) {
                    reason = "AT_END";
                } else {
                    reason = "AT_BEGINNING";
                }

                // Get the nearest dates for navigation hints
                LocalDate nearestPrev = storageFacade.getPreviousDateWithComic(comicId, comic.getName(), from).orElse(null);
                LocalDate nearestNext = storageFacade.getNextDateWithComic(comicId, comic.getName(), from).orElse(null);

                return ComicNavigationResult.notFound(reason, from, nearestPrev, nearestNext);
            }

            // Get the image for the found date
            LocalDate targetDate = dateOpt.get();
            Optional<ImageDto> imageOpt = storageFacade.getComicStrip(comicId, comic.getName(), targetDate);

            if (imageOpt.isEmpty()) {
                // Image date exists but image couldn't be loaded
                return ComicNavigationResult.notFound("ERROR", targetDate, null, null);
            }

            // Get boundary dates for navigation hints
            LocalDate nearestPrev = storageFacade.getPreviousDateWithComic(comicId, comic.getName(), targetDate).orElse(null);
            LocalDate nearestNext = storageFacade.getNextDateWithComic(comicId, comic.getName(), targetDate).orElse(null);

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
            // Get the current comic configuration
            ComicConfig config = configFacade.loadComicConfig();
            
            // Download latest comics
            List<ComicDownloadResult> results = downloaderFacade.downloadLatestComics(config);
            
            // Process the results (just log them, don't affect return value)
            for (ComicDownloadResult result : results) {
                ComicDownloadRequest request = result.getRequest();
                
                if (result.isSuccessful()) {
                    // Save the comic to storage
                    boolean saved = storageFacade.saveComicStrip(
                            request.getComicId(),
                            request.getComicName(),
                            request.getDate(),
                            result.getImageData());
                    
                    if (!saved) {
                        log.error("Failed to save comic {} to storage", request.getComicName());
                    }
                    
                    // Update comic item metadata
                    getComic(request.getComicId()).ifPresent(comic -> {
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
                    });
                } else {
                    log.error("Failed to download comic {}: {}", 
                            request.getComicName(), result.getErrorMessage());
                }
            }
            
            // Always return true, as we're only concerned with triggering the update, not if it succeeded
            return true;
        } catch (Exception e) {
            log.error("Error occurred while updating all comics", e);
            // Still return true since we successfully triggered the update
            return true;
        }
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
    public boolean reconcileWithBootstrap() {
        log.info("Running comic configuration reconciliation...");
        
        try {
            // Only perform the reconciliation if it hasn't run today
            if (taskExecutionTracker.canRunToday(TASK_NAME)) {
                log.info("Performing reconciliation for today");
                
                // Load the configurations
                ComicConfig comicConfig = configFacade.loadComicConfig();
                Bootstrap config = configFacade.loadBootstrapConfig();
                
                // Reconcile with bootstrap config
                reconcileBootstrapConfig(comicConfig, config);
                
                // Save updated configuration
                configFacade.saveComicConfig(comicConfig);
                
                // Reload the comics
                refreshComicList();
                
                // Mark task as executed
                taskExecutionTracker.markTaskExecuted(TASK_NAME);
                
                return true;
            } else {
                log.info("Reconciliation already ran today ({}), skipping", 
                        taskExecutionTracker.getLastExecutionDate(TASK_NAME));
                return true;
            }
        } catch (Exception e) {
            log.error("Error during reconciliation: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Reconciles the comic configuration with the bootstrap configuration.
     * - New entries found in the bootstrap will be added and immediately cached
     * - Entries found in the comic config but not in the bootstrap will be removed
     */
    private void reconcileBootstrapConfig(ComicConfig comicConfig, Bootstrap bootstrap) {
        log.info("Begin reconciliation of bootstrap config and comic config");
        
        // Log any comic items with null names
        if (comicConfig.getItems() != null) {
            comicConfig.getItems().entrySet().stream()
                    .filter(entry -> entry.getValue().getName() == null)
                    .forEach(entry -> log.info("Comic with id={} has a null name in the configuration", entry.getKey()));
        }
        
        // Check for new GoComics
        if (bootstrap.getDailyComics() != null) {
            for (IComicsBootstrap daily : bootstrap.getDailyComics()) {
                ComicItem comic = findComicItem(comicConfig, daily);
                if (comic == null) {
                    log.info("Bootstrapping new daily comic: {}", daily.stripName());
                    
                    // Create download request
                    ComicDownloadRequest request = ComicDownloadRequest.builder()
                            .comicId(daily.stripName().hashCode())
                            .comicName(daily.stripName())
                            .source(daily.getSource())
                            .sourceIdentifier(daily.getSourceIdentifier())
                            .date(LocalDate.now())
                            .build();
                    
                    // Download the comic
                    ComicDownloadResult result = downloaderFacade.downloadComic(request);
                    
                    if (result.isSuccessful()) {
                        // Save the comic to storage
                        storageFacade.saveComicStrip(
                                request.getComicId(),
                                request.getComicName(),
                                request.getDate(),
                                result.getImageData());
                        
                        // Create and add new comic item
                        ComicItem newComic = ComicItem.builder()
                                .id(daily.stripName().hashCode())
                                .name(daily.stripName())
                                .newest(LocalDate.now())
                                .oldest(daily.startDate())
                                .enabled(true)
                                .source(daily.getSource())
                                .sourceIdentifier(daily.getSourceIdentifier())
                                .build();
                        
                        comicConfig.getItems().put(newComic.getId(), newComic);
                    } else {
                        log.error("Failed to bootstrap comic {}: {}", 
                                daily.stripName(), result.getErrorMessage());
                    }
                }
            }
        }
        
        // Check for new King Features
        if (bootstrap.getKingComics() != null) {
            for (IComicsBootstrap king : bootstrap.getKingComics()) {
                ComicItem comic = findComicItem(comicConfig, king);
                if (comic == null) {
                    log.info("Bootstrapping new King Features comic: {}", king.stripName());
                    
                    // Create download request
                    ComicDownloadRequest request = ComicDownloadRequest.builder()
                            .comicId(king.stripName().hashCode())
                            .comicName(king.stripName())
                            .source(king.getSource())
                            .sourceIdentifier(king.getSourceIdentifier())
                            .date(LocalDate.now())
                            .build();
                    
                    // Download the comic
                    ComicDownloadResult result = downloaderFacade.downloadComic(request);
                    
                    if (result.isSuccessful()) {
                        // Save the comic to storage
                        storageFacade.saveComicStrip(
                                request.getComicId(),
                                request.getComicName(),
                                request.getDate(),
                                result.getImageData());
                        
                        // Create and add new comic item
                        ComicItem newComic = ComicItem.builder()
                                .id(king.stripName().hashCode())
                                .name(king.stripName())
                                .newest(LocalDate.now())
                                .oldest(king.startDate())
                                .enabled(true)
                                .source(king.getSource())
                                .sourceIdentifier(king.getSourceIdentifier())
                                .build();
                        
                        comicConfig.getItems().put(newComic.getId(), newComic);
                    } else {
                        log.error("Failed to bootstrap comic {}: {}", 
                                king.stripName(), result.getErrorMessage());
                    }
                }
            }
        }
        
        // Remove entries found in comic config but not in bootstrap
        comicConfig.getItems().entrySet().removeIf(entry -> 
                findBootstrapComic(bootstrap, entry.getValue()) == null);
        
        log.info("Reconciliation complete");
    }

    /**
     * Given a comic item, locate the corresponding IComicsBootstrap from the bootstrap configuration.
     *
     * @param bootstrap Bootstrap configuration
     * @param comic ComicItem to lookup
     * @return IComicsBootstrap or null if none could be located
     */
    private IComicsBootstrap findBootstrapComic(Bootstrap bootstrap, ComicItem comic) {
        String comicName = comic.getName();
        if (comicName == null) {
            log.info("Comic with id={} has null name. This will be excluded from bootstrap reconciliation.", comic.getId());
            return null;
        }
        
        // Check daily comics
        if (bootstrap.getDailyComics() != null && !bootstrap.getDailyComics().isEmpty()) {
            for (IComicsBootstrap comic_bs : bootstrap.getDailyComics()) {
                if (comic_bs.stripName().equalsIgnoreCase(comicName)) {
                    return comic_bs;
                }
            }
        }
        
        // Check king comics
        if (bootstrap.getKingComics() != null && !bootstrap.getKingComics().isEmpty()) {
            for (IComicsBootstrap comic_bs : bootstrap.getKingComics()) {
                if (comic_bs.stripName().equalsIgnoreCase(comicName)) {
                    return comic_bs;
                }
            }
        }
        
        log.warn("Comic {} was not found in bootstrap config. It will be disabled.", comicName);
        return null;
    }

    /**
     * Given an IComicsBootstrap, locate the corresponding ComicItem from the comic configuration.
     *
     * @param comicConfig Comic configuration
     * @param comic IComicsBootstrap to lookup
     * @return ComicItem or null if none could be located
     */
    private ComicItem findComicItem(ComicConfig comicConfig, IComicsBootstrap comic) {
        if (comicConfig.getItems() != null && !comicConfig.getItems().isEmpty()) {
            // First check for and log any comics with null names
            comicConfig.getItems().entrySet().stream()
                    .filter(p -> p.getValue().getName() == null)
                    .forEach(p -> log.info("Found comic item with id={} that has a null name during bootstrap reconciliation", 
                            p.getKey()));
            
            // Find matching comic
            Optional<Map.Entry<Integer, ComicItem>> entry = comicConfig.getItems().entrySet().stream()
                    .filter(p -> {
                        String comicName = p.getValue().getName();
                        return comicName != null && comicName.equalsIgnoreCase(comic.stripName());
                    })
                    .findFirst();
            
            return entry.map(Map.Entry::getValue).orElse(null);
        }
        return null;
    }

    @Override
    public void scheduleReconciliation(String scheduleTime) {
        log.info("Configuring scheduled reconciliation for {}", scheduleTime);
        
        try {
            // Parse the schedule time
            LocalTime scheduledTime = LocalTime.parse(scheduleTime);
            
            var localNow = LocalDateTime.now();
            var currentZone = ZoneId.of("America/New_York");
            var zonedNow = ZonedDateTime.of(localNow, currentZone);
            
            // Set up next run time
            ZonedDateTime nextRun = zonedNow.withHour(scheduledTime.getHour())
                    .withMinute(scheduledTime.getMinute())
                    .withSecond(scheduledTime.getSecond());
            
            // If the scheduled time for today has already passed, schedule for tomorrow
            if (zonedNow.compareTo(nextRun) > 0) {
                nextRun = nextRun.plusDays(1);
            }
            
            // Calculate initial delay
            var duration = Duration.between(zonedNow, nextRun);
            long initialDelay = duration.getSeconds();
            
            // If already run today, adjust the initial delay to tomorrow
            if (!taskExecutionTracker.canRunToday(TASK_NAME)) {
                log.info("Reconciliation already ran today ({}), scheduling for tomorrow", 
                        taskExecutionTracker.getLastExecutionDate(TASK_NAME));
                nextRun = nextRun.plusDays(1);
                initialDelay = Duration.between(zonedNow, nextRun).getSeconds();
            }
            
            // Create and configure the scheduler
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(
                    this::reconcileWithBootstrap, 
                    initialDelay,
                    24 * 60 * 60L, 
                    TimeUnit.SECONDS);
            
            log.info("Scheduled reconciliation configured for {} (in {} minutes)", 
                    nextRun.toLocalTime(),
                    initialDelay / 60);
        } catch (Exception e) {
            log.error("Error scheduling reconciliation: {}", e.getMessage(), e);
        }
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