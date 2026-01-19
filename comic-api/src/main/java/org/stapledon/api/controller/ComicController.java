package org.stapledon.api.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.model.ComicImageNotFoundException;
import org.stapledon.common.util.Direction;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.infrastructure.caching.PredictiveCacheService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for binary image endpoints.
 * These endpoints return binary image data that cannot be served via GraphQL.
 * 
 * For comic CRUD operations, use the GraphQL API (ComicResolver).
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping({ "/api/v1" })
public class ComicController {

        private final ManagementFacade comicManagementFacade;
        private final Optional<PredictiveCacheService> predictiveCacheService;
        private final Optional<org.stapledon.metrics.collector.AccessMetricsCollector> accessMetricsCollector;

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
        public @ResponseBody ResponseEntity<ComicNavigationResult> retrieveFirstComicImage(
                        @PathVariable(name = "comic") Integer comicId) {
                String comicName = comicManagementFacade.getComic(comicId)
                                .map(ComicItem::getName)
                                .orElse("UNKNOWN");
                log.info("API /first called: comicId={}, comicName={}", comicId, comicName);

                ComicNavigationResult result = comicManagementFacade.getComicStrip(comicId, Direction.FORWARD);

                log.info("API /first returning: found={}, currentDate={}, nearestPrev={}, nearestNext={}",
                                result.isFound(), result.getCurrentDate(), result.getNearestPreviousDate(),
                                result.getNearestNextDate());

                if (result.isFound() && result.getCurrentDate() != null) {
                        predictiveCacheService.ifPresent(
                                        service -> service.prefetchAdjacentComics(comicId, result.getCurrentDate(),
                                                        Direction.FORWARD));
                }

                return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS))
                                .body(result);
        }

        @GetMapping("/comics/{comic}/next/{date}")
        public @ResponseBody ResponseEntity<ComicNavigationResult> retrieveNextComicImage(
                        @PathVariable(name = "comic") Integer comicId,
                        @PathVariable String date) {
                long startTime = System.currentTimeMillis();
                var from = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                String comicName = comicManagementFacade.getComic(comicId)
                                .map(ComicItem::getName)
                                .orElse("UNKNOWN");
                log.info("API /next/{} called: comicId={}, comicName={}, fromDate={}", date, comicId, comicName, from);

                ComicNavigationResult result = comicManagementFacade.getComicStrip(comicId, Direction.FORWARD, from);

                log.info("API /next/{} returning: found={}, currentDate={}, nearestPrev={}, nearestNext={}",
                                date, result.isFound(), result.getCurrentDate(), result.getNearestPreviousDate(),
                                result.getNearestNextDate());

                trackAccess(comicId, result, startTime);

                if (result.isFound() && result.getCurrentDate() != null) {
                        predictiveCacheService.ifPresent(
                                        service -> service.prefetchAdjacentComics(comicId, result.getCurrentDate(),
                                                        Direction.FORWARD));
                }

                return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS))
                                .body(result);
        }

        @GetMapping("/comics/{comic}/previous/{date}")
        public @ResponseBody ResponseEntity<ComicNavigationResult> retrievePreviousComicImage(
                        @PathVariable(name = "comic") Integer comicId,
                        @PathVariable String date) {
                long startTime = System.currentTimeMillis();
                var from = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                String comicName = comicManagementFacade.getComic(comicId)
                                .map(ComicItem::getName)
                                .orElse("UNKNOWN");
                log.info("API /previous/{} called: comicId={}, comicName={}, fromDate={}", date, comicId, comicName,
                                from);

                ComicNavigationResult result = comicManagementFacade.getComicStrip(comicId, Direction.BACKWARD, from);

                log.info("API /previous/{} returning: found={}, currentDate={}, nearestPrev={}, nearestNext={}",
                                date, result.isFound(), result.getCurrentDate(), result.getNearestPreviousDate(),
                                result.getNearestNextDate());

                trackAccess(comicId, result, startTime);

                if (result.isFound() && result.getCurrentDate() != null) {
                        predictiveCacheService.ifPresent(
                                        service -> service.prefetchAdjacentComics(comicId, result.getCurrentDate(),
                                                        Direction.BACKWARD));
                }

                return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS))
                                .body(result);
        }

        @GetMapping("/comics/{comic}/strips/last")
        public @ResponseBody ResponseEntity<ComicNavigationResult> retrieveLastComicImage(
                        @PathVariable(name = "comic") Integer comicId) {
                long startTime = System.currentTimeMillis();

                String comicName = comicManagementFacade.getComic(comicId)
                                .map(ComicItem::getName)
                                .orElse("UNKNOWN");
                log.info("API /last called: comicId={}, comicName={}", comicId, comicName);

                ComicNavigationResult result = comicManagementFacade.getComicStrip(comicId, Direction.BACKWARD);

                log.info("API /last returning: found={}, currentDate={}, nearestPrev={}, nearestNext={}",
                                result.isFound(), result.getCurrentDate(), result.getNearestPreviousDate(),
                                result.getNearestNextDate());

                trackAccess(comicId, result, startTime);

                if (result.isFound() && result.getCurrentDate() != null) {
                        predictiveCacheService.ifPresent(
                                        service -> service.prefetchAdjacentComics(comicId, result.getCurrentDate(),
                                                        Direction.BACKWARD));
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