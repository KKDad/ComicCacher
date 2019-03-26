package com.stapledon.comic;

import com.stapledon.cache.Direction;
import com.stapledon.interop.ComicItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class ComicController
{
    @Autowired
    private ComicsService comicsService;

    @RequestMapping(method=GET, path = "/api/v1/comics")
    public List<ComicItem> retrieveAllComics()
    {
        return comicsService.retrieveAll();
    }

    @RequestMapping(method=GET, path = "/api/v1/comic/{comic_id}")
    public ComicItem retrieveComicDetails(@PathVariable String comic_id)
    {
        return comicsService.retrieveComic(comic_id);
    }

    @RequestMapping(method=GET, path = "/api/v1/comic/{comic_id}/strips/first")
    public @ResponseBody ResponseEntity<byte[]> retrieveFirstComicImage(@PathVariable String comic_id) throws IOException
    {
        return comicsService.retrieveComicStrip(comic_id, Direction.FORWARD);
    }

    @RequestMapping(method=GET, path = "/api/v1/comic/{comic_id}/strips/last")
    public @ResponseBody ResponseEntity<byte[]> retrieveLastComicImage(@PathVariable String comic_id) throws IOException
    {
        return comicsService.retrieveComicStrip(comic_id, Direction.BACKWARD);
    }

}