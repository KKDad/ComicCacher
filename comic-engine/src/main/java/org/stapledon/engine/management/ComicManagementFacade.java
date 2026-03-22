package org.stapledon.engine.management;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicSaveData;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.dto.StripLoaderKey;
import org.stapledon.common.dto.StripLoaderKey.DateStripKey;
import org.stapledon.common.dto.StripLoaderKey.BoundaryStripKey;
import org.stapledon.common.service.ComicConfigurationService;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.common.service.RetrievalStatusService;
import org.stapledon.common.util.Direction;
import org.stapledon.engine.downloader.DownloaderFacade;

/**
 * Implementation of the ManagementFacade interface. Acts as the central
 * coordinator between all other facades in the application.
 */
@Slf4j
@ToString
@Component
public class ComicManagementFacade implements ManagementFacade {

    private final ComicStorageFacade storageFacade;
    private final ComicConfigurationService configFacade;
    private final DownloaderFacade downloaderFacade;
    private final RetrievalStatusService retrievalStatusService;

    /**
     * In-memory cache of comics for O(1) lookups.
     * <p>
     * This is the PRIMARY SOURCE during runtime - the config file serves as the
     * persistence layer. Changes are written through to config but reads are
     * always served from this cache.
     * </p>
     * <p>
     * IMPORTANT:
     * <ul>
     *   <li>If config file is modified externally, call {@link #refreshComicList()}</li>
     *   <li>Multi-instance deployments should use distributed cache instead (Redis, Hazelcast, etc.)</li>
     *   <li>All mutation methods automatically update both cache and config file</li>
     * </ul>
     * </p>
     * <p>
     * Thread safety: ConcurrentHashMap provides thread-safe read/write operations.
     * However, the write-through to config file is not atomic - see individual
     * mutation methods for transaction handling.
     * </p>
     */
    private final Map<Integer, ComicItem> comics = new ConcurrentHashMap<>();

    public ComicManagementFacade(ComicStorageFacade storageFacade, ComicConfigurationService configFacade,
            DownloaderFacade downloaderFacade, RetrievalStatusService retrievalStatusService) {
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
    public Optional<ComicItem> getComic(int comicId) {
        return Optional.ofNullable(comics.get(comicId));
    }

    @Override
    public Optional<ComicItem> getComicByName(String comicName) {
        if (comicName == null) {
            return Optional.empty();
        }

        return comics.values().stream()
                .filter(comic -> Optional.ofNullable(comic.getName())
                                        .map(name -> name.equalsIgnoreCase(comicName))
                                        .orElse(false))
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
            comicItem = comicItem.toBuilder().id(comicId).build();
        }

        comics.put(comicId, comicItem);

        // Save to configuration
        ComicConfig config = configFacade.loadComicConfig();
        config.getItems().put(comicId, comicItem);
        configFacade.saveComicConfig(config);

        return Optional.of(comicItem);
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
            ComicDownloadRequest request = ComicDownloadRequest.builder().comicId(comic.getId())
                    .comicName(comic.getName()).source(comic.getSource())
                    .sourceIdentifier(comic.getSourceIdentifier()).date(LocalDate.now()).build();

            // Download the comic
            ComicDownloadResult result = downloaderFacade.downloadComic(request);

            if (result.isSuccessful()) {
                // Save the comic to storage
                boolean saved = storageFacade.saveComicStrip(ComicIdentifier.from(comic), request.getDate(),
                        result.getImageData());

                if (saved) {
                    // Update comic item metadata
                    ComicItem updated = comic.toBuilder().newest(request.getDate()).build();

                    updateComic(comic.getId(), updated);
                } else {
                    log.error("Failed to save comic {} to storage", comic.getName());
                }
            } else {
                log.error("Failed to download comic {}: {}", comic.getName(), result.getErrorMessage());
            }

            // Always return true if the comic exists and we attempted to update it,
            // regardless of whether the update succeeded
            return true;

        } catch (Exception e) {
            log.error("Error occurred while updating comic with ID {}", comicId, e);
            // Still return true since we successfully triggered the update for an existing
            // comic
            return true;
        }
    }

    @Override
    public boolean updateComic(String comicName) {
        return getComicByName(comicName).map(comic -> updateComic(comic.getId())).orElse(false);
    }

    @Override
    @CacheEvict(value = "comicMetadata", allEntries = true)
    public boolean deleteComic(int comicId) {
        ComicItem removed = comics.remove(comicId);

        if (removed != null) {
            // Also remove from storage and configuration
            storageFacade.deleteComic(ComicIdentifier.from(removed));

            ComicConfig config = configFacade.loadComicConfig();
            config.getItems().remove(comicId);
            configFacade.saveComicConfig(config);

            return true;
        }

        return false;
    }

    @Override
    public ComicNavigationResult getComicStrip(int comicId, Direction direction) {
        return getComic(comicId)
                .map(comic -> getComicStripForDirection(comic, direction))
                .orElseGet(() -> ComicNavigationResult.notFound("NO_COMICS_AVAILABLE", null, null, null));
    }

    @Override
    public ComicNavigationResult getComicStrip(int comicId, Direction direction, LocalDate from) {
        return getComic(comicId)
                .map(comic -> getComicStripInternal(comicId, comic.getName(), direction, from))
                .orElseGet(() -> ComicNavigationResult.notFound("NO_COMICS_AVAILABLE", from, null, null));
    }

    private ComicNavigationResult getComicStripForDirection(ComicItem comic, Direction direction) {
        try {
            ComicIdentifier identifier = ComicIdentifier.from(comic);

            // Get the date based on direction
            Optional<LocalDate> dateOpt = direction == Direction.FORWARD
                    ? storageFacade.getOldestDateWithComic(identifier)
                    : storageFacade.getNewestDateWithComic(identifier);

            return dateOpt
                    .flatMap(date -> storageFacade.getComicStrip(identifier, date)
                            .map(image -> {
                                LocalDate nearestPrev = storageFacade.getPreviousDateWithComic(identifier, date)
                                        .orElse(null);
                                LocalDate nearestNext = storageFacade.getNextDateWithComic(identifier, date)
                                        .orElse(null);
                                return ComicNavigationResult.found(image, nearestPrev, nearestNext);
                            }))
                    .orElseGet(() -> ComicNavigationResult.notFound("NO_COMICS_AVAILABLE", null, null, null));
        } catch (Exception e) {
            log.error("Error retrieving comic strip: {}", e.getMessage(), e);
            return ComicNavigationResult.notFound("ERROR", null, null, null);
        }
    }

    /**
     * Internal implementation of getComicStrip.
     */
    private ComicNavigationResult getComicStripInternal(int comicId, String comicName, Direction direction,
            LocalDate from) {
        log.info("getComicStrip: comicId={}, comicName={}, direction={}, from={}", comicId, comicName, direction, from);

        ComicIdentifier identifier = new ComicIdentifier(comicId, comicName);

        try {
            // Get the next/previous date
            Optional<LocalDate> dateOpt = direction == Direction.FORWARD
                    ? storageFacade.getNextDateWithComic(identifier, from)
                    : storageFacade.getPreviousDateWithComic(identifier, from);

            log.info("get{}DateWithComic returned: {} (from: {})",
                    direction == Direction.FORWARD ? "Next" : "Previous",
                    dateOpt.orElse(null), from);

            if (dateOpt.isEmpty()) {
                String reason = direction == Direction.FORWARD ? "AT_END" : "AT_BEGINNING";
                log.info("No comic found going {} from {}, reason={}", direction, from, reason);

                // Get the nearest dates for navigation hints
                LocalDate nearestPrev = storageFacade.getPreviousDateWithComic(identifier, from).orElse(null);
                LocalDate nearestNext = storageFacade.getNextDateWithComic(identifier, from).orElse(null);

                log.info("Returning navigation result: found=false, currentDate=null, nearestPrev={}, nearestNext={}",
                        nearestPrev, nearestNext);
                return ComicNavigationResult.notFound(reason, from, nearestPrev, nearestNext);
            }

            // Get the image for the found date
            LocalDate targetDate = dateOpt.get();
            log.info("Found comic at {}, loading image and calculating boundaries...", targetDate);

            return storageFacade.getComicStrip(identifier, targetDate)
                    .map(image -> {
                        // Get boundary dates for navigation hints
                        LocalDate nearestPrev = storageFacade.getPreviousDateWithComic(identifier, targetDate)
                                .orElse(null);
                        LocalDate nearestNext = storageFacade.getNextDateWithComic(identifier, targetDate)
                                .orElse(null);

                        log.info("Returning navigation result: found=true, currentDate={}, nearestPrev={}, nearestNext={}",
                                targetDate, nearestPrev, nearestNext);
                        return ComicNavigationResult.found(image, nearestPrev, nearestNext);
                    })
                    .orElseGet(() -> {
                        log.warn("Image date exists but image couldn't be loaded for {} on {}", comicName, targetDate);
                        return ComicNavigationResult.notFound("ERROR", targetDate, null, null);
                    });
        } catch (Exception e) {
            log.error("Error retrieving comic strip: {}", e.getMessage(), e);
            return ComicNavigationResult.notFound("ERROR", from, null, null);
        }
    }

    @Override
    public Optional<ImageDto> getComicStripOnDate(int comicId, LocalDate date) {
        return getComic(comicId).flatMap(comic -> storageFacade.getComicStrip(ComicIdentifier.from(comic), date));
    }

    @Override
    public Optional<ImageDto> getComicStripOnDate(String comicName, LocalDate date) {
        return getComicByName(comicName)
                .flatMap(comic -> storageFacade.getComicStrip(ComicIdentifier.from(comic), date));
    }

    @Override
    public ComicNavigationResult getComicStripWithNavigation(int comicId, LocalDate date) {
        return getComic(comicId)
                .map(comic -> getComicStripWithNavigationInternal(comic, date))
                .orElseGet(() -> ComicNavigationResult.notFound("NO_COMICS_AVAILABLE", date, null, null));
    }

    private ComicNavigationResult getComicStripWithNavigationInternal(ComicItem comic, LocalDate date) {
        ComicIdentifier identifier = ComicIdentifier.from(comic);
        log.info("getComicStripWithNavigation: comicId={}, comicName={}, date={}", comic.getId(), comic.getName(), date);

        try {
            // Calculate navigation boundaries
            LocalDate nearestPrev = storageFacade.getPreviousDateWithComic(identifier, date).orElse(null);
            LocalDate nearestNext = storageFacade.getNextDateWithComic(identifier, date).orElse(null);

            // Get the image for the exact date requested
            return storageFacade.getComicStrip(identifier, date)
                    .map(image -> {
                        log.info("Found comic strip for {} on {}, prev={}, next={}",
                                comic.getName(), date, nearestPrev, nearestNext);
                        return ComicNavigationResult.found(image, nearestPrev, nearestNext);
                    })
                    .orElseGet(() -> {
                        log.info("No comic strip found for {} on {}, returning navigation hints: prev={}, next={}",
                                comic.getName(), date, nearestPrev, nearestNext);
                        return ComicNavigationResult.notFound("NOT_AVAILABLE", date, nearestPrev, nearestNext);
                    });
        } catch (Exception e) {
            log.error("Error retrieving comic strip on exact date: {}", e.getMessage(), e);
            return ComicNavigationResult.notFound("ERROR", date, null, null);
        }
    }

    @Override
    public Map<StripLoaderKey, ComicNavigationResult> getComicStripsWithNavigation(Set<StripLoaderKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("Batch loading {} comic strips", keys.size());

        // Group by comicId for efficient processing
        Map<Integer, List<StripLoaderKey>> byComic = keys.stream()
                .collect(Collectors.groupingBy(StripLoaderKey::comicId));

        Map<StripLoaderKey, ComicNavigationResult> results = new HashMap<>();

        byComic.forEach((comicId, comicKeys) ->
            getComic(comicId).ifPresentOrElse(
                comic -> comicKeys.forEach(key -> {
                    ComicNavigationResult result = switch (key) {
                        case DateStripKey dateKey -> getComicStripWithNavigation(comicId, dateKey.date());
                        case BoundaryStripKey boundaryKey -> getComicStrip(comicId, boundaryKey.direction());
                    };
                    results.put(key, result);
                }),
                () -> comicKeys.forEach(key -> {
                    LocalDate dateForResult = switch (key) {
                        case DateStripKey dateKey -> dateKey.date();
                        case BoundaryStripKey boundaryKey -> null;
                    };
                    results.put(key, ComicNavigationResult.notFound(
                            "NO_COMICS_AVAILABLE", dateForResult, null, null));
                })
            )
        );

        return results;
    }

    @Override
    public Optional<ImageDto> getAvatar(int comicId) {
        return getComic(comicId).flatMap(comic -> storageFacade.getAvatar(ComicIdentifier.from(comic)));
    }

    @Override
    public Optional<ImageDto> getAvatar(String comicName) {
        return getComicByName(comicName).flatMap(comic -> storageFacade.getAvatar(ComicIdentifier.from(comic)));
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
                        log.info("Skipping comic '{}' - not published on {} (publishes: {})", comic.getName(),
                                dayOfWeek, comic.getPublicationDays());
                        continue;
                    }
                }

                // Check if comic already exists on disk
                if (storageFacade.comicStripExists(ComicIdentifier.from(comic), date)) {
                    log.info("Skipping comic '{}' for {} - already cached", comic.getName(), date);
                    continue;
                }

                // Handle indexed comics differently
                if (downloaderFacade.isIndexedSource(comic.getSource())) {
                    Optional<ComicDownloadResult> indexedResult = downloadLatestIndexedComic(comic);
                    indexedResult.ifPresent(results::add);
                    continue;
                }

                // Comic doesn't exist - download it
                ComicDownloadRequest request = ComicDownloadRequest.builder().comicId(comic.getId())
                        .comicName(comic.getName()).source(comic.getSource())
                        .sourceIdentifier(comic.getSourceIdentifier()).date(date).build();

                ComicDownloadResult result = downloaderFacade.downloadComic(request);
                results.add(result);

                if (result.isSuccessful()) {
                    // Save the comic to storage
                    boolean saved = storageFacade.saveComicStrip(ComicIdentifier.from(comic), date,
                            result.getImageData());

                    if (!saved) {
                        log.error("Failed to save comic {} to storage", comic.getName());
                        continue;
                    }

                    // Update comic item metadata
                    ComicItem updated = comic.toBuilder().newest(date).build();

                    updateComic(comic.getId(), updated);
                } else {
                    log.error("Failed to download comic {}: {}", comic.getName(), result.getErrorMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while updating comics for date {}", date, e);
        }

        return results;
    }

    @Override
    public Optional<ComicDownloadResult> downloadComicForDate(ComicItem comic, LocalDate date) {
        // Validate comic has a source
        if (comic.getSource() == null || comic.getSource().isEmpty()) {
            log.warn("Cannot download comic '{}' - has null or empty source", comic.getName());
            return Optional.empty();
        }

        // Check if comic already exists on disk
        if (storageFacade.comicStripExists(ComicIdentifier.from(comic), date)) {
            log.debug("Comic '{}' for {} already cached", comic.getName(), date);
            return Optional.empty();
        }

        // Create download request and download
        ComicDownloadRequest request = ComicDownloadRequest.builder().comicId(comic.getId()).comicName(comic.getName())
                .source(comic.getSource())
                .sourceIdentifier(comic.getSourceIdentifier()).date(date).build();

        ComicDownloadResult result = downloaderFacade.downloadComic(request);

        if (result.isSuccessful()) {
            LocalDate saveDate = result.getActualDate() != null ? result.getActualDate() : date;
            boolean saved = saveDownloadResult(comic, saveDate, result);

            if (!saved) {
                return Optional.empty();
            }

            // Update oldest date if this is earlier than known
            LocalDate currentOldest = comic.getOldest();
            if (currentOldest == null || saveDate.isBefore(currentOldest)) {
                ComicItem updated = comic.toBuilder().oldest(saveDate).build();
                updateComic(comic.getId(), updated);
            }

            // Update strip number tracking for indexed comics
            updateStripNumberTracking(comic, result);
        }

        return Optional.of(result);
    }

    @Override
    public Optional<ComicDownloadResult> downloadLatestIndexedComic(ComicItem comic) {
        if (comic.getSource() == null || comic.getSource().isEmpty()) {
            log.warn("Cannot download comic '{}' - has null or empty source", comic.getName());
            return Optional.empty();
        }

        ComicDownloadResult result = downloaderFacade.downloadLatestStrip(comic);

        if (result.isSuccessful()) {
            LocalDate saveDate = result.getActualDate() != null ? result.getActualDate() : LocalDate.now();

            // Check if this date already exists
            if (storageFacade.comicStripExists(ComicIdentifier.from(comic), saveDate)) {
                log.debug("Comic '{}' for {} already cached", comic.getName(), saveDate);
                return Optional.empty();
            }

            boolean saved = saveDownloadResult(comic, saveDate, result);
            if (!saved) {
                return Optional.empty();
            }

            // Update newest date
            ComicItem.ComicItemBuilder builder = comic.toBuilder().newest(saveDate);
            updateStripNumberOnBuilder(builder, result);
            updateComic(comic.getId(), builder.build());
        }

        return Optional.of(result);
    }

    @Override
    public Optional<ComicDownloadResult> downloadComicByStripNumber(ComicItem comic, int stripNumber) {
        if (comic.getSource() == null || comic.getSource().isEmpty()) {
            log.warn("Cannot download comic '{}' - has null or empty source", comic.getName());
            return Optional.empty();
        }

        ComicDownloadResult result = downloaderFacade.downloadStrip(comic, stripNumber);

        if (result.isSuccessful()) {
            LocalDate saveDate = result.getActualDate() != null ? result.getActualDate() : LocalDate.now();

            // Check if this date already exists
            if (storageFacade.comicStripExists(ComicIdentifier.from(comic), saveDate)) {
                log.debug("Comic '{}' for {} already cached", comic.getName(), saveDate);
                return Optional.empty();
            }

            boolean saved = saveDownloadResult(comic, saveDate, result);
            if (!saved) {
                return Optional.empty();
            }

            // Update oldest date if this is earlier than known (backfill goes backwards)
            LocalDate currentOldest = comic.getOldest();
            if (currentOldest == null || saveDate.isBefore(currentOldest)) {
                updateComic(comic.getId(), comic.toBuilder().oldest(saveDate).build());
            }
        }

        return Optional.of(result);
    }

    /**
     * Saves a download result to storage via ComicSaveData DTO.
     */
    private boolean saveDownloadResult(ComicItem comic, LocalDate date, ComicDownloadResult result) {
        ComicSaveData saveData = ComicSaveData.builder()
                .imageData(result.getImageData())
                .transcript(result.getTranscript())
                .stripNumber(result.getStripNumber())
                .build();

        boolean saved = storageFacade.saveComicStripWithResult(
                ComicIdentifier.from(comic), date, saveData).isSuccess();

        if (!saved) {
            log.error("Failed to save comic {} to storage", comic.getName());
        }
        return saved;
    }

    /**
     * Updates strip number tracking on the comic if the result contains strip number info.
     */
    private void updateStripNumberTracking(ComicItem comic, ComicDownloadResult result) {
        if (result.getStripNumber() != null) {
            Integer currentLast = comic.getLastStripNumber();
            if (currentLast == null || result.getStripNumber() > currentLast) {
                updateComic(comic.getId(),
                        comic.toBuilder().lastStripNumber(result.getStripNumber()).build());
            }
        }
    }

    /**
     * Updates strip number fields on a builder if the result contains strip number info.
     */
    private void updateStripNumberOnBuilder(ComicItem.ComicItemBuilder builder, ComicDownloadResult result) {
        if (result.getStripNumber() != null) {
            builder.lastStripNumber(result.getStripNumber());
        }
    }

    @Override
    public void refreshComicList() {
        long startTime = System.currentTimeMillis();
        try {
            // Load comic configuration
            ComicConfig comicConfig = configFacade.loadComicConfig();

            // Clear and reload comics
            comics.clear();
            if (comicConfig.getItems() != null) {
                comics.putAll(comicConfig.getItems());
            }

            // Sync oldest/newest dates and avatarAvailable flag from the actual index
            boolean configDirty = false;
            for (Map.Entry<Integer, ComicItem> entry : comics.entrySet()) {
                ComicItem comic = entry.getValue();
                ComicIdentifier id = ComicIdentifier.from(comic);
                Optional<LocalDate> actualOldest = storageFacade.getOldestDateWithComic(id);
                Optional<LocalDate> actualNewest = storageFacade.getNewestDateWithComic(id);
                boolean avatarExists = storageFacade.getAvatar(id).isPresent();

                boolean datesStale = actualOldest.isPresent() && !actualOldest.get().equals(comic.getOldest())
                        || actualNewest.isPresent() && !actualNewest.get().equals(comic.getNewest());
                boolean avatarStale = avatarExists != comic.isAvatarAvailable();

                if (datesStale || avatarStale) {
                    ComicItem updated = comic.toBuilder()
                            .oldest(actualOldest.orElse(comic.getOldest()))
                            .newest(actualNewest.orElse(comic.getNewest()))
                            .avatarAvailable(avatarExists)
                            .build();
                    entry.setValue(updated);
                    comicConfig.getItems().put(updated.getId(), updated);
                    configDirty = true;
                }
            }
            if (configDirty) {
                configFacade.saveComicConfig(comicConfig);
                log.info("Synced comic metadata from index for {} comics", comics.size());
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Refreshed comic list: loaded {} comics in {}ms", comics.size(), duration);
        } catch (Exception e) {
            log.error("Error refreshing comic list: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean purgeOldImages(int daysToKeep) {
        boolean allSucceeded = true;

        for (ComicItem comic : comics.values()) {
            boolean success = storageFacade.purgeOldImages(ComicIdentifier.from(comic), daysToKeep);
            if (!success) {
                log.error("Failed to purge old images for comic {}", comic.getName());
                allSucceeded = false;
            }
        }

        return allSucceeded;
    }

    @Override
    public List<ComicNavigationResult> getStripWindow(int comicId, LocalDate center, int before, int after) {
        return getComic(comicId)
                .map(comic -> {
                    ComicIdentifier identifier = ComicIdentifier.from(comic);
                    List<LocalDate> dates = new ArrayList<>();

                    // Walk backward from center to collect `before` dates
                    LocalDate cursor = center;
                    for (int i = 0; i < before; i++) {
                        Optional<LocalDate> prev = storageFacade.getPreviousDateWithComic(identifier, cursor);
                        if (prev.isEmpty()) {
                            break;
                        }
                        dates.addFirst(prev.get());
                        cursor = prev.get();
                    }

                    // Add center
                    dates.add(center);

                    // Walk forward from center to collect `after` dates
                    cursor = center;
                    for (int i = 0; i < after; i++) {
                        Optional<LocalDate> next = storageFacade.getNextDateWithComic(identifier, cursor);
                        if (next.isEmpty()) {
                            break;
                        }
                        dates.add(next.get());
                        cursor = next.get();
                    }

                    // Fetch navigation results for each date
                    return dates.stream()
                            .map(date -> getComicStripWithNavigation(comicId, date))
                            .toList();
                })
                .orElse(List.of());
    }

    @Override
    public Optional<LocalDate> getRandomDate(int comicId) {
        return getComic(comicId)
                .flatMap(comic -> {
                    List<LocalDate> dates = storageFacade.getAvailableDates(ComicIdentifier.from(comic));
                    if (dates.isEmpty()) {
                        return Optional.empty();
                    }
                    int index = java.util.concurrent.ThreadLocalRandom.current().nextInt(dates.size());
                    return Optional.of(dates.get(index));
                });
    }

    @Override
    public Optional<LocalDate> getNewestDateWithComic(int comicId) {
        return getComic(comicId).flatMap(comic -> storageFacade.getNewestDateWithComic(ComicIdentifier.from(comic)));
    }

    @Override
    public Optional<LocalDate> getOldestDateWithComic(int comicId) {
        return getComic(comicId).flatMap(comic -> storageFacade.getOldestDateWithComic(ComicIdentifier.from(comic)));
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
    public List<ComicRetrievalRecord> getFilteredRetrievalRecords(String comicName, ComicRetrievalStatus status,
            LocalDate fromDate, LocalDate toDate, int limit) {
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

    @Override
    @CacheEvict(value = "comicMetadata", allEntries = true)
    public int downloadMissingAvatars() {
        int downloaded = 0;
        int skipped = 0;
        int failed = 0;

        for (ComicItem comic : comics.values()) {
            ComicIdentifier identifier = ComicIdentifier.from(comic);

            // Skip if avatar already exists on disk
            if (storageFacade.getAvatar(identifier).isPresent()) {
                skipped++;
                continue;
            }

            // Skip if no source configured
            if (comic.getSource() == null || comic.getSource().isEmpty()) {
                log.debug("Skipping avatar download for '{}' - no source configured", comic.getName());
                continue;
            }

            log.info("Downloading missing avatar for '{}'", comic.getName());
            Optional<byte[]> avatarData = downloaderFacade.downloadAvatar(
                    comic.getId(), comic.getName(), comic.getSource(), comic.getSourceIdentifier());

            if (avatarData.isPresent()) {
                boolean saved = storageFacade.saveAvatar(identifier, avatarData.get());
                if (saved) {
                    if (!comic.isAvatarAvailable()) {
                        ComicItem updated = comic.toBuilder().avatarAvailable(true).build();
                        updateComic(comic.getId(), updated);
                    }
                    downloaded++;
                    log.info("Successfully downloaded avatar for '{}'", comic.getName());
                } else {
                    failed++;
                    log.error("Failed to save avatar for '{}'", comic.getName());
                }
            } else {
                // Download failed — ensure flag reflects reality
                if (comic.isAvatarAvailable()) {
                    ComicItem updated = comic.toBuilder().avatarAvailable(false).build();
                    updateComic(comic.getId(), updated);
                }
                failed++;
                log.warn("Could not download avatar for '{}'", comic.getName());
            }
        }

        log.info("Avatar backfill complete: {} downloaded, {} skipped (already exist), {} failed",
                downloaded, skipped, failed);
        return downloaded;
    }
}
