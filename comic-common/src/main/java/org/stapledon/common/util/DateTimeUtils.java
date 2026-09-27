package org.stapledon.common.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import lombok.extern.slf4j.Slf4j;

/**
 * Shared date-time parsing utilities.
 */
@Slf4j
public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    /**
     * Gives a local date-time (as Spring Batch records them) the offset it had in {@code zone},
     * normally {@code batch.timezone}. Returns null for null.
     */
    public static OffsetDateTime toOffset(LocalDateTime localDateTime, ZoneId zone) {
        return localDateTime == null ? null : localDateTime.atZone(zone).toOffsetDateTime();
    }

    /**
     * Parse an ISO-8601 date-time string to OffsetDateTime, returning null on failure.
     * Handles both offset formats (e.g. "2024-01-15T10:30:00Z") and local formats
     * (e.g. "2024-01-15T10:30:00") by assuming UTC for local timestamps.
     */
    public static OffsetDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(dateStr);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(dateStr).atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException e2) {
                log.debug("Could not parse date string: {}", dateStr);
                return null;
            }
        }
    }
}
