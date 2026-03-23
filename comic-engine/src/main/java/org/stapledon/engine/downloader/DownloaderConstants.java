package org.stapledon.engine.downloader;

/**
 * Shared constants for comic downloader strategies.
 */
public final class DownloaderConstants {

    /**
     * Default User-Agent string used for all HTTP requests to comic sources.
     */
    public static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36";

    /**
     * Default connection timeout in milliseconds for Jsoup requests.
     */
    public static final int DEFAULT_TIMEOUT = 10 * 1000;

    private DownloaderConstants() {
    }
}
