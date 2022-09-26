package org.stapledon.api;

import org.springframework.http.ResponseEntity;
import org.stapledon.dto.ComicItem;
import org.stapledon.dto.ImageDto;
import org.stapledon.utils.Direction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface ComicsService {
    List<ComicItem> retrieveAll();

    ComicItem retrieveComic(int comicId);

    ComicItem createComic(int comicId, ComicItem comicItem);

    ComicItem updateComic(int comicId, ComicItem comicItem);

    boolean deleteComic(int comicId);

    ResponseEntity<ImageDto> retrieveComicStrip(int comicId, Direction which) throws IOException;

    ResponseEntity<ImageDto> retrieveComicStrip(int comicId, Direction which, LocalDate from) throws IOException;

    ResponseEntity<ImageDto> retrieveAvatar(int comicId) throws IOException;

}
