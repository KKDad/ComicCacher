package org.stapledon.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.stapledon.downloader.ComicCacher;
import org.stapledon.dto.ComicItem;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

@Component
public class UpdateService implements IUpdateService
{
    private static final Logger logger = LoggerFactory.getLogger(UpdateService.class);


    @Override
    public boolean updateAll()
    {
        try {
            ComicCacher comicCacher = new ComicCacher();
            return comicCacher.cacheAll();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateComic(int comicId)
    {
        try {
            ComicCacher comicCacher = new ComicCacher();

            // Determine the comic to be updated
            ComicItem comic = ComicsService.getComics().stream().filter(p -> p.id == comicId).findFirst().orElse(null);
            if (comic == null)
                return false;
            return comicCacher.cacheSingle(comic);

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }
}
