package org.stapledon.infrastructure.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.core.AuthenticationException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.stream.Stream;

class JwtAuthenticationEntryPointTest {

    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();

    @ParameterizedTest
    @MethodSource("exceptionMessages")
    void commenceShouldSendUnauthorizedError(String message) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException exception = new AuthenticationException(message) {};

        entryPoint.commence(request, response, exception);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: " + message);
    }

    static Stream<String> exceptionMessages() {
        return Stream.of(
                "Bad credentials",
                "Token expired",
                "Full authentication is required to access this resource",
                "User account is locked"
        );
    }
}
