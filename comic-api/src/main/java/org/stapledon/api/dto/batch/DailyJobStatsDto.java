package org.stapledon.api.dto.batch;

import java.time.LocalDate;

/**
 * DTO for daily batch job execution statistics.
 */
public record DailyJobStatsDto(
        LocalDate date,
        int executionCount,
        int successCount,
        int failureCount
) {
}
