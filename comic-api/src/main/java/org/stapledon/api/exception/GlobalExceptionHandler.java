package org.stapledon.api.exception;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.stapledon.common.model.ComicCachingException;
import org.stapledon.common.model.ComicImageNotFoundException;
import org.stapledon.common.model.ComicNotFoundException;
import org.stapledon.core.auth.model.AuthenticationException;
import org.stapledon.engine.caching.CacheException;

import java.net.URI;
import java.time.format.DateTimeParseException;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


/**
 * Global exception handler that emits RFC 7807 Problem Details for REST error responses.
 * Success-path responses still use {@code ApiResponse<T>}; only the error path is RFC 7807.
 */
@Slf4j
@ToString
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final int MAX_MESSAGE_LENGTH = 100;
    private static final String TRUNCATION_SUFFIX = "...";

    @ExceptionHandler(ComicNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleComicNotFoundException(ComicNotFoundException ex, WebRequest request) {
        log.warn("Comic not found: {}", sanitizeAndTruncate(ex.getMessage()), ex);
        return problem(HttpStatus.NOT_FOUND, "The requested comic could not be found", request);
    }

    @ExceptionHandler(ComicImageNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleComicImageNotFoundException(ComicImageNotFoundException ex, WebRequest request) {
        log.warn("Comic image not found: {}", sanitizeAndTruncate(ex.getMessage()), ex);
        return problem(HttpStatus.NOT_FOUND, "The requested comic image could not be found", request);
    }

    @ExceptionHandler(ComicCachingException.class)
    public ResponseEntity<ProblemDetail> handleComicCachingException(ComicCachingException ex, WebRequest request) {
        log.error("Error caching comic: {}", sanitizeAndTruncate(ex.getMessage()), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "There was an error processing the comic", request);
    }

    @ExceptionHandler(CacheException.class)
    public ResponseEntity<ProblemDetail> handleCacheException(CacheException ex, WebRequest request) {
        log.error("Cache error: {}", sanitizeAndTruncate(ex.getMessage()), ex);
        return problem(HttpStatus.NOT_FOUND, "The requested data could not be retrieved from cache", request);
    }

    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ProblemDetail> handleAuthenticationException(Exception ex, WebRequest request) {
        log.warn("Authentication failed: {}", sanitizeAndTruncate(ex.getMessage()), ex);
        return problem(HttpStatus.UNAUTHORIZED, "Authentication failed. Please verify your credentials.", request);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUsernameNotFoundException(UsernameNotFoundException ex, WebRequest request) {
        log.warn("Username not found: {}", sanitizeAndTruncate(ex.getMessage()), ex);
        return problem(HttpStatus.UNAUTHORIZED, "Authentication failed. Please verify your credentials.", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        log.warn("Response status exception: {}", sanitizeAndTruncate(ex.getReason()), ex);

        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String detail = switch (status) {
            case NOT_FOUND -> "The requested resource was not found";
            case BAD_REQUEST -> "Invalid request parameters";
            case UNAUTHORIZED -> "Authentication required";
            case FORBIDDEN -> "You don't have permission to access this resource";
            default -> "An error occurred processing your request";
        };

        return problem(status, detail, request);
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatchException(TypeMismatchException ex, WebRequest request) {
        String value = ex.getValue() != null ? ex.getValue().toString() : "null";
        String propertyName = sanitizeAndTruncate(sanitizeInput(ex.getPropertyName()), 30);
        String requiredType = sanitizeAndTruncate(
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown", 30);

        log.warn("Type mismatch exception: Invalid value '{}' for field '{}'. Expected type: {}",
                sanitizeAndTruncate(value, 20), propertyName, requiredType, ex);

        String detail = String.format("Invalid value for field '%s'. Expected type: %s", propertyName, requiredType);
        return problem(HttpStatus.BAD_REQUEST, detail, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", sanitizeAndTruncate(ex.getMessage()), ex);
        return problem(HttpStatus.BAD_REQUEST, "Invalid request parameter", request);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ProblemDetail> handleDateTimeParseException(DateTimeParseException ex, WebRequest request) {
        log.warn("Date parsing exception for input: {}", sanitizeAndTruncate(ex.getParsedString(), 30), ex);
        return problem(HttpStatus.BAD_REQUEST, "Invalid date format. Expected format: yyyy-MM-dd", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("Unhandled exception: {}", sanitizeAndTruncate(ex.getMessage()), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    /**
     * Builds an RFC 7807 Problem Details response with the given status, detail, and request URI.
     */
    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        URI instance = extractInstance(request);
        if (instance != null) {
            problemDetail.setInstance(instance);
        }
        return ResponseEntity.status(status).body(problemDetail);
    }

    private URI extractInstance(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            String uri = servletRequest.getRequest().getRequestURI();
            if (uri != null && !uri.isBlank()) {
                try {
                    return URI.create(uri);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private String sanitizeAndTruncate(String input) {
        return sanitizeAndTruncate(input, MAX_MESSAGE_LENGTH);
    }

    private String sanitizeAndTruncate(String input, int maxLength) {
        if (input == null) {
            return "[null]";
        }
        String sanitized = sanitizeInput(input);
        if (sanitized.length() > maxLength) {
            return sanitized.substring(0, maxLength) + TRUNCATION_SUFFIX;
        }
        return sanitized;
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return "[null]";
        }
        return input.replaceAll("[\\r\\n\\t\\\\\"'<>&]", "_");
    }
}
