package org.stapledon.core.comic.management;

import org.springframework.stereotype.Component;
import org.stapledon.api.dto.comic.ComicConfig;
import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.api.dto.comic.ImageDto;
import org.stapledon.common.util.Bootstrap;
import org.stapledon.common.util.Direction;
import org.stapledon.core.comic.downloader.ComicDownloaderFacade;
import org.stapledon.core.comic.dto.ComicDownloadRequest;
import org.stapledon.core.comic.dto.ComicDownloadResult;
import org.stapledon.events.CacheMissEvent;
import org.stapledon.infrastructure.config.ConfigurationFacade;
import org.stapledon.infrastructure.config.IComicsBootstrap;
import org.stapledon.infrastructure.config.TaskExecutionTracker;
import org.stapledon.infrastructure.config.properties.StartupReconcilerProperties;
import org.stapledon.infrastructure.storage.ComicStorageFacade;

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
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the ComicManagementFacade interface.
 * Acts as the central coordinator between all other facades in the application.
 */
@Slf4j
@Component
public class ComicManagementFacadeImpl implements ComicManagementFacade {

    private static final String TASK_NAME = "ComicReconciliation";
    
    private final ComicStorageFacade storageFacade;
    private final ConfigurationFacade configFacade;
    private final ComicDownloaderFacade downloaderFacade;
    private final TaskExecutionTracker taskExecutionTracker;
    
    // Thread-safe collection for comics
    private final Map<Integer, ComicItem> comics = new ConcurrentHashMap<>();
    private final List<Consumer<CacheMissEvent>> cacheMissListeners = new ArrayList<>();
    
    public ComicManagementFacadeImpl(
            ComicStorageFacade storageFacade,
            ConfigurationFacade configFacade,
            ComicDownloaderFacade downloaderFacade,
            StartupReconcilerProperties reconcilerProperties,
            TaskExecutionTracker taskExecutionTracker) {
        this.storageFacade = storageFacade;
        this.configFacade = configFacade;
        this.downloaderFacade = downloaderFacade;
        this.taskExecutionTracker = taskExecutionTracker;
        
        // Register as listener for cache miss events
        storageFacade.addCacheMissListener(this::handleCacheMiss);
        
        // Load comics from configuration
        refreshComicList();
        
        // Schedule reconciliation if enabled
        if (reconcilerProperties.isEnabled()) {
            scheduleReconciliation(reconcilerProperties.getScheduleTime());
        } else {
            log.warn("Scheduled reconciliation is disabled");
        }
    }

    /**
     * Handles cache miss events by attempting to download the missing comic.
     */
    private void handleCacheMiss(CacheMissEvent event) {
        log.debug("Handling cache miss for comic id={}, name={}, date={}", 
                event.getComicId(), event.getComicName(), event.getDate());
        
        // Notify any registered listeners
        notifyCacheMissListeners(event);
        
        // Only download if we can find the comic
        ComicItem comic = comics.get(event.getComicId());
        if (comic != null) {
            // Create download request
            ComicDownloadRequest request = ComicDownloadRequest.builder()
                    .comicId(event.getComicId())
                    .comicName(event.getComicName())
                    .source(comic.getSource())
                    .sourceIdentifier(comic.getSourceIdentifier())
                    .date(event.getDate())
                    .build();
            
            // Download the comic
            ComicDownloadResult result = downloaderFacade.downloadComic(request);
            
            // Save the comic if download was successful
            if (result.isSuccessful()) {
                storageFacade.saveComicStrip(
                        event.getComicId(),
                        event.getComicName(),
                        event.getDate(),
                        result.getImageData());
            } else {
                log.error("Failed to download comic {} for date {}: {}", 
                        event.getComicName(), event.getDate(), result.getErrorMessage());
            }
        } else {
            log.warn("Cannot handle cache miss - comic id={} not found", event.getComicId());
        }
    }
    
    /**
     * Notifies all registered cache miss listeners about the event.
     */
    private void notifyCacheMissListeners(CacheMissEvent event) {
        for (Consumer<CacheMissEvent> listener : cacheMissListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                log.error("Error notifying cache miss listener: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public void registerCacheMissHandler(Consumer<CacheMissEvent> handler) {
        cacheMissListeners.add(handler);
    }

    @Override
    public List<ComicItem> getAllComics() {
        List<ComicItem> result = new ArrayList<>(comics.values());
        Collections.sort(result);
        return result;
    }

    @Override
    public Optional<ComicItem> getComic(int comicId) {
        return Optional.ofNullable(comics.get(comicId));
    }

    @Override
    public Optional<ComicItem> getComicByName(String comicName) {
        return comics.values().stream()
                .filter(comic -> comic.getName().equalsIgnoreCase(comicName))
                .findFirst();
    }

    @Override
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
    public Optional<ImageDto> getComicStrip(int comicId, Direction direction) {
        return getComic(comicId)
                .flatMap(comic -> {
                    try {
                        // Get the date based on direction
                        LocalDate date;
                        if (direction == Direction.FORWARD) {
                            Optional<LocalDate> oldestDate = storageFacade.getOldestDateWithComic(comicId, comic.getName());
                            if (oldestDate.isEmpty()) {
                                return Optional.empty();
                            }
                            date = oldestDate.get();
                        } else {
                            Optional<LocalDate> newestDate = storageFacade.getNewestDateWithComic(comicId, comic.getName());
                            if (newestDate.isEmpty()) {
                                return Optional.empty();
                            }
                            date = newestDate.get();
                        }
                        
                        // Get the image for the date
                        return storageFacade.getComicStrip(comicId, comic.getName(), date);
                    } catch (Exception e) {
                        log.error("Error retrieving comic strip: {}", e.getMessage(), e);
                        return Optional.empty();
                    }
                });
    }

    @Override
    public Optional<ImageDto> getComicStrip(int comicId, Direction direction, LocalDate from) {
        return getComic(comicId)
                .flatMap(comic -> {
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
                            return Optional.empty();
                        }
                        
                        // Get the image for the date
                        return storageFacade.getComicStrip(comicId, comic.getName(), dateOpt.get());
                    } catch (Exception e) {
                        log.error("Error retrieving comic strip: {}", e.getMessage(), e);
                        return Optional.empty();
                    }
                });
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
        // Get the current comic configuration
        ComicConfig config = configFacade.loadComicConfig();
        
        // Download latest comics
        List<ComicDownloadResult> results = downloaderFacade.downloadLatestComics(config);
        
        boolean allSucceeded = true;
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
                    allSucceeded = false;
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
                allSucceeded = false;
            }
        }
        
        return allSucceeded;
    }

    @Override
    public boolean updateComic(int comicId) {
        return getComic(comicId)
                .map(comic -> {
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
                            return true;
                        } else {
                            log.error("Failed to save comic {} to storage", comic.getName());
                            return false;
                        }
                    } else {
                        log.error("Failed to download comic {}: {}", 
                                comic.getName(), result.getErrorMessage());
                        return false;
                    }
                })
                .orElse(false);
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
        // Check daily comics
        if (bootstrap.getDailyComics() != null && !bootstrap.getDailyComics().isEmpty()) {
            for (IComicsBootstrap comic_bs : bootstrap.getDailyComics()) {
                if (comic_bs.stripName().equalsIgnoreCase(comic.getName())) {
                    return comic_bs;
                }
            }
        }
        
        // Check king comics
        if (bootstrap.getKingComics() != null && !bootstrap.getKingComics().isEmpty()) {
            for (IComicsBootstrap comic_bs : bootstrap.getKingComics()) {
                if (comic_bs.stripName().equalsIgnoreCase(comic.getName())) {
                    return comic_bs;
                }
            }
        }
        
        log.warn("Comic {} was not found in bootstrap config. It will be disabled.", comic.getName());
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
            Optional<Map.Entry<Integer, ComicItem>> entry = comicConfig.getItems().entrySet().stream()
                    .filter(p -> p.getValue().getName().equalsIgnoreCase(comic.stripName()))
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
}