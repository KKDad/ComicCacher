package org.stapledon.api.exception;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.model.ResponseBuilder;
import org.stapledon.common.model.ComicCachingException;
import org.stapledon.common.model.ComicImageNotFoundException;
import org.stapledon.common.model.ComicNotFoundException;
import org.stapledon.core.auth.model.AuthenticationException;
import org.stapledon.engine.caching.CacheException;

import java.time.format.DateTimeParseException;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


/**
 * Global exception handler for the application
 */
@Slf4j
@ToString
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final int MAX_MESSAGE_LENGTH = 100;
    private static final String TRUNCATION_SUFFIX = "...";

    /**
     * Handle ComicNotFoundException
     */
    @ExceptionHandler(ComicNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleComicNotFoundException(ComicNotFoundException ex, WebRequest request) {
        String sanitizedMessage = sanitizeAndTruncate(ex.getMessage());
        log.warn("Comic not found: {}", sanitizedMessage, ex);
        // Use a generic message for the response
        return ResponseBuilder.error(HttpStatus.NOT_FOUND, "The requested comic could not be found");
    }

    /**
     * Handle ComicImageNotFoundException
     */
    @ExceptionHandler(ComicImageNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleComicImageNotFoundException(ComicImageNotFoundException ex, WebRequest request) {
        String sanitizedMessage = sanitizeAndTruncate(ex.getMessage());
        log.warn("Comic image not found: {}", sanitizedMessage, ex);
        return ResponseBuilder.error(HttpStatus.NOT_FOUND, "The requested comic image could not be found");
    }

    /**
     * Handle ComicCachingException
     */
    @ExceptionHandler(ComicCachingException.class)
    public ResponseEntity<ApiResponse<Void>> handleComicCachingException(ComicCachingException ex, WebRequest request) {
        String sanitizedMessage = sanitizeAndTruncate(ex.getMessage());
        log.error("Error caching comic: {}", sanitizedMessage, ex);
        return ResponseBuilder.error(HttpStatus.INTERNAL_SERVER_ERROR, "There was an error processing the comic");
    }

    /**
     * Handle CacheException
     */
    @ExceptionHandler(CacheException.class)
    public ResponseEntity<ApiResponse<Void>> handleCacheException(CacheException ex, WebRequest request) {
        String sanitizedMessage = sanitizeAndTruncate(ex.getMessage());
        log.error("Cache error: {}", sanitizedMessage, ex);
        return ResponseBuilder.error(HttpStatus.NOT_FOUND, "The requested data could not be retrieved from cache");
    }

    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(Exception ex, WebRequest request) {
        String sanitizedMessage = sanitizeAndTruncate(ex.getMessage());
        log.warn("Authentication failed: {}", sanitizedMessage, ex);
        // Use a generic message to avoid revealing authentication mechanisms
        return ResponseBuilder.error(HttpStatus.UNAUTHORIZED, "Authentication failed. Please verify your credentials.");
    }

    /**
     * Handle username not found exception
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameNotFoundException(UsernameNotFoundException ex, WebRequest request) {
        String sanitizedMessage = sanitizeAndTruncate(ex.getMessage());
        log.warn("Username not found: {}", sanitizedMessage, ex);
        // Use a generic message to prevent username enumeration
        return ResponseBuilder.error(HttpStatus.UNAUTHORIZED, "Authentication failed. Please verify your credentials.");
    }

    /**
     * Handle ResponseStatusException
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        String sanitizedReason = sanitizeAndTruncate(ex.getReason());
        log.warn("Response status exception: {}", sanitizedReason, ex);
        
        // Generate appropriate status-specific message
        String message;
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        
        // Use generic messages based on status code
        switch (status) {
            case NOT_FOUND:
                message = "The requested resource was not found";
                break;
            case BAD_REQUEST:
                message = "Invalid request parameters";
                break;
            case UNAUTHORIZED:
                message = "Authentication required";
                break;
            case FORBIDDEN:
                message = "You don't have permission to access this resource";
                break;
            default:
                message = "An error occurred processing your request";
        }
        
        return ResponseBuilder.error(status, message);
    }

    /**
     * Handle TypeMismatchException which occurs when path variables can't be converted to the specified type
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(TypeMismatchException ex, WebRequest request) {
        String value = ex.getValue() != null ? ex.getValue().toString() : "null";
        String propertyName = sanitizeInput(ex.getPropertyName());
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        
        // Sanitize and truncate all inputs
        value = sanitizeAndTruncate(value, 20);
        propertyName = sanitizeAndTruncate(propertyName, 30);
        requiredType = sanitizeAndTruncate(requiredType, 30);
        
        String logMessage = String.format("Invalid value '%s' for field '%s'. Expected type: %s", 
                value, propertyName, requiredType);
                
        log.warn("Type mismatch exception: {}", logMessage, ex);
        
        // Use a generic message that doesn't expose raw input values
        String message = String.format("Invalid value for field '%s'. Expected type: %s", 
                propertyName, requiredType);
                
        return ResponseBuilder.error(HttpStatus.BAD_REQUEST, message);
    }
    
    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        String sanitizedMessage = sanitizeAndTruncate(ex.getMessage());
        log.warn("Invalid argument: {}", sanitizedMessage, ex);
        return ResponseBuilder.error(HttpStatus.BAD_REQUEST, "Invalid request parameter");
    }
    
    /**
     * Handle DateTimeParseException which occurs when a date string can't be parsed
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ApiResponse<Void>> handleDateTimeParseException(DateTimeParseException ex, WebRequest request) {
        String parsedString = ex.getParsedString();
        String sanitizedInput = sanitizeAndTruncate(parsedString, 30);
        
        // Log the sanitized input
        log.warn("Date parsing exception for input: {}", sanitizedInput, ex);
        
        // Return a generic message that doesn't include the exact input
        return ResponseBuilder.error(HttpStatus.BAD_REQUEST, "Invalid date format. Expected format: yyyy-MM-dd");
    }
    
    /**
     * Helper method to sanitize and truncate input for safe logging and error messages
     */
    private String sanitizeAndTruncate(String input) {
        return sanitizeAndTruncate(input, MAX_MESSAGE_LENGTH);
    }
    
    /**
     * Helper method to sanitize and truncate input with a specified max length
     */
    private String sanitizeAndTruncate(String input, int maxLength) {
        if (input == null) {
            return "[null]";
        }
        
        // Sanitize the input first
        String sanitized = sanitizeInput(input);
        
        // Then truncate if necessary
        if (sanitized.length() > maxLength) {
            return sanitized.substring(0, maxLength) + TRUNCATION_SUFFIX;
        }
        
        return sanitized;
    }
    
    /**
     * Helper method to sanitize input for safe logging and error messages
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return "[null]";
        }
        // Replace characters that could be used for log injection or XSS
        return input.replaceAll("[\\r\\n\\t\\\\\"'<>&]", "_");
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllExceptions(Exception ex, WebRequest request) {
        String sanitizedMessage = sanitizeAndTruncate(ex.getMessage());
        log.error("Unhandled exception: {}", sanitizedMessage, ex);
        return ResponseBuilder.error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }
}