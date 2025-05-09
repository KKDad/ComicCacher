package org.stapledon.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.model.ResponseBuilder;
import org.stapledon.exceptions.ComicCachingException;
import org.stapledon.exceptions.ComicImageNotFoundException;
import org.stapledon.exceptions.ComicNotFoundException;
import org.stapledon.api.service.ComicsService;
import org.stapledon.dto.ComicItem;
import org.stapledon.dto.ImageDto;
import org.stapledon.utils.Direction;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1"})
public class ComicController {

    private final ComicsService comicsService;

    /*****************************************************************************************************************
     * Comic Strip Listing and Configuration
     *****************************************************************************************************************/

    @GetMapping("/comics")
    public ResponseEntity<ApiResponse<List<ComicItem>>> retrieveAllComics() {
        return ResponseBuilder.collection(comicsService.retrieveAll());
    }

    @GetMapping("/comics/{comic}")
    public ResponseEntity<ApiResponse<ComicItem>> retrieveComicDetails(@PathVariable String comic) {
        var comicId = Integer.parseInt(comic);
        return comicsService.retrieveComic(comicId)
                .map(ResponseBuilder::ok)
                .orElseThrow(() -> new ComicNotFoundException(comicId));
    }

    @PostMapping("/comics/{comic}")
    public ResponseEntity<ApiResponse<ComicItem>> createComicDetails(@RequestBody ComicItem comicItem, @PathVariable String comic) {
        var comicId = Integer.parseInt(comic);
        return comicsService.createComic(comicId, comicItem)
                .map(ResponseBuilder::created)
                .orElseThrow(() -> new ComicCachingException("Unable to save ComicItem"));
    }

    @PatchMapping("/comics/{comic}")
    public ResponseEntity<ApiResponse<ComicItem>> updateComicDetails(@PathVariable String comic, @RequestBody ComicItem comicItem) {
        var comicId = Integer.parseInt(comic);
        return comicsService.updateComic(comicId, comicItem)
                .map(ResponseBuilder::ok)
                .orElseThrow(() -> new ComicCachingException("Unable to update ComicItem"));
    }

    @DeleteMapping("/comics/{comic}")
    public ResponseEntity<Void> deleteComicDetails(@PathVariable String comic) {
        var comicId = Integer.parseInt(comic);
        boolean result = comicsService.deleteComic(comicId);

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
    public @ResponseBody ResponseEntity<ImageDto> retrieveAvatar(@PathVariable String comic) throws IOException {
        var comicId = Integer.parseInt(comic);
        return comicsService.retrieveAvatar(comicId)
                .map(imageDto -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                        .body(imageDto))
                .orElseThrow(() -> ComicImageNotFoundException.forAvatar(comicId));
    }

    @GetMapping("/comics/{comic}/strips/first")
    public @ResponseBody ResponseEntity<ImageDto> retrieveFirstComicImage(@PathVariable String comic) throws IOException {
        var comicId = Integer.parseInt(comic);
        return comicsService.retrieveComicStrip(comicId, Direction.FORWARD)
                .map(imageDto -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS))
                        .body(imageDto))
                .orElseThrow(() -> new ComicImageNotFoundException("First comic strip for comic ID " + comicId + " could not be found"));
    }

    @GetMapping("/comics/{comic}/next/{date}")
    public @ResponseBody ResponseEntity<ImageDto> retrieveNextComicImage(@PathVariable String comic, @PathVariable String date) throws IOException {
        var comicId = Integer.parseInt(comic);
        var from = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return comicsService.retrieveComicStrip(comicId, Direction.FORWARD, from)
                .map(imageDto -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS))
                        .body(imageDto))
                .orElseThrow(() -> new ComicImageNotFoundException(comicId, from));
    }

    @GetMapping("/comics/{comic}/previous/{date}")
    public @ResponseBody ResponseEntity<ImageDto> retrievePreviousComicImage(@PathVariable String comic, @PathVariable String date) throws IOException {
        var comicId = Integer.parseInt(comic);
        var from = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return comicsService.retrieveComicStrip(comicId, Direction.BACKWARD, from)
                .map(imageDto -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS))
                        .body(imageDto))
                .orElseThrow(() -> new ComicImageNotFoundException(comicId, from));
    }

    @GetMapping("/comics/{comic}/strips/last")
    public @ResponseBody ResponseEntity<ImageDto> retrieveLastComicImage(@PathVariable String comic) throws IOException {
        var comicId = Integer.parseInt(comic);
        return comicsService.retrieveComicStrip(comicId, Direction.BACKWARD)
                .map(imageDto -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS))
                        .body(imageDto))
                .orElseThrow(() -> new ComicImageNotFoundException("Last comic strip for comic ID " + comicId + " could not be found"));
    }

}