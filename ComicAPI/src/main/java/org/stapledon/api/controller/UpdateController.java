package org.stapledon.api.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stapledon.api.service.UpdateService;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1"})
@Tag(name = "Update Comics", description = "Update and Cache individual or all configured comics")
public class UpdateController {
    private final UpdateService updateService;

    @GetMapping(path = "/update")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<String> updateAllComics() {
        var headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        boolean result = updateService.updateAll();
        if (!result)
            return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(null, headers, HttpStatus.OK);
    }

    @GetMapping(path = "/update/{comicId}")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<String> updateSpecificComic(@PathVariable int comicId) {
        var headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());


        boolean result = updateService.updateComic(comicId);
        if (!result)
            return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(null, headers, HttpStatus.OK);
    }
}