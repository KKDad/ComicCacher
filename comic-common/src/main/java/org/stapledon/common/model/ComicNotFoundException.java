package org.stapledon.common.model;

/**
 * Exception thrown when a requested comic cannot be found
 */
public class ComicNotFoundException extends RuntimeException {

    /**
     * Creates a new ComicNotFoundException with the specified comic ID
     * 
     * @param comicId ID of the comic that could not be found
     */
    public ComicNotFoundException(int comicId) {
        super("Comic with ID " + comicId + " could not be found");
    }
    
    /**
     * Creates a new ComicNotFoundException with the specified comic name
     * 
     * @param comicName Name of the comic that could not be found
     */
    public ComicNotFoundException(String comicName) {
        super("Comic with name '" + comicName + "' could not be found");
    }
}