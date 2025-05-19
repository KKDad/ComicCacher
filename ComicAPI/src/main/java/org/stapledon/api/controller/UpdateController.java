package org.stapledon.api.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.stapledon.core.comic.management.ComicManagementFacade;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1"})
@Tag(name = "Update Comics", description = "Update and Cache individual or all configured comics")
public class UpdateController {
    private final ComicManagementFacade comicManagementFacade;

    @GetMapping(path = "/update")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<String> updateAllComics() {
        var headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        boolean result = comicManagementFacade.updateAllComics();
        if (!result)
            return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(null, headers, HttpStatus.OK);
    }

    @GetMapping(path = "/update/{comicId}")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<String> updateSpecificComic(@PathVariable int comicId) {
        var headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        boolean result = comicManagementFacade.updateComic(comicId);
        if (!result)
            return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(null, headers, HttpStatus.OK);
    }
}