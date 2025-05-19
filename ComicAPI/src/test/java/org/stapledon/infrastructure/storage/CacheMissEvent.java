package org.stapledon.infrastructure.storage;

import java.time.LocalDate;

/**
 * Event class for comic cache misses
 */
public class CacheMissEvent {
    private final int comicId;
    private final String comicName;
    private final LocalDate date;

    public CacheMissEvent(int comicId, String comicName, LocalDate date) {
        this.comicId = comicId;
        this.comicName = comicName;
        this.date = date;
    }

    public int getComicId() {
        return comicId;
    }

    public String getComicName() {
        return comicName;
    }

    public LocalDate getDate() {
        return date;
    }
}