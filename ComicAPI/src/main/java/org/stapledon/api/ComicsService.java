package org.stapledon.api;

import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class ComicsService implements IComicsService
{
    static String cacheLocation;

    private static List<ComicItem> comics = new ArrayList<>();
    public  static List<ComicItem> getComics() { return comics; }

    /**
     * Return details of a all configured comics
     *
     * @return list of all configured comics
     */
    @Override
    public List<ComicItem> retrieveAll()
    {
        var list = new ComicList();
        list.getComics().addAll(comics);
        Collections.sort(list.getComics());
        return list.getComics();
    }

    /**
     * Return details of a specific api
     *
     * @param comicId - Comic to lookup
     * @return details of the api
     */
    @Override
    public ComicItem retrieveComic(int comicId)
    {
        var comic = comics.stream().filter(p -> p.id == comicId).findFirst().orElse(null);
        if (comic == null)
            log.error("Unknown comic id={}, total known: {}", comicId, comics.size());
        return comic;
    }

    @Override
    public ComicItem createComic(int comicId, ComicItem comicItem) {
        if (comics.contains(comicItem))
            return null;
        comics.add(comicItem);
        return comicItem;
    }

    @Override
    public ComicItem updateComic(int comicId, ComicItem comicItem) {
        comics.add(comicItem);
        return comicItem;
    }

    @Override
    public boolean deleteComic(int comicId) {

       ComicItem comic = comics.stream().filter(p -> p.id == comicId).findFirst().orElse(null);
       return comics.remove(comic);
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
        log.trace("Entering retrieveComicStrip for comicId={}, Direction={}", comicId, which);
        var headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        ComicItem comic = this.retrieveComic(comicId);
        if (comic == null)
            return new ResponseEntity<>(null, headers, HttpStatus.NOT_FOUND);

        var cacheUtils = new CacheUtils(cacheLocation);
        File image = cacheUtils.findFirst(comic, which);
        if (image == null) {
            log.error("Unable to locate first strip for {}", comic.name);
            return new ResponseEntity<>(null, headers, HttpStatus.NOT_FOUND);
        }

        headers.setContentType(MediaType.APPLICATION_JSON);
        var dto = ImageUtils.getImageDto(image);
        return new ResponseEntity<>(dto, headers, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ImageDto> retrieveComicStrip(int comicId, Direction which, LocalDate from) throws IOException
    {
        log.trace("Entering retrieveComicStrip for comicId={}, Direction={}, from={}", comicId, which, from);
        var headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        ComicItem comic = this.retrieveComic(comicId);
        if (comic == null)
            return new ResponseEntity<>(null, headers, HttpStatus.NOT_FOUND);

        var cacheUtils = new CacheUtils(cacheLocation);
        File image;
        if (which == Direction.FORWARD)
            image = cacheUtils.findNext(comic, from);
        else
            image = cacheUtils.findPrevious(comic, from);
        if (image == null) {
            log.error("Unable to locate first strip for {}", comic.name);
            return new ResponseEntity<>(null, headers, HttpStatus.NOT_FOUND);
        }

        headers.setContentType(MediaType.APPLICATION_JSON);
        var dto = ImageUtils.getImageDto(image);
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
        var headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        ComicItem comic = this.retrieveComic(comicId);
        if (comic == null)
            return new ResponseEntity<>(null, headers, HttpStatus.NOT_FOUND);

        String comicNameParsed = comic.name.replace(" ", "");
        var avatar = new File(String.format("%s/%s/avatar.png", cacheLocation, comicNameParsed));
        if (!avatar.exists()) {
            log.error("Unable to locate avatar for {}", comic.name);
            log.error("   checked {}", avatar.getAbsolutePath());
            return new ResponseEntity<>(null, headers, HttpStatus.NOT_FOUND);
        }

        headers.setContentType(MediaType.APPLICATION_JSON);
        var dto = ImageUtils.getImageDto(avatar);
        return new ResponseEntity<>(dto, headers, HttpStatus.OK);
    }
}
