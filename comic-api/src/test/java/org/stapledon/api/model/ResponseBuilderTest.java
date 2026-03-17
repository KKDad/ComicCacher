package org.stapledon.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.stream.Stream;

class ResponseBuilderTest {

    @Test
    void okWithDataShouldReturn200() {
        ResponseEntity<ApiResponse<String>> response = ResponseBuilder.ok("data");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo("data");
        assertThat(response.getBody().getStatus()).isEqualTo(200);
    }

    @Test
    void okWithDataAndMessageShouldReturn200WithMessage() {
        ResponseEntity<ApiResponse<String>> response = ResponseBuilder.ok("data", "custom msg");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo("data");
        assertThat(response.getBody().getMessage()).isEqualTo("custom msg");
    }

    @Test
    void collectionShouldReturn200WithList() {
        List<String> items = List.of("a", "b", "c");
        ResponseEntity<ApiResponse<List<String>>> response = ResponseBuilder.collection(items);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(items);
    }

    @Test
    void createdShouldReturn201() {
        ResponseEntity<ApiResponse<String>> response = ResponseBuilder.created("new-item");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo("new-item");
        assertThat(response.getBody().getMessage()).isEqualTo("Resource created successfully");
    }

    @Test
    void noContentShouldReturn204() {
        ResponseEntity<Void> response = ResponseBuilder.noContent();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @ParameterizedTest
    @MethodSource("errorStatusProvider")
    void errorShouldReturnCorrectStatus(HttpStatus status, String message) {
        ResponseEntity<ApiResponse<Object>> response = ResponseBuilder.error(status, message);

        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(status.value());
        assertThat(response.getBody().getMessage()).isEqualTo(message);
        assertThat(response.getBody().getData()).isNull();
    }

    static Stream<Arguments> errorStatusProvider() {
        return Stream.of(
                Arguments.of(HttpStatus.BAD_REQUEST, "Bad request"),
                Arguments.of(HttpStatus.UNAUTHORIZED, "Unauthorized"),
                Arguments.of(HttpStatus.FORBIDDEN, "Forbidden"),
                Arguments.of(HttpStatus.NOT_FOUND, "Not found"),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, "Server error"),
                Arguments.of(HttpStatus.CONFLICT, "Conflict")
        );
    }
}
