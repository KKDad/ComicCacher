package org.stapledon.config;

import org.stapledon.downloader.IDailyComic;

import java.time.LocalDate;

public interface IComicsBootstrap {
    String stripName();

    LocalDate startDate();

    IDailyComic getDownloader();
}
