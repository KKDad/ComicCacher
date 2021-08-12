package org.stapledon.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UpdateController
{
    @Autowired
    private IUpdateService updateService;

    @GetMapping(path = "/api/v1/update")
    public ResponseEntity<String> updateAllComics()
    {
        var headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        boolean result = updateService.updateAll();
        if (!result)
            return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(null, headers, HttpStatus.OK);
    }

    @GetMapping(path = "/api/v1/update/{comicId}")
    public ResponseEntity<String> updateSpecificComic(@PathVariable int comicId)
    {
        var headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());


        boolean result = updateService.updateComic(comicId);
        if (!result)
            return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(null, headers, HttpStatus.OK);
    }
}