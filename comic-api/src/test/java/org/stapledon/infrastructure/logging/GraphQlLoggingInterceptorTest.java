package org.stapledon.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import reactor.core.publisher.Mono;

class GraphQlLoggingInterceptorTest {

    @AfterEach
    void cleanUp() {
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void recordsTheOperationNameInTheMdcAndOnTheServletRequest() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("POST", "/graphql");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
        WebGraphQlRequest request = mock(WebGraphQlRequest.class);
        when(request.getOperationName()).thenReturn("GetComics");
        // Spring GraphQL hands the interceptor read-only attributes; writing to them used to fail every request
        when(request.getAttributes()).thenReturn(Map.of());
        when(request.getVariables()).thenReturn(Map.of());
        WebGraphQlInterceptor.Chain chain = mock(WebGraphQlInterceptor.Chain.class);
        when(chain.next(any())).thenReturn(Mono.just(mock(WebGraphQlResponse.class)));

        new GraphQlLoggingInterceptor().intercept(request, chain).block();

        assertThat(MDC.get("gqlOp")).isEqualTo("GetComics");
        assertThat(servletRequest.getAttribute(RequestLoggingFilter.GRAPHQL_OPERATION_ATTRIBUTE)).isEqualTo("GetComics");
    }

    @Test
    void recordsTheGraphqlExecutionTimeInTheRequestTimings() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("POST", "/graphql");
        RequestTimings timings = new RequestTimings();
        servletRequest.setAttribute(RequestTimings.ATTRIBUTE, timings);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
        WebGraphQlRequest request = mock(WebGraphQlRequest.class);
        when(request.getOperationName()).thenReturn("GetComic");
        when(request.getVariables()).thenReturn(Map.of());
        WebGraphQlInterceptor.Chain chain = mock(WebGraphQlInterceptor.Chain.class);
        when(chain.next(any())).thenReturn(Mono.just(mock(WebGraphQlResponse.class)));

        new GraphQlLoggingInterceptor().intercept(request, chain).block();

        assertThat(timings.summary()).matches(" \\(gql=\\d+ms\\)");
    }

    @Test
    void fallsBackToTheNameOfTheDocumentsOnlyOperation() {
        assertThat(GraphQlLoggingInterceptor.operationName(request(null, "query GetComic($id: Int!) { comic(id: $id) { id } }"))).isEqualTo("GetComic");
        assertThat(GraphQlLoggingInterceptor.operationName(request("", "fragment F on Comic { id } mutation Refresh { refresh }"))).isEqualTo("Refresh");
    }

    @Test
    void prefersTheOperationNameTheClientSent() {
        assertThat(GraphQlLoggingInterceptor.operationName(request("GetComics", "query GetComics { comics { id } } query Other { x }"))).isEqualTo("GetComics");
    }

    @Test
    void isAnonymousWhenTheDocumentDoesNotNameOneOperation() {
        assertThat(GraphQlLoggingInterceptor.operationName(request(null, "{ comics { id } }"))).isEqualTo("anonymous");
        assertThat(GraphQlLoggingInterceptor.operationName(request(null, "query A { x } query B { y }"))).isEqualTo("anonymous");
        assertThat(GraphQlLoggingInterceptor.operationName(request(null, "query Broken {"))).isEqualTo("anonymous");
    }

    private static WebGraphQlRequest request(String operationName, String document) {
        WebGraphQlRequest request = mock(WebGraphQlRequest.class);
        when(request.getOperationName()).thenReturn(operationName);
        when(request.getDocument()).thenReturn(document);
        return request;
    }
}
