package org.stapledon.engine.downloader;

import lombok.extern.slf4j.Slf4j;
import org.brotli.dec.BrotliInputStream;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.stapledon.common.infrastructure.web.UserAgentService;

/**
 * Fetches pages the way desktop Chrome does, for sources behind bot protection (GoComics sits behind Cloudflare). Sends Chrome's navigation headers
 * with {@code Sec-Ch-Ua} client hints that match the configured User-Agent, and decodes Brotli, which Jsoup doesn't. An HTTP 429 becomes a
 * {@link RateLimitedException}; any other status of 400 or more a Jsoup {@link HttpStatusException}.
 */
@Slf4j
public class BrowserFetcher {

    /** Default timeout for one request. */
    public static final int DEFAULT_TIMEOUT_MS = 5 * 1000;

    /** Jsoup's own default body limit (2 MB), right for a single strip or about page. */
    public static final int DEFAULT_MAX_BODY_BYTES = 2 * 1024 * 1024;

    private static final Pattern CHROME_MAJOR_VERSION = Pattern.compile("Chrome/(\\d+)\\.");

    private final UserAgentService userAgentService;

    public BrowserFetcher(UserAgentService userAgentService) {
        this.userAgentService = userAgentService;
    }

    /**
     * Fetches and parses an HTML page, as a top-level Chrome navigation.
     *
     * @param maxBodyBytes the most to read, or 0 for no limit
     */
    public Document fetchDocument(String source, String url, int timeoutMs, int maxBodyBytes) throws IOException {
        Connection.Response response = execute(source, url, "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
                true, timeoutMs, maxBodyBytes);
        try (InputStream body = decodedBody(response)) {
            return Jsoup.parse(body, response.charset(), response.url().toExternalForm());
        }
    }

    /**
     * Fetches a JSON (or other text) resource, as a script on the source's own site would.
     *
     * @param maxBodyBytes the most to read, or 0 for no limit
     */
    public String fetchJson(String source, String url, int timeoutMs, int maxBodyBytes) throws IOException {
        Connection.Response response = execute(source, url, "application/json", false, timeoutMs, maxBodyBytes);
        Charset charset = Optional.ofNullable(response.charset()).map(Charset::forName).orElse(StandardCharsets.UTF_8);
        try (InputStream body = decodedBody(response)) {
            return new String(body.readAllBytes(), charset);
        }
    }

    /**
     * Fetches a Next.js page's React Server Components payload ({@code text/x-component}), as the site's own router does when a reader clicks
     * through to {@code url} from {@code fromPath}: an {@code RSC: 1} request with {@code Next-Url}, and the {@code _rsc} cache-busting parameter
     * computed the way the router computes it.
     *
     * @param fromPath the site path the navigation starts from, such as {@code /garfield}
     * @param maxBodyBytes the most to read, or 0 for no limit
     */
    public String fetchNextJsFlight(String source, String url, String fromPath, int timeoutMs, int maxBodyBytes) throws IOException {
        URI uri = URI.create(url);
        String origin = uri.getScheme() + "://" + uri.getAuthority();
        String flightUrl = url + (uri.getRawQuery() == null ? "?" : "&") + "_rsc=" + nextJsCacheBuster(fromPath);
        Connection.Response response = execute(source, flightUrl, "*/*", false, timeoutMs, maxBodyBytes, Map.of(
                "RSC", "1",
                "Next-Url", fromPath,
                "Referer", origin + fromPath,
                "Sec-Fetch-Site", "same-origin"));
        Charset charset = Optional.ofNullable(response.charset()).map(Charset::forName).orElse(StandardCharsets.UTF_8);
        try (InputStream body = decodedBody(response)) {
            return new String(body.readAllBytes(), charset);
        }
    }

    /**
     * The {@code _rsc} value the Next.js router sends for a navigation with only {@code Next-Url} set: the first 12 bytes of the SHA-256 of
     * {@code "<prefetch>,<segment-prefetch>,<state-tree>,<next-url>"} (absent headers count as {@code 0}), base64url without padding.
     * The server doesn't check it; sending what a browser would keeps the request ordinary.
     */
    static String nextJsCacheBuster(String nextUrl) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(("0,0,0," + nextUrl).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(digest, 12));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private Connection.Response execute(String source, String url, String accept, boolean navigation, int timeoutMs, int maxBodyBytes) throws IOException {
        return execute(source, url, accept, navigation, timeoutMs, maxBodyBytes, Map.of());
    }

    private Connection.Response execute(String source, String url, String accept, boolean navigation, int timeoutMs, int maxBodyBytes,
            Map<String, String> extraHeaders) throws IOException {
        long start = System.nanoTime();
        String userAgent = userAgentService.getUserAgent(source);
        Connection connection = Jsoup.connect(url)
                .userAgent(userAgent)
                .header("Accept", accept)
                .header("Accept-Language", "en-US,en;q=0.9")
                // Chrome also advertises zstd, which we can't decode, so it is left out
                .header("Accept-Encoding", "gzip, deflate, br")
                .ignoreContentType(!navigation)
                .maxBodySize(maxBodyBytes)
                .timeout(timeoutMs)
                .ignoreHttpErrors(true);
        chromeClientHints(userAgent).ifPresent(hint -> connection
                .header("Sec-Ch-Ua", hint)
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "\"Windows\""));
        if (navigation) {
            connection.header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1");
        } else {
            connection.header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-site");
        }
        // Set last, so these replace any default above
        connection.headers(extraHeaders);
        Connection.Response response = connection.execute();
        log.debug("GET {} [{}] -> HTTP {} in {}ms", url, source, response.statusCode(), (System.nanoTime() - start) / 1_000_000);

        if (response.statusCode() == RateLimitedException.HTTP_TOO_MANY_REQUESTS) {
            throw RateLimitedException.of(url, response.header("Retry-After"));
        }
        if (response.statusCode() >= 400) {
            throw new HttpStatusException("HTTP error fetching URL", response.statusCode(), url);
        }
        return response;
    }

    private static InputStream decodedBody(Connection.Response response) throws IOException {
        InputStream stream = response.bodyStream();
        if ("br".equalsIgnoreCase(response.header("Content-Encoding"))) {
            return new BrotliInputStream(stream);
        }
        return stream;
    }

    /**
     * Builds the {@code Sec-Ch-Ua} value real Chrome would send alongside {@code userAgent}, so the client hints never disagree with the UA string.
     * Empty when the UA isn't a Chrome UA (e.g. a Firefox per-source override), since other browsers don't send client hints.
     */
    static Optional<String> chromeClientHints(String userAgent) {
        if (userAgent == null) {
            return Optional.empty();
        }
        Matcher m = CHROME_MAJOR_VERSION.matcher(userAgent);
        if (!m.find()) {
            return Optional.empty();
        }
        String major = m.group(1);
        return Optional.of(String.format("\"Chromium\";v=\"%1$s\", \"Google Chrome\";v=\"%1$s\", \"Not?A_Brand\";v=\"99\"", major));
    }
}
