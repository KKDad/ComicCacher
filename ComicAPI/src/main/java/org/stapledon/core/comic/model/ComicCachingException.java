package org.stapledon.core.comic.model;

/**
 * Exception thrown when there is an error caching a comic
 */
public class ComicCachingException extends RuntimeException {

    /**
     * Creates a new ComicCachingException with the specified message
     * 
     * @param message Error message
     */
    public ComicCachingException(String message) {
        super(message);
    }
    
    /**
     * Creates a new ComicCachingException with the specified message and cause
     * 
     * @param message Error message
     * @param cause Cause of the exception
     */
    public ComicCachingException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Creates a new ComicCachingException for a specific comic
     * 
     * @param comicName Name of the comic
     * @param cause Cause of the exception
     * @return A new ComicCachingException
     */
    public static ComicCachingException forComic(String comicName, Throwable cause) {
        return new ComicCachingException("Failed to cache comic: " + comicName, cause);
    }
}