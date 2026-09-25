package org.stapledon.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicReference;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    void echoesAWellFormedIncomingRequestIdAndExposesItInTheMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/graphql");
        request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "frontend-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenInMdc = new AtomicReference<>();

        filter.doFilter(request, response, new MockFilterChain(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res) {
                seenInMdc.set(MDC.get("requestId"));
                MDC.put("user", "alice");
            }
        }));

        assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isEqualTo("frontend-123");
        assertThat(seenInMdc.get()).isEqualTo("frontend-123");
        // Nothing leaks to the next request served by this thread
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("user")).isNull();
    }

    @Test
    void generatesAnIdWhenTheHeaderIsMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/comics/1/avatar"), response, new MockFilterChain());

        assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).matches("[0-9a-f]{8}");
    }

    @Test
    void rejectsIdsThatCouldForgeLogLines() {
        assertThat(RequestLoggingFilter.resolveRequestId("abc\ninjected")).matches("[0-9a-f]{8}");
        assertThat(RequestLoggingFilter.resolveRequestId("x".repeat(65))).matches("[0-9a-f]{8}");
        assertThat(RequestLoggingFilter.resolveRequestId("abc-DEF-123")).isEqualTo("abc-DEF-123");
    }
}
