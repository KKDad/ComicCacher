package org.stapledon.api.dto;

/**
 * Application error codes matching the GraphQL ErrorCode enum.
 */
public enum ErrorCode {
    COMIC_NOT_FOUND,
    FORBIDDEN,
    INTERNAL_ERROR,
    INVALID_CREDENTIALS,
    INVALID_PASSWORD,
    INVALID_TOKEN,
    NOT_FOUND,
    RATE_LIMITED,
    STRIP_NOT_FOUND,
    TOKEN_EXPIRED,
    UNAUTHENTICATED,
    USER_ALREADY_EXISTS,
    USER_NOT_FOUND,
    VALIDATION_ERROR
}
