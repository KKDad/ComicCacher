package org.stapledon.core.comic.service;

import org.springframework.stereotype.Service;
import org.stapledon.core.comic.management.ComicManagementFacade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateServiceImpl implements UpdateService {

    private final ComicManagementFacade comicManagementFacade;
    
    @Override
    public boolean updateAll() {
        log.info("Updating all comics");
        return comicManagementFacade.updateAllComics();
    }

    @Override
    public boolean updateComic(int comicId) {
        log.info("Updating comic with id={}", comicId);
        return comicManagementFacade.updateComic(comicId);
    }
}