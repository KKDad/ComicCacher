package org.stapledon.downloader;

import org.stapledon.dto.ComicItem;

import java.time.LocalDate;

public class KingFeatures extends DailyComic {
    @Override
    public IDailyComic setDate(LocalDate date) {
        return null;
    }

    @Override
    public IDailyComic setComic(String comicName) {
        return null;
    }

    @Override
    public boolean ensureCache() {
        return false;
    }

    @Override
    public LocalDate advance() {
        return null;
    }

    @Override
    public LocalDate getLastStripOn() {
        return null;
    }

    @Override
    public void updateComicMetadata(ComicItem comicItem) {

    }
}
