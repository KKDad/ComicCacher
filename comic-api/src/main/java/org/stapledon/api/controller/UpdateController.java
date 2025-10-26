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
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.model.ResponseBuilder;
import org.stapledon.engine.management.ManagementFacade;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1"})
@Tag(name = "Update Comics", description = "Update and Cache individual or all configured comics")
public class UpdateController {
    private final ManagementFacade comicManagementFacade;

    @GetMapping(path = "/update")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<ApiResponse<String>> updateAllComics() {
        var headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        boolean result = comicManagementFacade.updateAllComics();
        if (!result) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .headers(headers)
                    .body(ApiResponse.<String>error(HttpStatus.NOT_FOUND.value(), "Failed to update all comics"));
        }

        return ResponseBuilder.ok("Update initiated for all comics", "Comics update initiated successfully");
    }

    @GetMapping(path = "/update/{comicId}")
    @ResponseStatus(code = HttpStatus.OK)
    public ResponseEntity<ApiResponse<String>> updateSpecificComic(@PathVariable int comicId) {
        var headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        boolean result = comicManagementFacade.updateComic(comicId);
        if (!result) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .headers(headers)
                    .body(ApiResponse.<String>error(HttpStatus.NOT_FOUND.value(), "Comic not found"));
        }

        return ResponseBuilder.ok("Update initiated for comic ID: " + comicId, "Comic update initiated successfully");
    }
}