package org.stapledon.engine.caching;

import java.time.LocalDate;

/**
 * Exception thrown when there is an issue with the comic cache
 */
public class CacheException extends RuntimeException {

    /**
     * Creates a new CacheException with the specified message
     *
     * @param message Error message
     */
    public CacheException(String message) {
        super(message);
    }

    /**
     * Creates a new CacheException with the specified message and cause
     *
     * @param message Error message
     * @param cause Cause of the exception
     */
    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a CacheException for a comic that does not have a cache directory
     *
     * @param comicName Name of the comic
     * @param path Path that was checked
     * @return A new CacheException
     */
    public static CacheException directoryNotFound(String comicName, String path) {
        return new CacheException(
                String.format("Cache directory for comic '%s' does not exist: %s", comicName, path)
        );
    }

    /**
     * Creates a CacheException for a comic date that was not found
     *
     * @param comicName Name of the comic
     * @param date Date that was requested
     * @return A new CacheException
     */
    public static CacheException dateNotFound(String comicName, LocalDate date) {
        return new CacheException(
                String.format("Comic '%s' date '%s' not found in cache", comicName, date)
        );
    }
}
