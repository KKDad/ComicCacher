package org.stapledon.engine.source;

import java.time.LocalDate;

/**
 * Where a comic starts at its source: the first strip's date for daily comics, its number for indexed ones. Either may be null.
 */
public record StartInfo(LocalDate date, Integer stripNumber) {

    /** A daily comic's first strip date. */
    public static StartInfo ofDate(LocalDate date) {
        return new StartInfo(date, null);
    }

    /** An indexed comic's first strip number. */
    public static StartInfo ofStripNumber(int stripNumber) {
        return new StartInfo(null, stripNumber);
    }
}
