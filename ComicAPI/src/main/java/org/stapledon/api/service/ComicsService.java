package org.stapledon.api.service;

import org.stapledon.dto.ComicItem;
import org.stapledon.dto.ImageDto;
import org.stapledon.utils.Direction;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ComicsService {
    List<ComicItem> retrieveAll();

    Optional<ComicItem> retrieveComic(int comicId);

    Optional<ComicItem> createComic(int comicId, ComicItem comicItem);

    Optional<ComicItem> updateComic(int comicId, ComicItem comicItem);

    boolean deleteComic(int comicId);

    Optional<ImageDto> retrieveComicStrip(int comicId, Direction which) throws IOException;

    Optional<ImageDto> retrieveComicStrip(int comicId, Direction which, LocalDate from) throws IOException;

    Optional<ImageDto> retrieveAvatar(int comicId) throws IOException;

}
