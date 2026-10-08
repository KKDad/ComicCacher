package org.stapledon.engine.downloader;

/**
 * Shared constants for comic downloader strategies. User-Agent strings come from
 * {@link org.stapledon.common.infrastructure.web.UserAgentService}.
 */
public final class DownloaderConstants {

    /**
     * Default connection timeout in milliseconds for Jsoup requests.
     */
    public static final int DEFAULT_TIMEOUT = 10 * 1000;

    /**
     * HTTP 403 (Forbidden): the source refused the request.
     */
    public static final int HTTP_FORBIDDEN = 403;

    /**
     * After this many HTTP 403s in a row from one source, a download run or backfill skips that source's remaining comics. One 403 can be a
     * single forbidden strip; several in a row mean the source is blocking us, and every further request only deepens the block.
     */
    public static final int BLOCKED_IN_A_ROW_TO_STOP_SOURCE = 3;

    private DownloaderConstants() {
    }
}
