package org.stapledon.common.util;

import java.time.OffsetDateTime;
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
     */
    public static OffsetDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(dateStr);
        } catch (DateTimeParseException e) {
            log.debug("Could not parse date string: {}", dateStr);
            return null;
        }
    }
}
