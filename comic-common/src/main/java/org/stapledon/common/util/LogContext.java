package org.stapledon.common.util;

/**
 * MDC keys shared by every module. The log pattern prints whichever of them are set (see {@code MdcContextConverter} in comic-api), so a support
 * search for {@code req=}, {@code user=} or {@code comic=} finds every line of one request, user or comic.
 */
public final class LogContext {

    /** Short id of the HTTP request, echoed to the client in the {@code X-Request-Id} header. */
    public static final String REQUEST_ID = "requestId";

    /** Username of the authenticated caller. */
    public static final String USER = "user";

    /** GraphQL operation name, or "anonymous". */
    public static final String GRAPHQL_OPERATION = "gqlOp";

    /** Name of the comic being downloaded or processed. */
    public static final String COMIC = "comic";

    /** Strip date (ISO) being downloaded or processed. */
    public static final String DATE = "date";

    /** Strip number, for indexed sources. */
    public static final String STRIP = "strip";

    private LogContext() {
        // Constants only
    }
}
