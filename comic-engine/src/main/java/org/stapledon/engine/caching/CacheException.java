package org.stapledon.engine.caching;

/**
 * Exception thrown when there is an issue with the comic cache.
 */
public class CacheException extends RuntimeException {

    /**
     * Creates a new CacheException with the specified message.
     *
     * @param message Error message
     */
    public CacheException(String message) {
        super(message);
    }

    /**
     * Creates a new CacheException with the specified message and cause.
     *
     * @param message Error message
     * @param cause Cause of the exception
     */
    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
