package org.stapledon.core.comic.service;

import org.springframework.stereotype.Service;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.util.Direction;
import org.stapledon.core.comic.management.ComicManagementFacade;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComicsServiceImpl implements ComicsService {
    
    private final ComicManagementFacade comicManagementFacade;

    /**
     * Return details of all configured comics
     *
     * @return list of all configured comics
     */
    @Override
    public List<ComicItem> retrieveAll() {
        return comicManagementFacade.getAllComics();
    }

    /**
     * Return details of a specific comic
     *
     * @param comicId - Comic to lookup
     * @return Optional containing details of the comic, empty if not found
     */
    @Override
    public Optional<ComicItem> retrieveComic(int comicId) {
        Optional<ComicItem> comicOpt = comicManagementFacade.getComic(comicId);
        
        if (comicOpt.isEmpty()) {
            log.error("Unknown comic id={}", comicId);
        }
        
        return comicOpt;
    }

    @Override
    public Optional<ComicItem> createComic(int comicId, ComicItem comicItem) {
        return comicManagementFacade.createComic(comicItem);
    }

    @Override
    public Optional<ComicItem> updateComic(int comicId, ComicItem comicItem) {
        return comicManagementFacade.updateComic(comicId, comicItem);
    }

    @Override
    public boolean deleteComic(int comicId) {
        return comicManagementFacade.deleteComic(comicId);
    }

    /**
     * Returns the strip image for a specified comic
     *
     * @param comicId - Comic to retrieve
     * @param which   - Direction to retrieve from, either oldest or newest.
     * @return 200 with the image or 404 with no response body if not found
     */
    @Override
    public Optional<ImageDto> retrieveComicStrip(int comicId, Direction which) {
        log.info("Retrieving comic strip for comicId={}, Direction={}", comicId, which);
        return comicManagementFacade.getComicStrip(comicId, which);
    }

    @Override
    public Optional<ImageDto> retrieveComicStrip(int comicId, Direction which, LocalDate from) {
        log.info("Retrieving comic strip for comicId={}, Direction={}, from={}", comicId, which, from);
        return comicManagementFacade.getComicStrip(comicId, which, from);
    }

    /**
     * Returns the avatar for a specified comic
     *
     * @param comicId - Comic to retrieve
     * @return Optional containing the avatar image, empty if not found
     */
    @Override
    public Optional<ImageDto> retrieveAvatar(int comicId) {
        return comicManagementFacade.getAvatar(comicId);
    }
}