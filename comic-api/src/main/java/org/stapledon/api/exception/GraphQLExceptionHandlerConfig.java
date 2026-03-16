package org.stapledon.api.exception;

import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.stapledon.common.model.ComicCachingException;
import org.stapledon.common.model.ComicImageNotFoundException;
import org.stapledon.common.model.ComicNotFoundException;
import org.stapledon.common.model.ComicOperationException;
import org.stapledon.core.auth.model.AuthenticationException;
import org.stapledon.engine.caching.CacheException;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL-specific exception handler.
 * Maps domain exceptions to proper GraphQL errors with ErrorType classification
 * and errorCode extensions.
 */
@Slf4j
@ControllerAdvice
public class GraphQLExceptionHandlerConfig {

    private static final int MAX_MESSAGE_LENGTH = 100;

    @GraphQlExceptionHandler({AuthenticationException.class, BadCredentialsException.class, UsernameNotFoundException.class})
    public GraphQLError handleAuthenticationException(Exception ex, DataFetchingEnvironment env) {
        log.warn("GraphQL authentication failed on field '{}': {}", env.getField().getName(), sanitize(ex.getMessage()));
        return GraphQLError.newError()
                .errorType(ErrorClassification.errorClassification("UNAUTHORIZED"))
                .message("Authentication failed. Please verify your credentials.")
                .extensions(java.util.Map.of("errorCode", "UNAUTHENTICATED"))
                .build();
    }

    @GraphQlExceptionHandler(AccessDeniedException.class)
    public GraphQLError handleAccessDeniedException(AccessDeniedException ex, DataFetchingEnvironment env) {
        log.warn("GraphQL access denied on field '{}': {}", env.getField().getName(), sanitize(ex.getMessage()));
        return GraphQLError.newError()
                .errorType(ErrorClassification.errorClassification("FORBIDDEN"))
                .message("You don't have permission to access this resource.")
                .extensions(java.util.Map.of("errorCode", "FORBIDDEN"))
                .build();
    }

    @GraphQlExceptionHandler(ComicNotFoundException.class)
    public GraphQLError handleComicNotFoundException(ComicNotFoundException ex, DataFetchingEnvironment env) {
        log.warn("GraphQL comic not found on field '{}': {}", env.getField().getName(), sanitize(ex.getMessage()));
        return GraphQLError.newError()
                .errorType(ErrorClassification.errorClassification("NOT_FOUND"))
                .message("The requested comic could not be found.")
                .extensions(java.util.Map.of("errorCode", "COMIC_NOT_FOUND"))
                .build();
    }

    @GraphQlExceptionHandler(ComicImageNotFoundException.class)
    public GraphQLError handleComicImageNotFoundException(ComicImageNotFoundException ex, DataFetchingEnvironment env) {
        log.warn("GraphQL comic image not found on field '{}': {}", env.getField().getName(), sanitize(ex.getMessage()));
        return GraphQLError.newError()
                .errorType(ErrorClassification.errorClassification("NOT_FOUND"))
                .message("The requested comic strip could not be found.")
                .extensions(java.util.Map.of("errorCode", "STRIP_NOT_FOUND"))
                .build();
    }

    @GraphQlExceptionHandler(ComicOperationException.class)
    public GraphQLError handleComicOperationException(ComicOperationException ex, DataFetchingEnvironment env) {
        log.error("GraphQL comic operation failed on field '{}': {}", env.getField().getName(), sanitize(ex.getMessage()));
        return GraphQLError.newError()
                .errorType(ErrorClassification.errorClassification("INTERNAL_ERROR"))
                .message("The comic operation failed.")
                .extensions(java.util.Map.of("errorCode", "INTERNAL_ERROR"))
                .build();
    }

    @GraphQlExceptionHandler({ComicCachingException.class, CacheException.class})
    public GraphQLError handleCacheException(Exception ex, DataFetchingEnvironment env) {
        log.error("GraphQL cache error on field '{}': {}", env.getField().getName(), sanitize(ex.getMessage()));
        return GraphQLError.newError()
                .errorType(ErrorClassification.errorClassification("INTERNAL_ERROR"))
                .message("The requested data could not be retrieved.")
                .extensions(java.util.Map.of("errorCode", "INTERNAL_ERROR"))
                .build();
    }

    @GraphQlExceptionHandler(IllegalArgumentException.class)
    public GraphQLError handleIllegalArgumentException(IllegalArgumentException ex, DataFetchingEnvironment env) {
        log.warn("GraphQL validation error on field '{}': {}", env.getField().getName(), sanitize(ex.getMessage()));
        return GraphQLError.newError()
                .errorType(ErrorClassification.errorClassification("BAD_REQUEST"))
                .message("Invalid request parameter.")
                .extensions(java.util.Map.of("errorCode", "VALIDATION_ERROR"))
                .build();
    }

    @GraphQlExceptionHandler(IllegalStateException.class)
    public GraphQLError handleIllegalStateException(IllegalStateException ex, DataFetchingEnvironment env) {
        log.error("GraphQL illegal state on field '{}': {}", env.getField().getName(), sanitize(ex.getMessage()));
        return GraphQLError.newError()
                .errorType(ErrorClassification.errorClassification("INTERNAL_ERROR"))
                .message("An unexpected error occurred.")
                .extensions(java.util.Map.of("errorCode", "INTERNAL_ERROR"))
                .build();
    }

    @GraphQlExceptionHandler(Exception.class)
    public GraphQLError handleGenericException(Exception ex, DataFetchingEnvironment env) {
        log.error("GraphQL unhandled exception on field '{}': {}", env.getField().getName(), sanitize(ex.getMessage()), ex);
        return GraphQLError.newError()
                .errorType(ErrorClassification.errorClassification("INTERNAL_ERROR"))
                .message("An unexpected error occurred.")
                .extensions(java.util.Map.of("errorCode", "INTERNAL_ERROR"))
                .build();
    }

    private String sanitize(String input) {
        if (input == null) {
            return "[null]";
        }
        String sanitized = input.replaceAll("[\\r\\n\\t\\\\\"'<>&]", "_");
        if (sanitized.length() > MAX_MESSAGE_LENGTH) {
            return sanitized.substring(0, MAX_MESSAGE_LENGTH) + "...";
        }
        return sanitized;
    }
}
