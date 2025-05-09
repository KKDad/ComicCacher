package org.stapledon.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.model.ResponseBuilder;
import org.stapledon.exceptions.ComicCachingException;
import org.stapledon.exceptions.ComicImageNotFoundException;
import org.stapledon.exceptions.ComicNotFoundException;
import org.stapledon.utils.CacheException;


/**
 * Global exception handler for the application
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle ComicNotFoundException
     */
    @ExceptionHandler(ComicNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleComicNotFoundException(ComicNotFoundException ex, WebRequest request) {
        log.warn("Comic not found: {}", ex.getMessage());
        return ResponseBuilder.error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handle ComicImageNotFoundException
     */
    @ExceptionHandler(ComicImageNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleComicImageNotFoundException(ComicImageNotFoundException ex, WebRequest request) {
        log.warn("Comic image not found: {}", ex.getMessage());
        return ResponseBuilder.error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handle ComicCachingException
     */
    @ExceptionHandler(ComicCachingException.class)
    public ResponseEntity<ApiResponse<Void>> handleComicCachingException(ComicCachingException ex, WebRequest request) {
        log.error("Error caching comic: {}", ex.getMessage(), ex);
        return ResponseBuilder.error(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing comic: " + ex.getMessage());
    }

    /**
     * Handle CacheException
     */
    @ExceptionHandler(CacheException.class)
    public ResponseEntity<ApiResponse<Void>> handleCacheException(CacheException ex, WebRequest request) {
        log.error("Cache error: {}", ex.getMessage(), ex);
        return ResponseBuilder.error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handle ResponseStatusException
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        return ResponseBuilder.error(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason());
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseBuilder.error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

}