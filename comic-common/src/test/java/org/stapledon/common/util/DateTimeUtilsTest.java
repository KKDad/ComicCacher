package org.stapledon.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

class DateTimeUtilsTest {

    private static final ZoneId TORONTO = ZoneId.of("America/Toronto");

    @Test
    void toOffsetUsesTheZonesOffsetOnThatDate() {
        assertThat(DateTimeUtils.toOffset(LocalDateTime.of(2026, 1, 15, 6, 0), TORONTO))
                .isEqualTo(OffsetDateTime.of(2026, 1, 15, 6, 0, 0, 0, ZoneOffset.ofHours(-5)));
        assertThat(DateTimeUtils.toOffset(LocalDateTime.of(2026, 7, 15, 6, 0), TORONTO))
                .isEqualTo(OffsetDateTime.of(2026, 7, 15, 6, 0, 0, 0, ZoneOffset.ofHours(-4)));
    }

    @Test
    void toOffsetPassesNullThrough() {
        assertThat(DateTimeUtils.toOffset(null, TORONTO)).isNull();
    }

    @Test
    void parseDateTimeReadsOffsetAndOffsetLessValues() {
        assertThat(DateTimeUtils.parseDateTime("2026-03-18T10:00:00-04:00"))
                .isEqualTo(OffsetDateTime.of(2026, 3, 18, 10, 0, 0, 0, ZoneOffset.ofHours(-4)));
        assertThat(DateTimeUtils.parseDateTime("2026-03-18T10:00:00"))
                .isEqualTo(OffsetDateTime.of(2026, 3, 18, 10, 0, 0, 0, ZoneOffset.UTC));
        assertThat(DateTimeUtils.parseDateTime("not a date")).isNull();
    }
}
