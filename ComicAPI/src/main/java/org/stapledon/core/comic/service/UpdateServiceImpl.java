package org.stapledon.core.comic.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stapledon.core.comic.downloader.ComicCacher;
import org.stapledon.api.dto.comic.ComicItem;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateServiceImpl implements UpdateService {

    private final ComicCacher comicCacher;
    @Override
    public boolean updateAll() {
        return comicCacher.cacheAll();
    }

    @Override
    public boolean updateComic(int comicId) {
        // Determine the comic to be updated
        ComicItem comic = ComicsServiceImpl.getComics().stream().filter(p -> p.getId() == comicId).findFirst().orElse(null);
        if (comic == null)
            return false;
        return comicCacher.cacheSingle(comic);
    }
}
