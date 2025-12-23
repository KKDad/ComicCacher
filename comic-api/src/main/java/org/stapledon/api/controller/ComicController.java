package org.stapledon.api.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.model.ResponseBuilder;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.model.ComicCachingException;
import org.stapledon.common.model.ComicImageNotFoundException;
import org.stapledon.common.model.ComicNotFoundException;
import org.stapledon.common.util.Direction;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.infrastructure.caching.PredictiveCacheService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1"})
public class ComicController {

    private final ManagementFacade comicManagementFacade;
    private final Optional<PredictiveCacheService> predictiveCacheService;
    private final Optional<org.stapledon.metrics.collector.AccessMetricsCollector> accessMetricsCollector;

    /*****************************************************************************************************************
     * Comic Strip Listing and Configuration
     *****************************************************************************************************************/

    @GetMapping("/comics")
    public ResponseEntity<ApiResponse<List<ComicItem>>> retrieveAllComics() {
        return ResponseBuilder.collection(comicManagementFacade.getAllComics());
    }

    @GetMapping("/comics/{comic}")
    public ResponseEntity<ApiResponse<ComicItem>> retrieveComicDetails(@PathVariable(name = "comic") Integer comicId) {
        return comicManagementFacade.getComic(comicId)
                .map(ResponseBuilder::ok)
                .orElseThrow(() -> new ComicNotFoundException(comicId));
    }

    @PostMapping("/comics/{comic}")
    public ResponseEntity<ApiResponse<ComicItem>> createComicDetails(@RequestBody ComicItem comicItem, @PathVariable(name = "comic") Integer comicId) {
        
        // Validate comic name is not null
        if (comicItem.getName() == null) {
            throw new IllegalArgumentException("Comic name cannot be null");
        }
        
        // Ensure the comic ID in the path matches the comic ID in the request body
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
        
        return comicManagementFacade.createComic(comicItem)
                .map(ResponseBuilder::created)
                .orElseThrow(() -> new ComicCachingException("Unable to save ComicItem"));
    }

    @PatchMapping("/comics/{comic}")
    public ResponseEntity<ApiResponse<ComicItem>> updateComicDetails(@PathVariable(name = "comic") Integer comicId, @RequestBody ComicItem comicItem) {
        // Validate comic name is not null
        if (comicItem.getName() == null) {
            throw new IllegalArgumentException("Comic name cannot be null");
        }
        
        return comicManagementFacade.updateComic(comicId, comicItem)
                .map(ResponseBuilder::ok)
                .orElseThrow(() -> new ComicCachingException("Unable to update ComicItem"));
    }

    @DeleteMapping("/comics/{comic}")
    public ResponseEntity<Void> deleteComicDetails(@PathVariable(name = "comic") Integer comicId) {
        boolean result = comicManagementFacade.deleteComic(comicId);

        if (result) {
            return ResponseBuilder.noContent();
        } else {
            throw new ComicNotFoundException(comicId);
        }
    }

    /*****************************************************************************************************************
     * Comic Strip Image Retrieval Methods
     *****************************************************************************************************************/

    @GetMapping("/comics/{comic}/avatar")
    public @ResponseBody ResponseEntity<ImageDto> retrieveAvatar(@PathVariable(name = "comic") Integer comicId) {
        return comicManagementFacade.getAvatar(comicId)
                .map(imageDto -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                        .body(imageDto))
                .orElseThrow(() -> ComicImageNotFoundException.forAvatar(comicId));
    }

    @GetMapping("/comics/{comic}/strips/first")
    public @ResponseBody ResponseEntity<ComicNavigationResult> retrieveFirstComicImage(@PathVariable(name = "comic") Integer comicId) {
        // Log API entry with comic name
        String comicName = comicManagementFacade.getComic(comicId)
                .map(ComicItem::getName)
                .orElse("UNKNOWN");
        log.info("API /first called: comicId={}, comicName={}", comicId, comicName);

        ComicNavigationResult result = comicManagementFacade.getComicStrip(comicId, Direction.FORWARD);

        // Log API result
        log.info("API /first returning: found={}, currentDate={}, nearestPrev={}, nearestNext={}",
                result.isFound(), result.getCurrentDate(), result.getNearestPreviousDate(), result.getNearestNextDate());

        // Trigger predictive lookahead if result found and cache service is available
        if (result.isFound() && result.getCurrentDate() != null) {
            predictiveCacheService.ifPresent(service ->
                service.prefetchAdjacentComics(comicId, result.getCurrentDate(), Direction.FORWARD));
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS))
                .body(result);
    }

    @GetMapping("/comics/{comic}/next/{date}")
    public @ResponseBody ResponseEntity<ComicNavigationResult> retrieveNextComicImage(@PathVariable(name = "comic") Integer comicId, @PathVariable String date) {
        long startTime = System.currentTimeMillis();
        var from = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Log API entry with comic name
        String comicName = comicManagementFacade.getComic(comicId)
                .map(ComicItem::getName)
                .orElse("UNKNOWN");
        log.info("API /next/{} called: comicId={}, comicName={}, fromDate={}", date, comicId, comicName, from);

        ComicNavigationResult result = comicManagementFacade.getComicStrip(comicId, Direction.FORWARD, from);

        // Log API result
        log.info("API /next/{} returning: found={}, currentDate={}, nearestPrev={}, nearestNext={}",
                date, result.isFound(), result.getCurrentDate(), result.getNearestPreviousDate(), result.getNearestNextDate());

        // Track access metrics if available
        trackAccess(comicId, result, startTime);

        // Trigger predictive lookahead if result found and cache service is available
        if (result.isFound() && result.getCurrentDate() != null) {
            predictiveCacheService.ifPresent(service ->
                service.prefetchAdjacentComics(comicId, result.getCurrentDate(), Direction.FORWARD));
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS))
                .body(result);
    }

    @GetMapping("/comics/{comic}/previous/{date}")
    public @ResponseBody ResponseEntity<ComicNavigationResult> retrievePreviousComicImage(@PathVariable(name = "comic") Integer comicId, @PathVariable String date) {
        long startTime = System.currentTimeMillis();
        var from = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Log API entry with comic name
        String comicName = comicManagementFacade.getComic(comicId)
                .map(ComicItem::getName)
                .orElse("UNKNOWN");
        log.info("API /previous/{} called: comicId={}, comicName={}, fromDate={}", date, comicId, comicName, from);

        ComicNavigationResult result = comicManagementFacade.getComicStrip(comicId, Direction.BACKWARD, from);

        // Log API result
        log.info("API /previous/{} returning: found={}, currentDate={}, nearestPrev={}, nearestNext={}",
                date, result.isFound(), result.getCurrentDate(), result.getNearestPreviousDate(), result.getNearestNextDate());

        // Track access metrics if available
        trackAccess(comicId, result, startTime);

        // Trigger predictive lookahead if result found and cache service is available
        if (result.isFound() && result.getCurrentDate() != null) {
            predictiveCacheService.ifPresent(service ->
                service.prefetchAdjacentComics(comicId, result.getCurrentDate(), Direction.BACKWARD));
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS))
                .body(result);
    }

    @GetMapping("/comics/{comic}/strips/last")
    public @ResponseBody ResponseEntity<ComicNavigationResult> retrieveLastComicImage(@PathVariable(name = "comic") Integer comicId) {
        long startTime = System.currentTimeMillis();

        // Log API entry with comic name
        String comicName = comicManagementFacade.getComic(comicId)
                .map(ComicItem::getName)
                .orElse("UNKNOWN");
        log.info("API /last called: comicId={}, comicName={}", comicId, comicName);

        ComicNavigationResult result = comicManagementFacade.getComicStrip(comicId, Direction.BACKWARD);

        // Log API result
        log.info("API /last returning: found={}, currentDate={}, nearestPrev={}, nearestNext={}",
                result.isFound(), result.getCurrentDate(), result.getNearestPreviousDate(), result.getNearestNextDate());

        // Track access metrics if available
        trackAccess(comicId, result, startTime);

        // Trigger predictive lookahead if result found and cache service is available
        if (result.isFound() && result.getCurrentDate() != null) {
            predictiveCacheService.ifPresent(service ->
                service.prefetchAdjacentComics(comicId, result.getCurrentDate(), Direction.BACKWARD));
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS))
                .body(result);
    }

    /**
     * Track access metrics for comic strip retrieval
     */
    private void trackAccess(Integer comicId, ComicNavigationResult result, long startTime) {
        if (result.isFound() && accessMetricsCollector.isPresent()) {
            comicManagementFacade.getComic(comicId).ifPresent(comic -> {
                long accessTime = System.currentTimeMillis() - startTime;
                accessMetricsCollector.get().trackAccess(comic.getName(), true, accessTime);
            });
        }
    }
}