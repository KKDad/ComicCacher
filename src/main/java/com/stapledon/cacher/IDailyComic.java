package com.stapledon.cacher;

import java.time.LocalDate;

/**
 * Interface that all Daily Comic retrievers must implement
 */
public interface IDailyComic
{
    /**
     * Set the date for the retrieval
     * @param date date to set
     * @return this
     */
    IDailyComic setDate(LocalDate date);

    /**
     * Set the GoComic that to caching
     * @param comicName Name of the comic to process
     * @return this
     */
    IDailyComic setComic(String comicName);


    /**
     * Ensure that the comic is cached for the current date
     * @return True if the comic has been cached on this date
     */
    boolean ensureCache();

    /**
     * Advances the date by the interval that the comic is published on. For daily comics, this is one dat. For
     * periodically published comics, this may be multiple days. This cannot go past getLastStripOn.
     * @return Next available date.
     */
    LocalDate advance();

    /**
     * Determines when the latest published image it. Some comics are only available on the web a couple days or
     * a week after they were published in print.
     * @return Mst recent date we can get a comic for
     */
    LocalDate getLastStripOn();

    /**
     * Get a description of the comic from the authors/publishers website
     * @return description of the comic
     */
    String getComicDescription();
}
