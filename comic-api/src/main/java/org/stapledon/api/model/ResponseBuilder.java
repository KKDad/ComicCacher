package org.stapledon.api.model;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Utility class to build standardized API responses
 */
public final class ResponseBuilder {

    private ResponseBuilder() {
        // Utility class
    }

    /**
     * Creates a success response with data
     *
     * @param data The data to include in the response
     * @param <T>  Type of the data
     * @return ResponseEntity containing the data wrapped in an ApiResponse
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Creates a success response with data and custom message
     *
     * @param data    The data to include in the response
     * @param message Custom message
     * @param <T>     Type of the data
     * @return ResponseEntity containing the data wrapped in an ApiResponse
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }

    /**
     * Creates a response for a collection of items
     *
     * @param items Collection of items
     * @param <T>   Type of the items
     * @return ResponseEntity containing the items wrapped in an ApiResponse
     */
    public static <T> ResponseEntity<ApiResponse<List<T>>> collection(List<T> items) {
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    /**
     * Creates a response for a created item
     *
     * @param data The created item
     * @param <T>  Type of the item
     * @return ResponseEntity with HTTP 201 status containing the item wrapped in an
     *         ApiResponse
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        ApiResponse<T> response = ApiResponse.<T>builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.CREATED.value())
                .message("Resource created successfully")
                .data(data)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates a response for a successful deletion
     *
     * @return ResponseEntity with HTTP 204 status and no content
     */
    public static ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates an error response
     *
     * @param status  HTTP status
     * @param message Error message
     * @return ResponseEntity with the specified status containing the error message
     *         wrapped in an ApiResponse
     */
    public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message) {
        ApiResponse<T> response = ApiResponse.error(status.value(), message);
        return ResponseEntity.status(status).body(response);
    }
}