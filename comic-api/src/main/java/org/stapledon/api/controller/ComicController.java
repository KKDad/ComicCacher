package org.stapledon.api.controller;

import com.google.common.base.Stopwatch;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.stapledon.common.model.ComicImageNotFoundException;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.metrics.collector.AccessMetricsCollector;

import java.time.LocalDate;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for binary image endpoints.
 *
 * This controller serves binary image data (avatars) that cannot be efficiently
 * served via GraphQL due to base64 encoding overhead.
 *
 * For comic metadata and strip navigation, use the GraphQL API (ComicResolver).
 * Strip images are served via URL references in GraphQL, with the actual binary
 * data fetched via the image URL.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping({ "/api/v1" })
public class ComicController {

    private final ManagementFacade comicManagementFacade;
    private final AccessMetricsCollector accessMetricsCollector;

    /**
     * Retrieve the avatar image for a comic.
     */
    @GetMapping("/comics/{comic}/avatar")
    public @ResponseBody ResponseEntity<byte[]> retrieveAvatar(@PathVariable(name = "comic") Integer comicId) {
        var timer = Stopwatch.createStarted();
        return comicManagementFacade.getAvatar(comicId)
                .map(imageDto -> {
                    byte[] imageBytes = Base64.getDecoder().decode(imageDto.getImageData());
                    timer.stop();
                    trackAccess(comicId, true, timer.elapsed(TimeUnit.MILLISECONDS));
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(imageDto.getMimeType()))
                            .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(imageBytes.length))
                            .body(imageBytes);
                })
                .orElseThrow(() -> ComicImageNotFoundException.forAvatar(comicId));
    }

    /**
     * Retrieve the comic strip image for a specific date.
     */
    @GetMapping("/comics/{comic}/strip/{date}")
    public @ResponseBody ResponseEntity<byte[]> retrieveStrip(
            @PathVariable(name = "comic") Integer comicId,
            @PathVariable(name = "date") LocalDate date) {
        var timer = Stopwatch.createStarted();
        return comicManagementFacade.getComicStripOnDate(comicId, date)
                .map(imageDto -> {
                    byte[] imageBytes = Base64.getDecoder().decode(imageDto.getImageData());
                    timer.stop();
                    trackAccess(comicId, true, timer.elapsed(TimeUnit.MILLISECONDS));
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(imageDto.getMimeType()))
                            .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(imageBytes.length))
                            .body(imageBytes);
                })
                .orElseThrow(() -> new ComicImageNotFoundException(comicId, date));
    }

    /**
     * Track access metrics for a comic retrieval.
     */
    private void trackAccess(int comicId, boolean isHit, long accessTimeMs) {
        comicManagementFacade.getComic(comicId)
                .ifPresent(comic -> accessMetricsCollector.trackAccess(comic.getName(), isHit, accessTimeMs));
    }
}