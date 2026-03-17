package org.stapledon.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.common.model.ComicCachingException;
import org.stapledon.common.model.ComicImageNotFoundException;
import org.stapledon.common.model.ComicNotFoundException;
import org.stapledon.core.auth.model.AuthenticationException;
import org.stapledon.engine.caching.CacheException;

import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final WebRequest request = mock(WebRequest.class);

    @ParameterizedTest
    @MethodSource("exceptionStatusProvider")
    void shouldReturnCorrectStatusForException(Exception exception, HttpStatus expectedStatus, String expectedMessageSubstring) {
        ResponseEntity<ApiResponse<Void>> response = dispatch(exception);

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains(expectedMessageSubstring);
    }

    static Stream<Arguments> exceptionStatusProvider() {
        return Stream.of(
                Arguments.of(new ComicNotFoundException("missing"), HttpStatus.NOT_FOUND, "comic could not be found"),
                Arguments.of(new ComicImageNotFoundException("no image"), HttpStatus.NOT_FOUND, "comic image could not be found"),
                Arguments.of(new ComicCachingException("cache fail"), HttpStatus.INTERNAL_SERVER_ERROR, "error processing the comic"),
                Arguments.of(new CacheException("lookup fail"), HttpStatus.NOT_FOUND, "could not be retrieved from cache"),
                Arguments.of(new AuthenticationException("bad creds"), HttpStatus.UNAUTHORIZED, "Authentication failed"),
                Arguments.of(new BadCredentialsException("wrong pw"), HttpStatus.UNAUTHORIZED, "Authentication failed"),
                Arguments.of(new UsernameNotFoundException("no user"), HttpStatus.UNAUTHORIZED, "Authentication failed"),
                Arguments.of(new IllegalArgumentException("bad arg"), HttpStatus.BAD_REQUEST, "Invalid request parameter"),
                Arguments.of(new DateTimeParseException("bad date", "not-a-date", 0), HttpStatus.BAD_REQUEST, "Invalid date format"),
                Arguments.of(new RuntimeException("unexpected"), HttpStatus.INTERNAL_SERVER_ERROR, "unexpected error")
        );
    }

    @ParameterizedTest
    @MethodSource("responseStatusProvider")
    void shouldMapResponseStatusExceptionCorrectly(HttpStatus inputStatus, String expectedMessageSubstring) {
        ResponseStatusException ex = new ResponseStatusException(inputStatus, "detail");

        ResponseEntity<ApiResponse<Void>> response = handler.handleResponseStatusException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(inputStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains(expectedMessageSubstring);
    }

    static Stream<Arguments> responseStatusProvider() {
        return Stream.of(
                Arguments.of(HttpStatus.NOT_FOUND, "not found"),
                Arguments.of(HttpStatus.BAD_REQUEST, "Invalid request"),
                Arguments.of(HttpStatus.UNAUTHORIZED, "Authentication required"),
                Arguments.of(HttpStatus.FORBIDDEN, "permission"),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, "error occurred")
        );
    }

    @Test
    void shouldTruncateLongMessages() {
        String longMessage = "A".repeat(200);
        RuntimeException ex = new RuntimeException(longMessage);

        ResponseEntity<ApiResponse<Void>> response = handler.handleAllExceptions(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        // The handler sanitizes internally; the response uses a generic message
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void shouldSanitizeSpecialCharacters() {
        RuntimeException ex = new RuntimeException("msg with <script>alert('xss')</script>\nnewline");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAllExceptions(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).doesNotContain("<script>");
    }

    /**
     * Dispatches the exception to the appropriate handler method.
     */
    @SuppressWarnings("unchecked")
    private ResponseEntity<ApiResponse<Void>> dispatch(Exception ex) {
        if (ex instanceof ComicNotFoundException e) {
            return handler.handleComicNotFoundException(e, request);
        } else if (ex instanceof ComicImageNotFoundException e) {
            return handler.handleComicImageNotFoundException(e, request);
        } else if (ex instanceof ComicCachingException e) {
            return handler.handleComicCachingException(e, request);
        } else if (ex instanceof CacheException e) {
            return handler.handleCacheException(e, request);
        } else if (ex instanceof AuthenticationException || ex instanceof BadCredentialsException) {
            return handler.handleAuthenticationException(ex, request);
        } else if (ex instanceof UsernameNotFoundException e) {
            return handler.handleUsernameNotFoundException(e, request);
        } else if (ex instanceof IllegalArgumentException e) {
            return handler.handleIllegalArgumentException(e, request);
        } else if (ex instanceof DateTimeParseException e) {
            return handler.handleDateTimeParseException(e, request);
        } else {
            return handler.handleAllExceptions(ex, request);
        }
    }
}
