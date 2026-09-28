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
     * Converts a Spring Batch time to {@code zone}, normally {@code batch.timezone}. Returns null for null.
     *
     * <p>Spring Batch records job and step times with {@code LocalDateTime.now()}: the wall-clock time in the
     * JVM's default zone (UTC in the containers), so that is the zone the value is read in.
     */
    public static OffsetDateTime toOffset(LocalDateTime batchTime, ZoneId zone) {
        return toOffset(batchTime, ZoneId.systemDefault(), zone);
    }

    /**
     * Reads {@code localDateTime} as a wall-clock time in {@code recordedIn} and returns the same instant
     * in {@code zone}. Returns null for null.
     */
    public static OffsetDateTime toOffset(LocalDateTime localDateTime, ZoneId recordedIn, ZoneId zone) {
        return localDateTime == null ? null : localDateTime.atZone(recordedIn).withZoneSameInstant(zone).toOffsetDateTime();
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
