package org.stapledon.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.stapledon.downloader.ComicCacher;
import org.stapledon.dto.ComicItem;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
public class UpdateServiceImpl implements UpdateService
{
    @Override
    public boolean updateAll()
    {
        try {
            var comicCacher = new ComicCacher();
            return comicCacher.cacheAll();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateComic(int comicId)
    {
        try {
            var comicCacher = new ComicCacher();

            // Determine the comic to be updated
            ComicItem comic = ComicsServiceImpl.getComics().stream().filter(p -> p.id == comicId).findFirst().orElse(null);
            if (comic == null)
                return false;
            return comicCacher.cacheSingle(comic);

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
