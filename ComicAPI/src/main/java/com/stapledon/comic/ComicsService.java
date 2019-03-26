package com.stapledon.comic;

import com.stapledon.cache.CacheUtils;
import com.stapledon.cache.Direction;
import com.stapledon.interop.ComicItem;
import com.stapledon.interop.ComicList;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class ComicsService
{
    public int i = 54;
    static String cacheLocation;

    private static final Logger logger = Logger.getLogger(ComicsService.class.getName());

    static List<ComicItem> comics = new ArrayList<>();

    /**
     * Return details of a specific comic
     *
     * @param comicId - Comic to lookup
     * @return details of the comic
     */
    ComicItem retrieveComic(String comicId)
    {
        int i = Integer.parseInt(comicId);
        return comics.stream().filter(p -> p.id == i).findFirst().orElse(null);
    }

    /**
     * Return details of a all configured comics
     *
     * @return list of all configured comics
     */
    List<ComicItem> retrieveAll()
    {
        ComicList list = new ComicList();
        list.getComics().addAll(comics);
        Collections.sort(list.getComics());
        return list.getComics();
    }

    /**
     * Returns the strip image for a specified comic
     * @param comicId - Comic to retrieve
     * @param which - Direction to retrive from, either oldest or newest.
     * @return 200 with the image or 404 with no response body if not found
     */
    ResponseEntity<byte[]> retrieveComicStrip(String comicId, Direction which) throws IOException
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        int i = Integer.parseInt(comicId);
        ComicItem comic = comics.stream().filter(p -> p.id == i).findFirst().orElse(null);
        if (comic == null) {
            if (logger.isLoggable(Level.SEVERE))
                logger.log(Level.SEVERE, String.format("Unknown comic id=%d, total known: %d", i, comics.size()));
            return new ResponseEntity<>(null, headers, HttpStatus.NOT_FOUND);
        }

        headers.setContentType(MediaType.IMAGE_JPEG);
        CacheUtils cacheUtils = new CacheUtils(cacheLocation);
        File oldest = cacheUtils.findFirst(comic, which);
        if (oldest == null) {
            if (logger.isLoggable(Level.SEVERE))
                logger.log(Level.SEVERE, String.format("Unable to locate first strip for %s", comic.name));
            return new ResponseEntity<>(null, headers, HttpStatus.NOT_FOUND);
        }

        byte[] media = Files.readAllBytes(oldest.toPath());

        return new ResponseEntity<>(media, headers, HttpStatus.OK);
    }
}
