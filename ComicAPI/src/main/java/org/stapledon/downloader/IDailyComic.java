package org.stapledon.downloader;

import org.stapledon.dto.ComicItem;

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
     * @param comicName Name of the api to process
     * @return this
     */
    IDailyComic setComic(String comicName);


    /**
     * Ensure that the api is cached for the current date
     * @return True if the api has been cached on this date
     */
    boolean ensureCache();

    /**
     * Advances the date by the interval that the api is published on. For daily comics, this is one dat. For
     * periodically published comics, this may be multiple days. This cannot go past getLastStripOn.
     * @return Next available date.
     */
    LocalDate advance();

    /**
     * Determines when the latest published image it. Some comics are only available on the web a couple days or
     * a week after they were published in print.
     * @return Mst recent date we can get a api for
     */
    LocalDate getLastStripOn();

    /**
     * Update the api metadata from the authors/publishers website
     *  - Description
     *  - Author's name
     *  - Avatar and/or Icon
     */
    void updateComicMetadata(ComicItem comicItem);

}
