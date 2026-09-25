package org.stapledon.infrastructure.logging;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.stapledon.common.util.LogContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HexFormat;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Gives every request a short id and logs one line when it completes: method, path, GraphQL operation, status, time, client address and user.
 * <ul>
 * <li>The id comes from a well-formed {@code X-Request-Id} header (so the frontend can pass its own), otherwise it is generated; it is put in the
 * MDC and echoed back in the response header, so a user-reported error can be matched to its log lines.</li>
 * <li>GraphQL requests complete asynchronously. The filter also runs on the async dispatch and logs the completion line there, with the final
 * status.</li>
 * </ul>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /** Request attribute holding the GraphQL operation name, set by {@code GraphQlLoggingInterceptor}. */
    public static final String GRAPHQL_OPERATION_ATTRIBUTE = RequestLoggingFilter.class.getName() + ".gqlOp";

    private static final String REQUEST_ID_ATTRIBUTE = RequestLoggingFilter.class.getName() + ".requestId";
    private static final String START_ATTRIBUTE = RequestLoggingFilter.class.getName() + ".start";
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // Run again on the async dispatch so the completion line has the final status
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = (String) request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (requestId == null) {
            requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
            request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
            request.setAttribute(START_ATTRIBUTE, System.nanoTime());
            response.setHeader(REQUEST_ID_HEADER, requestId);
        }
        MDC.put(LogContext.REQUEST_ID, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (!isAsyncStarted(request)) {
                logCompletion(request, response);
            }
            MDC.remove(LogContext.REQUEST_ID);
            MDC.remove(LogContext.USER);
            MDC.remove(LogContext.GRAPHQL_OPERATION);
        }
    }

    static String resolveRequestId(String header) {
        if (header != null && VALID_REQUEST_ID.matcher(header).matches()) {
            return header;
        }
        return HexFormat.of().toHexDigits(ThreadLocalRandom.current().nextInt());
    }

    private static void logCompletion(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();
        boolean interesting = path.startsWith("/graphql") || path.startsWith("/api/");
        if (!interesting && !log.isDebugEnabled()) {
            return;
        }
        long elapsedMs = (System.nanoTime() - (Long) request.getAttribute(START_ATTRIBUTE)) / 1_000_000;
        Object operation = request.getAttribute(GRAPHQL_OPERATION_ATTRIBUTE);
        String user = request.getRemoteUser();
        String message = "{} {}{} -> {} in {}ms from {}{}";
        Object[] args = {request.getMethod(), path, operation != null ? " (" + operation + ")" : "", response.getStatus(), elapsedMs,
                request.getRemoteAddr(), user != null ? " user=" + user : ""};
        if (interesting) {
            log.info(message, args);
        } else {
            log.debug(message, args);
        }
    }
}
