package org.stapledon.infrastructure.config;

import org.stapledon.core.comic.downloader.IDailyComic;

import java.time.LocalDate;

public interface IComicsBootstrap {
    String stripName();

    LocalDate startDate();

    IDailyComic getDownloader();
}
