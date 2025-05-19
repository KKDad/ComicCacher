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
import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.api.dto.comic.ImageDto;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.model.ResponseBuilder;
import org.stapledon.common.util.Direction;
import org.stapledon.core.comic.management.ComicManagementFacade;
import org.stapledon.core.comic.model.ComicCachingException;
import org.stapledon.core.comic.model.ComicImageNotFoundException;
import org.stapledon.core.comic.model.ComicNotFoundException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1"})
public class ComicController {

    private final ComicManagementFacade comicManagementFacade;

    /*****************************************************************************************************************
     * Comic Strip Listing and Configuration
     *****************************************************************************************************************/

    @GetMapping("/comics")
    public ResponseEntity<ApiResponse<List<ComicItem>>> retrieveAllComics() {
        return ResponseBuilder.collection(comicManagementFacade.getAllComics());
    }

    @GetMapping("/comics/{comic}")
    public ResponseEntity<ApiResponse<ComicItem>> retrieveComicDetails(@PathVariable String comic) {
        var comicId = Integer.parseInt(comic);
        return comicManagementFacade.getComic(comicId)
                .map(ResponseBuilder::ok)
                .orElseThrow(() -> new ComicNotFoundException(comicId));
    }

    @PostMapping("/comics/{comic}")
    public ResponseEntity<ApiResponse<ComicItem>> createComicDetails(@RequestBody ComicItem comicItem, @PathVariable String comic) {
        var comicId = Integer.parseInt(comic);
        
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
    public ResponseEntity<ApiResponse<ComicItem>> updateComicDetails(@PathVariable String comic, @RequestBody ComicItem comicItem) {
        var comicId = Integer.parseInt(comic);
        return comicManagementFacade.updateComic(comicId, comicItem)
                .map(ResponseBuilder::ok)
                .orElseThrow(() -> new ComicCachingException("Unable to update ComicItem"));
    }

    @DeleteMapping("/comics/{comic}")
    public ResponseEntity<Void> deleteComicDetails(@PathVariable String comic) {
        var comicId = Integer.parseInt(comic);
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
    public @ResponseBody ResponseEntity<ImageDto> retrieveAvatar(@PathVariable String comic) {
        var comicId = Integer.parseInt(comic);
        return comicManagementFacade.getAvatar(comicId)
                .map(imageDto -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                        .body(imageDto))
                .orElseThrow(() -> ComicImageNotFoundException.forAvatar(comicId));
    }

    @GetMapping("/comics/{comic}/strips/first")
    public @ResponseBody ResponseEntity<ImageDto> retrieveFirstComicImage(@PathVariable String comic) {
        var comicId = Integer.parseInt(comic);
        return comicManagementFacade.getComicStrip(comicId, Direction.FORWARD)
                .map(imageDto -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS))
                        .body(imageDto))
                .orElseThrow(() -> new ComicImageNotFoundException("First comic strip for comic ID " + comicId + " could not be found"));
    }

    @GetMapping("/comics/{comic}/next/{date}")
    public @ResponseBody ResponseEntity<ImageDto> retrieveNextComicImage(@PathVariable String comic, @PathVariable String date) {
        var comicId = Integer.parseInt(comic);
        var from = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return comicManagementFacade.getComicStrip(comicId, Direction.FORWARD, from)
                .map(imageDto -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS))
                        .body(imageDto))
                .orElseThrow(() -> new ComicImageNotFoundException(comicId, from));
    }

    @GetMapping("/comics/{comic}/previous/{date}")
    public @ResponseBody ResponseEntity<ImageDto> retrievePreviousComicImage(@PathVariable String comic, @PathVariable String date) {
        var comicId = Integer.parseInt(comic);
        var from = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return comicManagementFacade.getComicStrip(comicId, Direction.BACKWARD, from)
                .map(imageDto -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS))
                        .body(imageDto))
                .orElseThrow(() -> new ComicImageNotFoundException(comicId, from));
    }

    @GetMapping("/comics/{comic}/strips/last")
    public @ResponseBody ResponseEntity<ImageDto> retrieveLastComicImage(@PathVariable String comic) {
        var comicId = Integer.parseInt(comic);
        return comicManagementFacade.getComicStrip(comicId, Direction.BACKWARD)
                .map(imageDto -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS))
                        .body(imageDto))
                .orElseThrow(() -> new ComicImageNotFoundException("Last comic strip for comic ID " + comicId + " could not be found"));
    }
}