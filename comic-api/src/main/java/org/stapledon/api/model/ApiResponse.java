package org.stapledon.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standardized API response wrapper
 * 
 * @param <T> Type of data contained in the response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private LocalDateTime timestamp;
    private int status;
    private String message;
    private T data;
    
    /**
     * Creates a successful response with data
     * 
     * @param data The data to include in the response
     * @param <T> Type of the data
     * @return ApiResponse containing the data
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .message("Success")
                .data(data)
                .build();
    }
    
    /**
     * Creates a successful response with data and custom message
     * 
     * @param data The data to include in the response
     * @param message Custom message
     * @param <T> Type of the data
     * @return ApiResponse containing the data
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .message(message)
                .data(data)
                .build();
    }
    
    /**
     * Creates an error response
     * 
     * @param status HTTP status code
     * @param message Error message
     * @param <T> Type parameter (not used for error responses)
     * @return ApiResponse containing the error details
     */
    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .message(message)
                .build();
    }
}