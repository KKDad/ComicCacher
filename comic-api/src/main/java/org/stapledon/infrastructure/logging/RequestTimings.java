package org.stapledon.infrastructure.logging;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Where one request's time went: GraphQL execution, the slowest GraphQL field and storage reads. {@link RequestLoggingFilter} creates it
 * and appends {@link #summary()} to the completion line.
 * <p>
 * Kept as a request attribute rather than a ThreadLocal: GraphQL fields can run on other threads, which reach it through the GraphQL
 * context, and the filter runs again on the async dispatch. Every update is thread-safe.
 */
public final class RequestTimings {

    /** Request attribute and GraphQL context key. */
    public static final String ATTRIBUTE = RequestTimings.class.getName();

    private final AtomicLong graphqlNanos = new AtomicLong(-1);
    private final AtomicLong storageNanos = new AtomicLong();
    private final AtomicLong storageReads = new AtomicLong();
    private String slowestField;
    private long slowestFieldNanos = -1;

    /**
     * The current request's timings, or {@code null} outside a request (batch jobs, startup).
     */
    public static RequestTimings current() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes != null ? (RequestTimings) attributes.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST) : null;
    }

    public void recordGraphql(long nanos) {
        graphqlNanos.set(nanos);
    }

    public synchronized void recordField(String field, long nanos) {
        if (nanos > slowestFieldNanos) {
            slowestField = field;
            slowestFieldNanos = nanos;
        }
    }

    public void recordStorage(long nanos) {
        storageNanos.addAndGet(nanos);
        storageReads.incrementAndGet();
    }

    /**
     * The breakdown for the completion line, e.g. {@code " (gql=790ms slowest=Query.strip:640ms storage=3/610ms)"}, or an empty string when
     * nothing was recorded.
     */
    public synchronized String summary() {
        StringBuilder parts = new StringBuilder();
        if (graphqlNanos.get() >= 0) {
            parts.append(" gql=").append(millis(graphqlNanos.get())).append("ms");
        }
        if (slowestField != null) {
            parts.append(" slowest=").append(slowestField).append(':').append(millis(slowestFieldNanos)).append("ms");
        }
        if (storageReads.get() > 0) {
            parts.append(" storage=").append(storageReads.get()).append('/').append(millis(storageNanos.get())).append("ms");
        }
        return parts.isEmpty() ? "" : " (" + parts.substring(1) + ")";
    }

    static long millis(long nanos) {
        return nanos / 1_000_000;
    }
}
