package org.stapledon.api.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.model.ComicImageNotFoundException;
import org.stapledon.engine.management.ManagementFacade;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for binary image endpoints.
 * 
 * This controller serves binary image data (avatars) that cannot be efficiently
 * served via GraphQL due to base64 encoding overhead.
 *
 * For comic metadata and strip navigation, use the GraphQL API (ComicResolver).
 * Strip images are served via URL references in GraphQL, with the actual binary
 * data fetched via the image URL.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping({ "/api/v1" })
public class ComicController {

        private final ManagementFacade comicManagementFacade;

        /**
         * Retrieve the avatar image for a comic.
         * 
         * @param comicId The comic ID
         * @return The avatar image as base64-encoded data with MIME type
         */
        @GetMapping("/comics/{comic}/avatar")
        public @ResponseBody ResponseEntity<ImageDto> retrieveAvatar(@PathVariable(name = "comic") Integer comicId) {
                return comicManagementFacade.getAvatar(comicId)
                                .map(imageDto -> ResponseEntity.ok()
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                                                .body(imageDto))
                                .orElseThrow(() -> ComicImageNotFoundException.forAvatar(comicId));
        }
}