package org.stapledon.api;

import org.springframework.stereotype.Component;
import org.stapledon.downloader.ComicCacher;
import org.stapledon.dto.ComicItem;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class UpdateService implements IUpdateService
{
    private static final Logger logger = Logger.getLogger(UpdateService.class.getName());


    @Override
    public boolean updateAll()
    {
        try {
            ComicCacher comicCacher = new ComicCacher();
            return comicCacher.cacheAll();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
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

            return comicCacher.cacheComic(comic);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return false;
    }
}
