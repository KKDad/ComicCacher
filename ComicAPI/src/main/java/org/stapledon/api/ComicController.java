package org.stapledon.api;

import com.google.common.io.Files;
import org.stapledon.dto.ImageDto;
import org.stapledon.utils.Direction;
import org.stapledon.dto.ComicItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@SuppressWarnings({"squid:S4488","squid:S00117"}) // API parameter names don't comply with Java naming conventions.
public class ComicController
{
    @Autowired
    private IComicsService iComicsService;

    @RequestMapping(method=GET, path = "/api/v1/comics")
    public List<ComicItem> retrieveAllComics()
    {
        return iComicsService.retrieveAll();
    }

    @RequestMapping(method=GET, path = "/api/v1/comics/{comic_id}")
    public ComicItem retrieveComicDetails(@PathVariable String comic_id)
    {
        int comicId = Integer.parseInt(comic_id);
        return iComicsService.retrieveComic(comicId);
    }

    @RequestMapping(method=GET, path = "/api/v1/comics/{comic_id}/avatar")
    public @ResponseBody ResponseEntity<ImageDto> retrieveAvatar(@PathVariable String comic_id) throws IOException
    {
        int comicId = Integer.parseInt(comic_id);
        return iComicsService.retrieveAvatar(comicId);
    }

    @RequestMapping(method=GET, path = "/api/v1/comics/{comic_id}/strips/first")
    public @ResponseBody ResponseEntity<ImageDto> retrieveFirstComicImage(@PathVariable String comic_id) throws IOException
    {
        int comicId = Integer.parseInt(comic_id);
        return iComicsService.retrieveComicStrip(comicId, Direction.FORWARD);
    }

    @RequestMapping(method=GET, path = "/api/v1/comics/{comic_id}/strips/next{strip_reference}")
    public @ResponseBody ResponseEntity<ImageDto> retrieveNextComicImage(@PathVariable String comic_id, @PathVariable String fromDate) throws IOException
    {
        int comicId = Integer.parseInt(comic_id);
        LocalDate from = LocalDate.parse(fromDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return iComicsService.retrieveComicStrip(comicId, Direction.FORWARD, from);
    }

    @RequestMapping(method=GET, path = "/api/v1/comics/{comic_id}/strips/previous{strip_reference}")
    public @ResponseBody ResponseEntity<ImageDto> retrievePreviousComicImage(@PathVariable String comic_id, @PathVariable String fromDate) throws IOException
    {
        int comicId = Integer.parseInt(comic_id);
        LocalDate from = LocalDate.parse(fromDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return iComicsService.retrieveComicStrip(comicId, Direction.BACKWARD, from);
    }

    @RequestMapping(method=GET, path = "/api/v1/comics/{comic_id}/strips/last")
    public @ResponseBody ResponseEntity<ImageDto> retrieveLastComicImage(@PathVariable String comic_id) throws IOException
    {
        int comicId = Integer.parseInt(comic_id);
        return iComicsService.retrieveComicStrip(comicId, Direction.BACKWARD);
    }

}