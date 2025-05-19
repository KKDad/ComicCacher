package org.stapledon.core.comic.service;

import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.api.dto.comic.ImageDto;
import org.stapledon.common.util.Direction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ComicsService {
    List<ComicItem> retrieveAll();

    Optional<ComicItem> retrieveComic(int comicId);

    Optional<ComicItem> createComic(int comicId, ComicItem comicItem);

    Optional<ComicItem> updateComic(int comicId, ComicItem comicItem);

    boolean deleteComic(int comicId);

    Optional<ImageDto> retrieveComicStrip(int comicId, Direction which);

    Optional<ImageDto> retrieveComicStrip(int comicId, Direction which, LocalDate from);

    Optional<ImageDto> retrieveAvatar(int comicId);

}
