package org.stapledon.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stapledon.downloader.ComicCacher;
import org.stapledon.dto.ComicItem;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateServiceImpl implements UpdateService {

    private final ComicCacher comicCacher;
    private final ComicsService comicsService;

    @Override
    public boolean updateAll() {
        return comicCacher.cacheAll();
    }

    @Override
    public boolean updateComic(int comicId) {
        // Determine the comic to be updated
        ComicItem comic = comicsService.getComics().stream().filter(p -> p.getId() == comicId).findFirst().orElse(null);
        if (comic == null)
            return false;
        return comicCacher.cacheSingle(comic);
    }
}
