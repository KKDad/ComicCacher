package org.stapledon.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

class ApiResponseTest {

    @Test
    void successWithDataShouldSetDefaults() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("Success");
        assertThat(response.getData()).isEqualTo("hello");
    }

    @Test
    void successWithDataAndMessageShouldUseCustomMessage() {
        ApiResponse<Integer> response = ApiResponse.success(42, "Found it");

        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("Found it");
        assertThat(response.getData()).isEqualTo(42);
    }

    @ParameterizedTest
    @MethodSource("dataTypeProvider")
    void successShouldPreserveVariousDataTypes(Object data) {
        ApiResponse<Object> response = ApiResponse.success(data);

        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getTimestamp()).isNotNull();
    }

    static Stream<Object> dataTypeProvider() {
        return Stream.of(
                "string value",
                123,
                45.67,
                true,
                List.of("a", "b", "c")
        );
    }

    @ParameterizedTest
    @MethodSource("errorStatusProvider")
    void errorShouldSetCorrectStatusAndMessage(int status, String message) {
        ApiResponse<Void> response = ApiResponse.error(status, message);

        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getData()).isNull();
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> errorStatusProvider() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(400, "Bad Request"),
                org.junit.jupiter.params.provider.Arguments.of(401, "Unauthorized"),
                org.junit.jupiter.params.provider.Arguments.of(403, "Forbidden"),
                org.junit.jupiter.params.provider.Arguments.of(404, "Not Found"),
                org.junit.jupiter.params.provider.Arguments.of(500, "Internal Server Error")
        );
    }
}
