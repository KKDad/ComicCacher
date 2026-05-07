package org.stapledon.engine.downloader;

/**
 * Shared constants for comic downloader strategies.
 *
 * <p>Note: User-Agent strings are no longer kept here — use {@link org.stapledon.common.infrastructure.web.UserAgentService} instead.
 */
public final class DownloaderConstants {

    /**
     * Default connection timeout in milliseconds for Jsoup requests.
     */
    public static final int DEFAULT_TIMEOUT = 10 * 1000;

    private DownloaderConstants() {
    }
}
