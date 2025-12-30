package org.stapledon.common.model;

import java.time.LocalDate;

/**
 * Exception thrown when a comic image cannot be found
 */
public class ComicImageNotFoundException extends RuntimeException {

    /**
     * Creates a new ComicImageNotFoundException with the specified comic ID and date
     *
     * @param comicId ID of the comic
     * @param date Date of the image that could not be found
     */
    public ComicImageNotFoundException(int comicId, LocalDate date) {
        super("Comic image for comic ID " + comicId + " on date " + date + " could not be found");
    }

    /**
     * Creates a new ComicImageNotFoundException with the specified comic name and date
     *
     * @param comicName Name of the comic
     * @param date Date of the image that could not be found
     */
    public ComicImageNotFoundException(String comicName, LocalDate date) {
        super("Comic image for '" + comicName + "' on date " + date + " could not be found");
    }

    /**
     * Creates a new ComicImageNotFoundException for a comic avatar
     *
     * @param comicId ID of the comic
     */
    public static ComicImageNotFoundException forAvatar(int comicId) {
        return new ComicImageNotFoundException("Avatar for comic ID " + comicId + " could not be found");
    }

    /**
     * Creates a new ComicImageNotFoundException with a custom message
     *
     * @param message Custom error message
     */
    public ComicImageNotFoundException(String message) {
        super(message);
    }
}