package org.stapledon.engine.downloader;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;


/**
 * Thrown when a source answers HTTP 429 (Too Many Requests). Carries the server's {@code Retry-After} hint, if it sent a usable one.
 */
@Getter
@Slf4j
public class RateLimitedException extends IOException {

    public static final int HTTP_TOO_MANY_REQUESTS = 429;

    private final String url;
    private final transient Optional<Duration> retryAfter;

    public RateLimitedException(String url, Optional<Duration> retryAfter) {
        super(String.format("HTTP 429 Too Many Requests from %s%s", url,
                retryAfter.map(d -> " (Retry-After " + d.toSeconds() + "s)").orElse("")));
        this.url = url;
        this.retryAfter = retryAfter;
    }

    /**
     * Builds the exception from a raw {@code Retry-After} header value.
     */
    public static RateLimitedException of(String url, String retryAfterHeader) {
        return new RateLimitedException(url, parseRetryAfter(retryAfterHeader, OffsetDateTime.now()));
    }

    /**
     * Parses a {@code Retry-After} header, which is either a number of seconds or an HTTP-date (RFC 1123). Returns empty for a missing or unparseable value;
     * a date in the past yields {@link Duration#ZERO}.
     */
    static Optional<Duration> parseRetryAfter(String header, OffsetDateTime now) {
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        String value = header.trim();
        try {
            long seconds = Long.parseLong(value);
            return seconds < 0 ? Optional.empty() : Optional.of(Duration.ofSeconds(seconds));
        } catch (NumberFormatException _) {
            // Not delta-seconds; fall through to HTTP-date
        }
        try {
            OffsetDateTime at = OffsetDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
            Duration wait = Duration.between(now, at);
            return Optional.of(wait.isNegative() ? Duration.ZERO : wait);
        } catch (DateTimeParseException _) {
            log.warn("Ignoring unparseable Retry-After header '{}'; using the configured backoff instead", value);
            return Optional.empty();
        }
    }
}
