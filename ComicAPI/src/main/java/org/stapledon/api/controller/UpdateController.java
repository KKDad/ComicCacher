package org.stapledon.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stapledon.api.UpdateService;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1"})
public class UpdateController {
    private final UpdateService updateService;

    @GetMapping(path = "/update")
    public ResponseEntity<String> updateAllComics() {
        var headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        boolean result = updateService.updateAll();
        if (!result)
            return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(null, headers, HttpStatus.OK);
    }

    @GetMapping(path = "/update/{comicId}")
    public ResponseEntity<String> updateSpecificComic(@PathVariable int comicId) {
        var headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());


        boolean result = updateService.updateComic(comicId);
        if (!result)
            return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(null, headers, HttpStatus.OK);
    }
}