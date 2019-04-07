package org.stapledon.api;

import org.springframework.http.ResponseEntity;
import org.stapledon.dto.ComicItem;
import org.stapledon.dto.ImageDto;
import org.stapledon.utils.Direction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface IComicsService
{
    ComicItem retrieveComic(int comicId);

    List<ComicItem> retrieveAll();

    ResponseEntity<ImageDto> retrieveComicStrip(int comicId, Direction which) throws IOException;

    ResponseEntity<ImageDto> retrieveComicStrip(int comicId, Direction which, LocalDate from) throws IOException;

    ResponseEntity<ImageDto> retrieveAvatar(int comicId)  throws IOException;
}
