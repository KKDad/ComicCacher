package org.stapledon.common.model;

import lombok.Getter;

/**
 * Domain exception for comic operation failures.
 * Provides specific context about which comic and which operation failed.
 */
@Getter
public class ComicOperationException extends RuntimeException {
    private final int comicId;
    private final String operation;

    public ComicOperationException(int comicId, String operation, String message) {
        super(message);
        this.comicId = comicId;
        this.operation = operation;
    }

    public ComicOperationException(int comicId, String operation, String message, Throwable cause) {
        super(message, cause);
        this.comicId = comicId;
        this.operation = operation;
    }

    public static ComicOperationException createFailed() {
        return new ComicOperationException(-1, "create",
            "Failed to create comic");
    }

    public static ComicOperationException updateFailed(int comicId) {
        return new ComicOperationException(comicId, "update",
            "Failed to update comic with id " + comicId);
    }
}
