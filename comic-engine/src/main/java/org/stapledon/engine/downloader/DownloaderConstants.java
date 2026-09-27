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

    private DownloaderConstants() {
    }
}
