package org.stapledon.api;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.stapledon.dto.ComicItem;
import org.stapledon.dto.ComicList;
import org.stapledon.dto.ImageDto;
import org.stapledon.utils.CacheUtils;
import org.stapledon.utils.Direction;
import org.stapledon.utils.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class ComicsService implements IComicsService
{
    static String cacheLocation;

    private static final Logger logger = Logger.getLogger(ComicsService.class.getName());

    static List<ComicItem> comics = new ArrayList<>();

    /**
     * Return details of a specific api
     *
     * @param comicId - Comic to lookup
     * @return details of the api
     */
    @Override
    public ComicItem retrieveComic(int comicId)
    {
        return comics.stream().filter(p -> p.id == comicId).findFirst().orElse(null);
    }

    /**
     * Return details of a all configured comics
     *
     * @return list of all configured comics
     */
    @Override
    public List<ComicItem> retrieveAll()
    {
        ComicList list = new ComicList();
        list.getComics().addAll(comics);
        Collections.sort(list.getComics());
        return list.getComics();
    }

    /**
     * Returns the strip image for a specified api
     * @param comicId - Comic to retrieve
     * @param which - Direction to retrive from, either oldest or newest.
     * @return 200 with the image or 404 with no response body if not found
     */
    @Override
    public ResponseEntity<ImageDto> retrieveComicStrip(int comicId, Direction which) throws IOException
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        ComicItem comic = comics.stream().filter(p -> p.id == comicId).findFirst().orElse(null);
        if (comic == null) {
            if (logger.isLoggable(Level.SEVERE))
                logger.log(Level.SEVERE, String.format("Unknown api id=%d, total known: %d", comicId, comics.size()));
            return new ResponseEntity<>(null, headers, HttpStatus.NOT_FOUND);
        }

        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        CacheUtils cacheUtils = new CacheUtils(cacheLocation);
        File oldest = cacheUtils.findFirst(comic, which);
        if (oldest == null) {
            if (logger.isLoggable(Level.SEVERE))
                logger.log(Level.SEVERE, String.format("Unable to locate first strip for %s", comic.name));
            return new ResponseEntity<>(null, headers, HttpStatus.NOT_FOUND);
        }

        ImageDto dto = ImageUtils.getImageDto(oldest);

        return new ResponseEntity<>(dto, headers, HttpStatus.OK);
    }

    /**
     * Returns the avatar for a specified api
     * @param comicId - Comic to retrieve
     * @return 200 with the image or 404 with no response body if not found
     */
    @Override
    public ResponseEntity<ImageDto> retrieveAvatar(int comicId)  throws IOException
    {
        HttpHeaders headers = new HttpHeaders();
        ComicItem comic = comics.stream().filter(p -> p.id == comicId).findFirst().orElse(null);
        if (comic == null)
            return new ResponseEntity<>(null, headers, HttpStatus.NOT_FOUND);

        String comicNameParsed = comic.name.replace(" ", "");


        File avatar = new File(String.format("%s/%s/avatar.png", cacheLocation, comicNameParsed));
        if (!avatar.exists()) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, String.format("Unable to locate avatar for %s", comic.name));
                logger.log(Level.SEVERE, String.format("   checked %s", avatar.getAbsolutePath()));
            }
            return new ResponseEntity<>(null, headers, HttpStatus.NOT_FOUND);
        }

        ImageDto dto = ImageUtils.getImageDto(avatar);

        return new ResponseEntity<>(dto, headers, HttpStatus.OK);
    }


}
