package org.stapledon.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.stapledon.api.service.ComicsService;
import org.stapledon.dto.ComicItem;
import org.stapledon.dto.ImageDto;
import org.stapledon.utils.Direction;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1"})
public class ComicController {
    @Autowired
    private ComicsService comicsService;

    /*****************************************************************************************************************
     * Comic Strip Listing and Configuration
     *****************************************************************************************************************/

    @GetMapping("/comics")
    public List<ComicItem> retrieveAllComics() {
        return comicsService.retrieveAll();
    }

    @GetMapping("/comics/{comic}")
    public ComicItem retrieveComicDetails(@PathVariable String comic) {
        var comicId = Integer.parseInt(comic);
        var comicItem = comicsService.retrieveComic(comicId);
        if (comicItem != null)
            return comicItem;
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/comics/{comic}")
    public ComicItem createComicDetails(@RequestBody ComicItem comicItem, @PathVariable String comic) {
        var comicId = Integer.parseInt(comic);
        ComicItem resultItem = comicsService.createComic(comicId, comicItem);
        if (resultItem != null)
            return resultItem;
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to save ComicItem");
    }

    @PatchMapping("/comics/{comic}")
    public ComicItem updateComicDetails(@PathVariable String comic, @RequestBody ComicItem comicItem) {
        var comicId = Integer.parseInt(comic);
        ComicItem resultItem = comicsService.updateComic(comicId, comicItem);
        if (resultItem != null)
            return resultItem;
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to save ComicItem");
    }

    @DeleteMapping("/comics/{comic}")
    public void deleteComicDetails(@PathVariable String comic) {
        var comicId = Integer.parseInt(comic);
        boolean result = comicsService.deleteComic(comicId);
        if (result)
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "ComicItem has been removed");
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    /*****************************************************************************************************************
     * Comic Strip Image Retrieval Methods
     *****************************************************************************************************************/

    @GetMapping("/comics/{comic}/avatar")
    public @ResponseBody ResponseEntity<ImageDto> retrieveAvatar(@PathVariable String comic) throws IOException {
        var comicId = Integer.parseInt(comic);
        return comicsService.retrieveAvatar(comicId);
    }

    @GetMapping("/comics/{comic}/strips/first")
    public @ResponseBody ResponseEntity<ImageDto> retrieveFirstComicImage(@PathVariable String comic) throws IOException {
        var comicId = Integer.parseInt(comic);
        return comicsService.retrieveComicStrip(comicId, Direction.FORWARD);
    }

    @GetMapping("/comics/{comic}/next/{date}")
    public @ResponseBody ResponseEntity<ImageDto> retrieveNextComicImage(@PathVariable String comic, @PathVariable String date) throws IOException {
        var comicId = Integer.parseInt(comic);
        var from = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return comicsService.retrieveComicStrip(comicId, Direction.FORWARD, from);
    }

    @GetMapping("/comics/{comic}/previous/{date}")
    public @ResponseBody ResponseEntity<ImageDto> retrievePreviousComicImage(@PathVariable String comic, @PathVariable String date) throws IOException {
        var comicId = Integer.parseInt(comic);
        var from = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return comicsService.retrieveComicStrip(comicId, Direction.BACKWARD, from);
    }

    @GetMapping("/comics/{comic}/strips/last")
    public @ResponseBody ResponseEntity<ImageDto> retrieveLastComicImage(@PathVariable String comic) throws IOException {
        var comicId = Integer.parseInt(comic);
        return comicsService.retrieveComicStrip(comicId, Direction.BACKWARD);
    }

}