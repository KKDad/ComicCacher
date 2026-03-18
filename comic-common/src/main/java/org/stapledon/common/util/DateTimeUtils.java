package org.stapledon.common.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
