package org.stapledon.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.stapledon.dto.ComicItem;
import org.stapledon.dto.ImageDto;
import org.stapledon.utils.Direction;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@SuppressWarnings({"squid:S4488","squid:S00117"}) // API parameter names don't comply with Java naming conventions.
public class ComicController
{
    @Autowired
    private IComicsService comicsService;

    /*****************************************************************************************************************
     * Comic Strip Listing and Configuration
     *****************************************************************************************************************/

    @RequestMapping(method=GET, path = "/api/v1/comics")
    public List<ComicItem> retrieveAllComics()
    {
        return comicsService.retrieveAll();
    }

    @RequestMapping(method=GET, path = "/api/v1/comics/{comic_id}")
    public ComicItem retrieveComicDetails(@PathVariable String comic_id)
    {
        var comicId = Integer.parseInt(comic_id);
        var comicItem = comicsService.retrieveComic(comicId);
        if (comicItem != null)
            return comicItem;
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(method=POST, path = "/api/v1/comics/{comic_id}")
    public ComicItem createComicDetails(@RequestBody ComicItem comicItem, @PathVariable String comic_id)
    {
        var comicId = Integer.parseInt(comic_id);
        ComicItem resultItem =  comicsService.createComic(comicId, comicItem);
        if (resultItem != null)
            return resultItem;
        throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Unable to save ComicItem");

    }

    @RequestMapping(method=PATCH, path = "/api/v1/comics/{comic_id}")
    public ComicItem updateComicDetails(@RequestBody ComicItem comicItem, @PathVariable String comic_id)
    {
        var comicId = Integer.parseInt(comic_id);
        ComicItem resultItem =  comicsService.updateComic(comicId, comicItem);
        if (resultItem != null)
            return resultItem;
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to save ComicItem");
    }

    @RequestMapping(method=DELETE, path = "/api/v1/comics/{comic_id}")
    public void deleteComicDetails(@PathVariable String comic_id)
    {
        var comicId = Integer.parseInt(comic_id);
        boolean result = comicsService.deleteComic(comicId);
        if (result)
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "ComicItem has been removed");
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    /*****************************************************************************************************************
     * Comic Strip Image Retrieval Methods
     *****************************************************************************************************************/

    @RequestMapping(method=GET, path = "/api/v1/comics/{comic_id}/avatar")
    public @ResponseBody ResponseEntity<ImageDto> retrieveAvatar(@PathVariable String comic_id) throws IOException
    {
        var comicId = Integer.parseInt(comic_id);
        return comicsService.retrieveAvatar(comicId);
    }

    @RequestMapping(method=GET, path = "/api/v1/comics/{comic_id}/strips/first")
    public @ResponseBody ResponseEntity<ImageDto> retrieveFirstComicImage(@PathVariable String comic_id) throws IOException
    {
        var comicId = Integer.parseInt(comic_id);
        return comicsService.retrieveComicStrip(comicId, Direction.FORWARD);
    }

    @RequestMapping(method=GET, path = "/api/v1/comics/{comic_id}/next/{strip_reference}")
    public @ResponseBody ResponseEntity<ImageDto> retrieveNextComicImage(@PathVariable String comic_id, @PathVariable String strip_reference) throws IOException
    {
        var comicId = Integer.parseInt(comic_id);
        var from = LocalDate.parse(strip_reference, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return comicsService.retrieveComicStrip(comicId, Direction.FORWARD, from);
    }

    @RequestMapping(method=GET, path = "/api/v1/comics/{comic_id}/previous/{strip_reference}")
    public @ResponseBody ResponseEntity<ImageDto> retrievePreviousComicImage(@PathVariable String comic_id, @PathVariable String strip_reference) throws IOException
    {
        var comicId = Integer.parseInt(comic_id);
        var from = LocalDate.parse(strip_reference, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return comicsService.retrieveComicStrip(comicId, Direction.BACKWARD, from);
    }

    @RequestMapping(method=GET, path = "/api/v1/comics/{comic_id}/strips/last")
    public @ResponseBody ResponseEntity<ImageDto> retrieveLastComicImage(@PathVariable String comic_id) throws IOException
    {
        var comicId = Integer.parseInt(comic_id);
        return comicsService.retrieveComicStrip(comicId, Direction.BACKWARD);
    }

}