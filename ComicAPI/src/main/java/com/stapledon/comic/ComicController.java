package com.stapledon.comic;

import com.stapledon.interop.ComicItem;
import com.stapledon.interop.ComicList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class ComicController
{
    @Autowired
    private ComicsService comicsService;

    @RequestMapping(method=GET, path = "/comics/v1/list")
    public List<ComicItem> getAll()
    {
        return comicsService.retrieveAll();
    }

    @RequestMapping(method=GET, path = "/comics/v1/list/{id}")
    public ComicItem getSpecific(@PathVariable String id)
    {
        return comicsService.retrieveComic(id);
    }
}