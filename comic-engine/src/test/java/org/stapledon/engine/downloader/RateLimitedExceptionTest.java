package org.stapledon.engine.downloader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;


class RateLimitedExceptionTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 9, 24, 6, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void parseRetryAfter_deltaSeconds() {
        assertThat(RateLimitedException.parseRetryAfter("120", NOW)).contains(Duration.ofSeconds(120));
    }

    @Test
    void parseRetryAfter_httpDate() {
        assertThat(RateLimitedException.parseRetryAfter("Thu, 24 Sep 2026 06:05:00 GMT", NOW)).contains(Duration.ofMinutes(5));
    }

    @Test
    void parseRetryAfter_httpDateInPast_isZero() {
        assertThat(RateLimitedException.parseRetryAfter("Thu, 24 Sep 2026 05:00:00 GMT", NOW)).contains(Duration.ZERO);
    }

    @Test
    void parseRetryAfter_missingOrGarbage_isEmpty() {
        assertThat(RateLimitedException.parseRetryAfter(null, NOW)).isEmpty();
        assertThat(RateLimitedException.parseRetryAfter("  ", NOW)).isEmpty();
        assertThat(RateLimitedException.parseRetryAfter("soon", NOW)).isEmpty();
        assertThat(RateLimitedException.parseRetryAfter("-5", NOW)).isEmpty();
    }

    @Test
    void message_includesUrlAndRetryAfter() {
        RateLimitedException e = RateLimitedException.of("https://example.com/x", "30");

        assertThat(e.getMessage()).contains("429", "https://example.com/x", "Retry-After 30s");
        assertThat(e.getRetryAfter()).contains(Duration.ofSeconds(30));
    }
}
