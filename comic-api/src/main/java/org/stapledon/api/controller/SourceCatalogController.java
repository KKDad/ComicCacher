package org.stapledon.api.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stapledon.engine.source.CatalogThumbnailService;

import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

/**
 * Serves cached source catalog thumbnails for the Sources page. Only what is already on disk is served: downloads are started by the
 * {@code requestCatalogThumbnails} mutation, which needs a signed-in operator, so an anonymous request can never make the server fetch anything.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping({ "/api/v1" })
public class SourceCatalogController {

    private final CatalogThumbnailService thumbnails;

    /**
     * A catalog thumbnail, or 404 when it hasn't been downloaded.
     */
    @GetMapping("/sources/{source}/thumbnails/{identifier}")
    public ResponseEntity<byte[]> retrieveThumbnail(@PathVariable String source, @PathVariable String identifier) {
        return thumbnails.load(source, identifier)
                .map(thumbnail -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(thumbnail.mediaType()))
                        .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(thumbnail.data().length))
                        .body(thumbnail.data()))
                .orElseGet(() -> ResponseEntity.notFound().cacheControl(CacheControl.noStore()).build());
    }
}
