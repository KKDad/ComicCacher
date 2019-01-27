package com.stapledon.cacher;

import java.time.LocalDate;

/**
 * Interface that all Daily Comic retrievers must implement
 */
public interface IDailyComic
{
    IDailyComic setDate(LocalDate date);

    IDailyComic setComic(String comicName);

    boolean ensureCache();

    LocalDate advance();

    /**
     * Determines when the latest published image it. Some comics are only available on the web a couple days or
     * a week after they were published in print.
     * @return Mst recent date we can get a comic for
     */
    LocalDate getLastStripOn();
}
