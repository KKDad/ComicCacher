package org.stapledon.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stapledon.dto.ComicItem;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class UpdateController
{
    @Autowired
    private IComicUpdateService comicUpdateService;

    @RequestMapping(method=GET, path = "/api/v1/update")
    public ResponseEntity<String> updateAllComics()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        boolean result = comicUpdateService.updateAll();
        if (!result)
            return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(null, headers, HttpStatus.OK);
    }

    @RequestMapping(method=GET, path = "/api/v1/update/{comicId}")
    public ResponseEntity<String> updateSpecificComic(@PathVariable int comicId)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());


        boolean result = comicUpdateService.updateComic(comicId);
        if (!result)
            return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(null, headers, HttpStatus.OK);
    }
}