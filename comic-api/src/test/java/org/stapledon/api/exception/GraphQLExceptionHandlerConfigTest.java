package org.stapledon.api.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.stapledon.common.model.ComicCachingException;
import org.stapledon.common.model.ComicImageNotFoundException;
import org.stapledon.common.model.ComicNotFoundException;
import org.stapledon.common.model.ComicOperationException;
import org.stapledon.core.auth.model.AuthenticationException;
import org.stapledon.engine.caching.CacheException;

import graphql.GraphQLError;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import java.util.stream.Stream;

class GraphQLExceptionHandlerConfigTest {

    private final GraphQLExceptionHandlerConfig handler = new GraphQLExceptionHandlerConfig();
    private final DataFetchingEnvironment env = mockEnv("testField");

    @ParameterizedTest
    @MethodSource("exceptionProvider")
    void shouldMapExceptionToCorrectGraphQLError(Exception exception, String expectedErrorType, String expectedErrorCode, String expectedMessageSubstring) {
        GraphQLError error = dispatch(exception);

        assertThat(error).isNotNull();
        assertThat(error.getErrorType().toString()).isEqualTo(expectedErrorType);
        assertThat(error.getExtensions()).containsEntry("errorCode", expectedErrorCode);
        assertThat(error.getMessage()).contains(expectedMessageSubstring);
    }

    static Stream<Arguments> exceptionProvider() {
        return Stream.of(
                Arguments.of(new AuthenticationException("bad creds"), "UNAUTHORIZED", "UNAUTHENTICATED", "Authentication failed"),
                Arguments.of(new BadCredentialsException("wrong pw"), "UNAUTHORIZED", "UNAUTHENTICATED", "Authentication failed"),
                Arguments.of(new UsernameNotFoundException("no user"), "UNAUTHORIZED", "UNAUTHENTICATED", "Authentication failed"),
                Arguments.of(new AccessDeniedException("denied"), "FORBIDDEN", "FORBIDDEN", "permission"),
                Arguments.of(new ComicNotFoundException("missing"), "NOT_FOUND", "COMIC_NOT_FOUND", "comic could not be found"),
                Arguments.of(new ComicImageNotFoundException("no strip"), "NOT_FOUND", "STRIP_NOT_FOUND", "comic strip could not be found"),
                Arguments.of(new ComicOperationException(1, "update", "failed"), "INTERNAL_ERROR", "INTERNAL_ERROR", "operation failed"),
                Arguments.of(new ComicCachingException("cache fail"), "INTERNAL_ERROR", "INTERNAL_ERROR", "could not be retrieved"),
                Arguments.of(new CacheException("lookup fail"), "INTERNAL_ERROR", "INTERNAL_ERROR", "could not be retrieved"),
                Arguments.of(new IllegalArgumentException("bad arg"), "BAD_REQUEST", "VALIDATION_ERROR", "Invalid request"),
                Arguments.of(new IllegalStateException("bad state"), "INTERNAL_ERROR", "INTERNAL_ERROR", "unexpected error"),
                Arguments.of(new RuntimeException("unknown"), "INTERNAL_ERROR", "INTERNAL_ERROR", "unexpected error")
        );
    }

    private GraphQLError dispatch(Exception ex) {
        if (ex instanceof AuthenticationException || ex instanceof BadCredentialsException || ex instanceof UsernameNotFoundException) {
            return handler.handleAuthenticationException(ex, env);
        } else if (ex instanceof AccessDeniedException e) {
            return handler.handleAccessDeniedException(e, env);
        } else if (ex instanceof ComicNotFoundException e) {
            return handler.handleComicNotFoundException(e, env);
        } else if (ex instanceof ComicImageNotFoundException e) {
            return handler.handleComicImageNotFoundException(e, env);
        } else if (ex instanceof ComicOperationException e) {
            return handler.handleComicOperationException(e, env);
        } else if (ex instanceof ComicCachingException || ex instanceof CacheException) {
            return handler.handleCacheException(ex, env);
        } else if (ex instanceof IllegalArgumentException e) {
            return handler.handleIllegalArgumentException(e, env);
        } else if (ex instanceof IllegalStateException e) {
            return handler.handleIllegalStateException(e, env);
        } else {
            return handler.handleGenericException(ex, env);
        }
    }

    private static DataFetchingEnvironment mockEnv(String fieldName) {
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        Field field = mock(Field.class);
        when(field.getName()).thenReturn(fieldName);
        when(env.getField()).thenReturn(field);
        return env;
    }
}
