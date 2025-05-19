package org.stapledon.events;

import java.time.LocalDate;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Event triggered when a requested comic strip is not found in the cache.
 * This event is used to notify listeners that a comic needs to be downloaded.
 */
@Data
@RequiredArgsConstructor
public class CacheMissEvent {
    private final int comicId;
    private final String comicName;
    private final LocalDate date;
}