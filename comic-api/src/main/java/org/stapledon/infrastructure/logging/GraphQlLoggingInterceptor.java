package org.stapledon.infrastructure.logging;

import org.slf4j.MDC;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.stapledon.common.util.LogContext;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Puts the GraphQL operation name in the MDC (key {@code gqlOp}) and on the request, so {@link RequestLoggingFilter}'s completion line and every
 * log line of the operation say which query or mutation they belong to.
 */
@Slf4j
@Component
public class GraphQlLoggingInterceptor implements WebGraphQlInterceptor {

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String operation = request.getOperationName() != null ? request.getOperationName() : "anonymous";
        MDC.put(LogContext.GRAPHQL_OPERATION, operation);
        // WebGraphQlRequest attributes are read-only; the servlet request is reachable through the request context on this thread
        RequestAttributes servletRequest = RequestContextHolder.getRequestAttributes();
        if (servletRequest != null) {
            servletRequest.setAttribute(RequestLoggingFilter.GRAPHQL_OPERATION_ATTRIBUTE, operation, RequestAttributes.SCOPE_REQUEST);
        }
        log.debug("GraphQL operation {} (variables: {})", operation, request.getVariables().keySet());
        return chain.next(request);
    }
}
